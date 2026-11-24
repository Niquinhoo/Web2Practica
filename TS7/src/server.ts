import { readFile } from "node:fs/promises";
import { createServer, type ServerResponse } from "node:http";
import { config } from "./config.js";
import { attachRealtime } from "./realtime.js";

const publicDir = new URL("../public/", import.meta.url);
const html = "text/html; charset=utf-8";
const javascript = "text/javascript; charset=utf-8";
const assets = new Map([
  ["/", { name: "index.html", type: html }],
  ["/pantalla", { name: "screen.html", type: html }],
  ["/participant.js", { name: "participant.js", type: javascript }],
  ["/screen.js", { name: "screen.js", type: javascript }],
  ["/ranking.js", { name: "ranking.js", type: javascript }],
  ["/styles.css", { name: "styles.css", type: "text/css; charset=utf-8" }],
]);

function text(response: ServerResponse, status: number, body: string): void {
  response.writeHead(status, { "Content-Type": "text/plain; charset=utf-8" });
  response.end(body);
}

const server = createServer(async (request, response) => {
  if (request.method !== "GET") {
    response.setHeader("Allow", "GET");
    text(response, 405, "Método no permitido.");
    return;
  }

  const path = request.url?.split("?", 1)[0] ?? "";
  if (path === "/events") {
    subscribeScreen(response);
    return;
  }
  if (path === "/health") {
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ status: "ok" }));
    return;
  }

  const asset = assets.get(path);
  if (!asset) {
    text(response, 404, "Ruta inexistente.");
    return;
  }

  try {
    const contents = await readFile(new URL(asset.name, publicDir));
    response.writeHead(200, { "Content-Type": asset.type });
    response.end(contents);
  } catch (error) {
    console.error("Error al servir el archivo", asset.name, error);
    text(response, 500, "No se pudo servir el archivo.");
  }
});

const subscribeScreen = attachRealtime(server);
server.listen(config.port, config.host, () => {
  console.log(`TS7 disponible en http://${config.host}:${config.port}`);
});
