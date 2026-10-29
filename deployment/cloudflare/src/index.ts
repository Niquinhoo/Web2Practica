import { Container, getContainer } from "@cloudflare/containers";
interface Env {
  API: DurableObjectNamespace<ApiContainer>;
  DATABASE_URL: string;
  POSTGRES_USER: string;
  POSTGRES_PASSWORD: string;
  JWT_SECRETO: string;
  PG_TUNNEL_HOSTNAME: string;
  TUNNEL_SERVICE_TOKEN_ID: string;
  TUNNEL_SERVICE_TOKEN_SECRET: string;
}
export class ApiContainer extends Container<Env> {
  defaultPort = 8080;
  override async fetch(request: Request): Promise<Response> {
    // La JVM y Flyway necesitan más tiempo de arranque que el default de 20 segundos.
    await this.startAndWaitForPorts(8080, {
      instanceGetTimeoutMS: 60000,
      portReadyTimeoutMS: 180000,
      abort: request.signal,
    });
    return this.containerFetch(request);
  }
  sleepAfter = "1m";
  envVars = {
    SPRING_PROFILES_ACTIVE: "prod",
    JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=65 -Duser.timezone=UTC",
    DATABASE_URL: this.env.DATABASE_URL,
    POSTGRES_USER: this.env.POSTGRES_USER,
    POSTGRES_PASSWORD: this.env.POSTGRES_PASSWORD,
    JWT_SECRETO: this.env.JWT_SECRETO,
    PG_TUNNEL_HOSTNAME: this.env.PG_TUNNEL_HOSTNAME,
    TUNNEL_SERVICE_TOKEN_ID: this.env.TUNNEL_SERVICE_TOKEN_ID,
    TUNNEL_SERVICE_TOKEN_SECRET: this.env.TUNNEL_SERVICE_TOKEN_SECRET,
  };
}
export default {
  fetch(request: Request, env: Env): Promise<Response> {
    return getContainer(env.API, "tp3").fetch(request);
  },
};
