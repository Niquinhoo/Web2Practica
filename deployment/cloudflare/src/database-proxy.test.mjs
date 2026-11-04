import { test, mock } from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
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
test("proxy conserva tráfico binario con el default Blob actual de Workers y cierra el socket", async () => {
  const originalPair = globalThis.WebSocketPair;
  const NativeResponse = globalThis.Response;
  const writes = [], replies = [], destinations = [], background = [];
  let server, controller, closes = 0;
  const closed = Promise.withResolvers();
  const socket = {
    readable: new ReadableStream({ start(c) { controller = c; } }),
    writable: new WritableStream({ write(bytes) { writes.push([...bytes]); } }),
    opened: Promise.resolve({}), closed: closed.promise,
    async close() { closes++; controller.close(); closed.resolve(); },
  };
  class Server extends EventTarget {
    binaryType = "blob";
    accept() {}
    send(bytes) { replies.push([...bytes]); }
    close() { this.dispatchEvent(new Event("close")); }
    binary(bytes) {
      const buffer = Uint8Array.from(bytes).buffer;
      this.dispatchEvent(new MessageEvent("message", { data: this.binaryType === "arraybuffer" ? buffer : new Blob([buffer]) }));
    }
  }
  globalThis.WebSocketPair = class {
    constructor() { this[0] = {}; this[1] = server = new Server(); }
  };
  const responseMock = mock.method(globalThis, "Response", function(body, init) {
    return init?.status === 101 ? { status: 101, webSocket: init.webSocket } : new NativeResponse(body, init);
  });
  try {
    const f = fixture();
    f.env.DB_NETWORK.connect = async address => { destinations.push(address); return socket; };
    const response = await proxy.fetch(request({ "CF-Access-Client-Id": "tp3-test", "CF-Access-Client-Secret": secret, Upgrade: "websocket" }), f.env, { waitUntil(p) { background.push(p); } });
    assert.equal(response.status, 101);
    server.binary([0, 0, 0, 8, 4, 210, 22, 47]);
    await setImmediate();
    assert.deepEqual(writes, [[0, 0, 0, 8, 4, 210, 22, 47]]);
    assert.deepEqual(destinations, ["127.0.0.1:5432"]);
    controller.enqueue(Uint8Array.from([83]));
    await setImmediate();
    assert.deepEqual(replies, [[83]]);
    server.close();
    await Promise.all(background);
    assert.equal(closes, 1);
  } finally {
    server?.close();
    await Promise.allSettled(background);
    responseMock.mock.restore();
    if (originalPair === undefined) delete globalThis.WebSocketPair;
    else globalThis.WebSocketPair = originalPair;
  }
});
