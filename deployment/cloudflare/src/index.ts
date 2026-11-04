import { DurableObject } from "cloudflare:workers";
interface Env {
  DATABASE_URL: string;
  POSTGRES_USER: string;
  POSTGRES_PASSWORD: string;
  JWT_SECRETO: string;
  PG_TUNNEL_HOSTNAME: string;
  TUNNEL_SERVICE_TOKEN_ID: string;
  TUNNEL_SERVICE_TOKEN_SECRET: string;
  DATABASE_FALLBACK_URL?: string;
  DATABASE_FALLBACK_USER?: string;
  DATABASE_FALLBACK_PASSWORD?: string;
}
export class ApiContainer extends DurableObject<Env> {
  private starting: Promise<void> | undefined;
  private ready = false;
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    if (ctx.container?.running) {
      void ctx.blockConcurrencyWhile(() => ctx.container!.setInactivityTimeout(60000));
    }
  }
  async fetch(request: Request): Promise<Response> {
    try {
      // Con la JVM ya verificada no se repite el readiness: sumaría latencia a cada petición.
      if (!this.ready || !this.ctx.container!.running) {
        this.starting ??= this.startAndWait().finally(() => { this.starting = undefined; });
        await this.starting;
      }
      const original = new URL(request.url);
      const url = new URL(request.url);
      url.protocol = "http:";
      url.host = "container";
      const forwarded = new Request(url, request);
      forwarded.headers.delete("host");
      forwarded.headers.delete("forwarded");
      forwarded.headers.set("X-Forwarded-Proto", original.protocol.slice(0, -1));
      forwarded.headers.set("X-Forwarded-Host", original.host);
      forwarded.headers.set("X-Forwarded-Port", original.port || (original.protocol === "https:" ? "443" : "80"));
      return await this.ctx.container!.getTcpPort(8080).fetch(forwarded);
    } catch (error) {
      this.ready = false;
      console.error("No se pudo iniciar o contactar la API", error instanceof Error ? error.message : "Error del contenedor");
      return Response.json({status: 503, mensaje: "La API está iniciando o no está disponible"}, {status: 503});
    }
  }
  private async startAndWait(): Promise<void> {
    this.ready = false;
    const container = this.ctx.container!;
    if (!container.running) {
      container.start({
        image: container.images.base,
        instance: "basic",
        enableInternet: true,
        env: {
          SPRING_PROFILES_ACTIVE: "prod",
          JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=65 -Duser.timezone=UTC",
          DATABASE_URL: this.env.DATABASE_URL,
          POSTGRES_USER: this.env.POSTGRES_USER,
          POSTGRES_PASSWORD: this.env.POSTGRES_PASSWORD,
          JWT_SECRETO: this.env.JWT_SECRETO,
          PG_TUNNEL_HOSTNAME: this.env.PG_TUNNEL_HOSTNAME,
          TUNNEL_SERVICE_TOKEN_ID: this.env.TUNNEL_SERVICE_TOKEN_ID,
          TUNNEL_SERVICE_TOKEN_SECRET: this.env.TUNNEL_SERVICE_TOKEN_SECRET,
          ...(this.env.DATABASE_FALLBACK_URL ? {
            DATABASE_FALLBACK_URL: this.env.DATABASE_FALLBACK_URL,
            DATABASE_FALLBACK_USER: this.env.DATABASE_FALLBACK_USER ?? "",
            DATABASE_FALLBACK_PASSWORD: this.env.DATABASE_FALLBACK_PASSWORD ?? "",
          } : {}),
        },
      });
    }
    await container.setInactivityTimeout(60000);
    // Spring/Flyway necesitan un readiness check más largo que un servidor HTTP pequeño.
    for (let attempt = 0; attempt < 180; attempt++) {
      try {
        const health = await container.getTcpPort(8080).fetch("http://container/health", {signal: AbortSignal.timeout(1000)});
        await health.body?.cancel();
        if (health.ok) { this.ready = true; return; }
      } catch { /* El puerto todavía puede estar iniciando. */ }
      await scheduler.wait(1000);
    }
    throw new Error("La JVM no quedó lista dentro de 180 intentos");
  }
}
export default {
  fetch(request: Request, _env: Env, ctx: ExecutionContext): Promise<Response> {
    return ctx.exports.ApiContainer.getByName("tp3").fetch(request);
  },
} satisfies ExportedHandler<Env>;
