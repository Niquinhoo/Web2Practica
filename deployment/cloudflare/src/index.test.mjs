import { registerHooks } from "node:module";
import { test, mock } from "node:test";
import assert from "node:assert/strict";
registerHooks({
  resolve(specifier, context, next) {
    if (specifier === "cloudflare:workers") return {
      url: "data:text/javascript," + encodeURIComponent("export class DurableObject { constructor(ctx, env) { this.ctx = ctx; this.env = env; } }"),
      shortCircuit: true,
    };
    return next(specifier, context);
  },
});
globalThis.scheduler = { wait: async () => {} };
const { ApiContainer, default: worker } = await import("./index.ts");
const env = { DATABASE_URL: "jdbc:test", POSTGRES_USER: "test", POSTGRES_PASSWORD: "private", JWT_SECRETO: "private", PG_TUNNEL_HOSTNAME: "db.example.com", TUNNEL_SERVICE_TOKEN_ID: "id", TUNNEL_SERVICE_TOKEN_SECRET: "private" };
function fixture(health = async () => new Response("ok"), running = false) {
  const starts = [], idle = [], forwarded = [];
  const container = {
    running, images: { base: "image@sha256:test" },
    start(options) { starts.push(options); this.running = true; },
    async setInactivityTimeout(value) { idle.push(value); },
    getTcpPort(port) {
      assert.equal(port, 8080);
      return { fetch: async (input) => {
        if (typeof input === "string") return health();
        forwarded.push(input);
        return new Response("forwarded");
      } };
    },
  };
  const api = new ApiContainer({container, blockConcurrencyWhile: fn => fn()}, env);
  return {api, starts, idle, forwarded};
}
test("cold start espera readiness y conserva body y bearer con headers de proxy confiables", async () => {
  let attempts = 0;
  const f = fixture(async () => new Response("", {status: ++attempts < 3 ? 503 : 200}));
  const request = new Request("https://api.example.com/api/listas", {method: "POST", body: "{}", headers: {Authorization: "Bearer ejemplo", Forwarded: "host=malicioso.example", "X-Forwarded-Host": "malicioso.example"}});
  assert.equal((await f.api.fetch(request)).status, 200);
  assert.equal(attempts, 3);
  assert.equal(f.starts.length, 1);
  assert.equal(f.starts[0].instance, "basic");
  assert.equal(f.starts[0].enableInternet, true);
  assert.equal(f.starts[0].env.POSTGRES_PASSWORD, "private");
  const forwarded = f.forwarded[0];
  assert.equal(forwarded.headers.get("Authorization"), "Bearer ejemplo");
  assert.equal(forwarded.headers.get("Forwarded"), null);
  assert.equal(forwarded.headers.get("X-Forwarded-Host"), "api.example.com");
  assert.equal(forwarded.headers.get("X-Forwarded-Proto"), "https");
  assert.equal(await forwarded.text(), "{}");
});
test("dos requests concurrentes comparten un único inicio y readiness", async () => {
  const gate = Promise.withResolvers();
  let healthCalls = 0;
  const f = fixture(async () => { healthCalls++; await gate.promise; return new Response("ok"); });
  const first = f.api.fetch(new Request("https://api.example.com/health"));
  const second = f.api.fetch(new Request("https://api.example.com/health"));
  await Promise.resolve(); gate.resolve();
  assert.deepEqual((await Promise.all([first,second])).map(r=>r.status), [200,200]);
  assert.equal(f.starts.length, 1); assert.equal(healthCalls, 1);
});
test("readiness agotado devuelve 503 y no expone detalles internos", async () => {
  const logger = mock.method(console,"error",()=>{});
  let attempts = 0;
  const f = fixture(async () => { attempts++; throw new Error("detalle-interno"); });
  const response = await f.api.fetch(new Request("https://api.example.com/health"));
  assert.equal(response.status, 503); assert.equal(attempts, 180);
  assert.equal((await response.text()).includes("detalle-interno"), false);
  logger.mock.restore();
});
test("instancia caliente restaura inactividad sin iniciar otra JVM", async () => {
  const f = fixture(undefined, true);
  assert.equal((await f.api.fetch(new Request("https://api.example.com/health"))).status, 200);
  assert.equal(f.starts.length, 0); assert.ok(f.idle.every(x=>x===60000));
});
test("router usa siempre la única instancia tp3", async () => {
  let name;
  const response = await worker.fetch(new Request("https://api.example.com/health"), {}, {exports: {ApiContainer: {getByName(value) { name=value; return {fetch: async()=>new Response("ok")}; }}}});
  assert.equal(name,"tp3"); assert.equal(response.status,200);
});
