import { randomUUID } from "node:crypto";
import type { Server, ServerResponse } from "node:http";
import { WebSocket, WebSocketServer } from "ws";
import { lookupProduct } from "./catalog.js";
import { GiftList } from "./gift-list.js";
import {
  parseMessage,
  type ActionError,
  type ProposalFailure,
  type ScreenEvent,
  type ServerMessage,
} from "./protocol.js";

interface Participant {
  id: string;
  name: string;
  connection: AbortController;
}

const MAX_OUTPUT_BYTES = 256 * 1024;
const SSE_KEEPALIVE_MS = 15_000;

function send(socket: WebSocket, message: ServerMessage): void {
  if (socket.readyState !== WebSocket.OPEN) {
    return;
  }
  if (socket.bufferedAmount > MAX_OUTPUT_BYTES) {
    socket.terminate();
    return;
  }
  socket.send(JSON.stringify(message));
}

function sendError(socket: WebSocket, code: ActionError, message: string): void {
  send(socket, { type: "error", code, message });
}

function writeEvent(response: ServerResponse, event: string): void {
  if (response.destroyed || response.writableEnded) {
    return;
  }
  if (response.writableLength > MAX_OUTPUT_BYTES) {
    response.destroy();
    return;
  }
  response.write(event);
}

export function attachRealtime(server: Server): (response: ServerResponse) => void {
  const list = new GiftList();
  const participants = new Map<WebSocket, Participant>();
  const screens = new Set<ServerResponse>();
  const sockets = new WebSocketServer({ noServer: true, maxPayload: 16 * 1024 });

  function rankingEvent(ranking: ReturnType<GiftList["ranking"]>): string {
    const event: ScreenEvent = { type: "ranking", ...ranking };
    return `event: ranking\ndata: ${JSON.stringify(event)}\n\n`;
  }

  function publish(): void {
    const ranking = list.ranking();
    const names = list.names();
    for (const [socket, participant] of participants) {
      send(socket, {
        type: "state",
        ...ranking,
        name: participant.name,
        participants: names,
        votedProductIds: list.votesBy(participant.id),
      });
    }

    const event = rankingEvent(ranking);
    for (const screen of screens) {
      writeEvent(screen, event);
    }
  }

  async function propose(socket: WebSocket, participant: Participant, ids: number[]): Promise<void> {
    const seen = new Set<number>();
    const results = await Promise.all(ids.map((id) => {
      if (seen.has(id)) {
        return { failure: { id, code: "repeated_id" } } as const;
      }
      seen.add(id);
      if (list.hasProduct(id)) {
        return { failure: { id, code: "already_listed" } } as const;
      }
      return lookupProduct(id, participant.connection.signal);
    }));

    if (participant.connection.signal.aborted || socket.readyState !== WebSocket.OPEN) {
      return;
    }

    const addedIds: number[] = [];
    const failures: ProposalFailure[] = [];
    for (const result of results) {
      if ("failure" in result) {
        failures.push(result.failure);
      } else if (list.addProduct(result.product, participant.name)) {
        addedIds.push(result.product.id);
      } else {
        failures.push({ id: result.product.id, code: "already_listed" });
      }
    }

    if (addedIds.length > 0) {
      publish();
    }
    send(socket, { type: "proposal_result", addedIds, failures });
  }

  server.on("upgrade", (request, socket, head) => {
    if (request.url !== "/ws") {
      socket.end("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n");
      return;
    }

    const { host, origin } = request.headers;
    if (origin && origin !== `http://${host}` && origin !== `https://${host}`) {
      socket.end("HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n");
      return;
    }

    sockets.handleUpgrade(request, socket, head, (connection) => {
      sockets.emit("connection", connection, request);
    });
  });

  sockets.on("connection", (socket) => {
    socket.on("error", () => socket.terminate());
    socket.on("close", () => {
      const participant = participants.get(socket);
      if (participant) {
        participant.connection.abort();
        participants.delete(socket);
        list.leave(participant.id);
        publish();
      }
    });

    socket.on("message", (data, isBinary) => {
      if (isBinary) {
        sendError(socket, "invalid_message", "Enviá un mensaje JSON de texto.");
        return;
      }
      const parsed = parseMessage(data.toString());
      if (!parsed.ok) {
        sendError(socket, "invalid_message", parsed.error);
        return;
      }

      const message = parsed.message;
      const participant = participants.get(socket);
      if (message.type === "join") {
        if (participant) {
          sendError(socket, "already_joined", "Ya ingresaste con un nombre.");
          return;
        }

        const id = randomUUID();
        if (!list.join(id, message.name)) {
          sendError(socket, "name_taken", "Ya hay una persona conectada con ese nombre.");
          return;
        }

        participants.set(socket, { id, name: message.name, connection: new AbortController() });
        publish();
        return;
      }

      if (!participant) {
        sendError(socket, "join_required", "Ingresá tu nombre antes de operar con la lista.");
        return;
      }

      switch (message.type) {
        case "propose":
          void propose(socket, participant, message.ids);
          break;
        case "vote": {
          const result = list.vote(participant.id, message.productId, message.voted);
          if (result === "updated") {
            publish();
          } else if (result === "unknown_product") {
            sendError(socket, result, "El producto no está en la lista.");
          }
          break;
        }
        case "remove": {
          const result = list.remove(message.productId);
          if (result === "removed") {
            publish();
          } else {
            const error = result === "unknown_product"
              ? "El producto no está en la lista."
              : "Solo se pueden quitar productos sin votos.";
            sendError(socket, result, error);
          }
          break;
        }
      }
    });
  });

  return (response) => {
    response.writeHead(200, {
      "Content-Type": "text/event-stream; charset=utf-8",
      "Cache-Control": "no-cache, no-transform",
    });
    screens.add(response);
    writeEvent(response, rankingEvent(list.ranking()));
    const keepAlive = setInterval(() => writeEvent(response, ": keep-alive\n\n"), SSE_KEEPALIVE_MS);
    keepAlive.unref();
    response.on("close", () => {
      clearInterval(keepAlive);
      screens.delete(response);
    });
  };
}
