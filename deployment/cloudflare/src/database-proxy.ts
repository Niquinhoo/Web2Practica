interface DatabaseProxyEnv {
  TUNNEL_SERVICE_TOKEN_ID: string;
  TUNNEL_SERVICE_TOKEN_SECRET: string;
  DB_NETWORK: { connect(address: string): Promise<Socket> };
}

export default {
  async fetch(request: Request, env: DatabaseProxyEnv, ctx: ExecutionContext): Promise<Response> {
    const provided = request.headers.get("CF-Access-Client-Secret");
    if (!env.TUNNEL_SERVICE_TOKEN_ID || !env.TUNNEL_SERVICE_TOKEN_SECRET ||
        env.TUNNEL_SERVICE_TOKEN_SECRET.length < 32 || !provided ||
        request.headers.get("CF-Access-Client-Id") !== env.TUNNEL_SERVICE_TOKEN_ID) {
      return new Response("No autorizado", { status: 401 });
    }
    const encoder = new TextEncoder();
    const algorithm = { name: "HMAC", hash: "SHA-256" };
    const expectedKey = await crypto.subtle.importKey("raw", encoder.encode(env.TUNNEL_SERVICE_TOKEN_SECRET), algorithm, false, ["sign"]);
    const candidateKey = await crypto.subtle.importKey("raw", encoder.encode(provided), algorithm, false, ["verify"]);
    const challenge = encoder.encode("web2-tp3-postgres");
    const signature = await crypto.subtle.sign("HMAC", expectedKey, challenge);
    if (!await crypto.subtle.verify("HMAC", candidateKey, signature, challenge)) {
      return new Response("No autorizado", { status: 401 });
    }
    if (request.method !== "GET" || new URL(request.url).pathname !== "/") {
      return new Response("No existe", { status: 404 });
    }
    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return new Response("Se requiere WebSocket", { status: 426 });
    }
    let socket: Socket;
    try {
      // El destino es fijo: nunca se acepta un host o puerto del cliente.
      socket = await env.DB_NETWORK.connect("127.0.0.1:5432");
      void socket.closed.catch(() => {});
      await socket.opened;
    } catch {
      return new Response("PostgreSQL no disponible", { status: 502 });
    }
    const [client, server] = Object.values(new WebSocketPair());
    server.accept();
    const writer = socket.writable.getWriter();
    let closed = false;
    let pendingBytes = 0;
    const close = (code = 1000) => {
      if (closed) return;
      closed = true;
      void socket.close().catch(() => {});
      try { server.close(code); } catch { /* El cliente ya puede haber cerrado. */ }
    };
    server.addEventListener("close", () => close());
    server.addEventListener("error", () => close(1011));
    server.addEventListener("message", event => {
      if (closed) return;
      if (!(event.data instanceof ArrayBuffer)) { close(1003); return; }
      const bytes = new Uint8Array(event.data);
      pendingBytes += bytes.byteLength;
      if (pendingBytes > 1024 * 1024) { close(1009); return; }
      void writer.write(bytes).catch(() => close(1011)).finally(() => { pendingBytes -= bytes.byteLength; });
    });
    ctx.waitUntil(socket.readable.pipeTo(new WritableStream<Uint8Array>({
      write(chunk) { if (!closed) server.send(chunk); },
      close() { close(); },
      abort() { close(1011); },
    })).catch(() => close(1011)));
    return new Response(null, { status: 101, webSocket: client });
  },
} satisfies ExportedHandler<DatabaseProxyEnv>;
