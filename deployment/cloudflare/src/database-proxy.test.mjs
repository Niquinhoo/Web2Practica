import { test } from "node:test";
import assert from "node:assert/strict";
import proxy from "./database-proxy.ts";
const secret = "clave-de-transporte-de-prueba-con-mas-de-32-bytes";
function fixture() {
  const destinations = [];
  return {
    destinations,
    env: {
      TUNNEL_SERVICE_TOKEN_ID: "tp3-test",
      TUNNEL_SERVICE_TOKEN_SECRET: secret,
      DB_NETWORK: { connect(address) { destinations.push(address); throw new Error("No debe conectar sin autorización y WebSocket"); } },
    },
  };
}
function request(headers = {}) {
  return new Request("https://database.example.workers.dev/", { headers });
}
test("proxy rechaza credenciales ausentes antes de conectar a PostgreSQL", async () => {
  const f = fixture();
  assert.equal((await proxy.fetch(request(), f.env)).status, 401);
  assert.deepEqual(f.destinations, []);
});
test("proxy rechaza secreto incorrecto aunque el identificador coincida", async () => {
  const f = fixture();
  assert.equal((await proxy.fetch(request({ "CF-Access-Client-Id": "tp3-test", "CF-Access-Client-Secret": secret + "incorrecto", Upgrade: "websocket" }), f.env)).status, 401);
  assert.deepEqual(f.destinations, []);
});
test("proxy falla cerrado si no tiene configurado su secreto", async () => {
  const f = fixture();
  f.env.TUNNEL_SERVICE_TOKEN_SECRET = "";
  assert.equal((await proxy.fetch(request({ "CF-Access-Client-Id": "tp3-test", "CF-Access-Client-Secret": "", Upgrade: "websocket" }), f.env)).status, 401);
  assert.deepEqual(f.destinations, []);
});
test("proxy autenticado requiere WebSocket y no permite un GET TCP arbitrario", async () => {
  const f = fixture();
  assert.equal((await proxy.fetch(request({ "CF-Access-Client-Id": "tp3-test", "CF-Access-Client-Secret": secret }), f.env)).status, 426);
  assert.deepEqual(f.destinations, []);
});
