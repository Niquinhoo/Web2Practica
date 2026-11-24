import { createServer } from "node:http";
import { config } from "./config.js";
import { attachRealtime } from "./realtime.js";

const server = createServer((request, response) => {
  if (request.method === "GET" && request.url === "/events") {
    subscribeScreen(response);
    return;
  }
  if (request.method === "GET" && request.url === "/health") {
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ status: "ok" }));
    return;
  }

  response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
  response.end("Ruta inexistente.");
});

const subscribeScreen = attachRealtime(server);
server.listen(config.port, config.host, () => {
  console.log(`TS7 disponible en http://${config.host}:${config.port}`);
});
