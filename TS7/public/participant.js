import { renderRanking } from "./ranking.js";

const join = document.querySelector("#join");
const nameInput = document.querySelector("#name");
const joinButton = join.querySelector("button");
const room = document.querySelector("#room");
const presence = document.querySelector("#presence");
const notice = document.querySelector("#notice");
const proposal = document.querySelector("#proposal");
const idsInput = document.querySelector("#ids");
const proposeButton = proposal.querySelector("button");
const result = document.querySelector("#proposal-result");
const summary = document.querySelector("#proposal-summary");
const failures = document.querySelector("#proposal-failures");

const failureText = {
  already_listed: "el producto ya está en la lista.",
  repeated_id: "el ID se repitió en esta propuesta.",
  not_found: "el producto no existe.",
  timeout: "el catálogo no respondió a tiempo.",
  catalog_unavailable: "el catálogo no está disponible o devolvió datos inválidos.",
};

let socket;
let joined = false;

function showNotice(text) {
  notice.textContent = text;
  notice.hidden = !text;
}

function send(message) {
  if (socket?.readyState !== WebSocket.OPEN) {
    showNotice("La conexión se cerró. Volvé a ingresar.");
    return false;
  }
  socket.send(JSON.stringify(message));
  return true;
}

function productButton(product, action, label, handler) {
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = label;
  button.setAttribute("aria-label", `${label} · ${product.title}`);
  button.dataset.focusKey = `${action}-${product.id}`;
  button.addEventListener("click", handler);
  return button;
}

function updateState(message) {
  const firstState = !joined;
  joined = true;
  join.hidden = true;
  room.hidden = false;
  presence.textContent = `Conectado como ${message.name}. En línea (${message.participantCount}) · ${message.participants.join(", ")}.`;
  if (firstState) {
    showNotice("");
    idsInput.focus();
  }

  const votedIds = new Set(message.votedProductIds);
  renderRanking(message.products, (product) => {
    const voted = votedIds.has(product.id);
    const vote = productButton(product, "vote", voted ? "Retirar voto" : "Votar", () => {
      showNotice("");
      send({ type: "vote", productId: product.id, voted: !voted });
    });
    const remove = productButton(product, "remove", "Quitar", () => {
      showNotice("");
      send({ type: "remove", productId: product.id });
    });
    remove.className = "secondary";
    remove.disabled = product.votes > 0;
    return [vote, remove];
  });
}

function showProposal(message) {
  proposeButton.disabled = false;
  result.hidden = false;
  summary.textContent = message.addedIds.length
    ? `IDs agregados · ${message.addedIds.join(", ")}.`
    : "No se agregaron productos.";
  failures.replaceChildren(...message.failures.map(({ id, code }) => {
    const item = document.createElement("li");
    item.textContent = `ID ${id} · ${failureText[code]}`;
    return item;
  }));
  failures.hidden = message.failures.length === 0;
}

function openSocket() {
  const protocol = location.protocol === "https:" ? "wss:" : "ws:";
  const connection = new WebSocket(`${protocol}//${location.host}/ws`);
  window.ts7Socket = connection;

  connection.addEventListener("message", ({ data }) => {
    const message = JSON.parse(data);
    switch (message.type) {
      case "state":
        updateState(message);
        break;
      case "proposal_result":
        showProposal(message);
        break;
      case "error":
        showNotice(message.message);
        if (!joined) {
          joinButton.disabled = false;
          nameInput.focus();
        }
        break;
    }
  });
  connection.addEventListener("close", () => {
    socket = undefined;
    window.ts7Socket = undefined;
    joined = false;
    join.hidden = false;
    joinButton.disabled = false;
    room.hidden = true;
    result.hidden = true;
    proposeButton.disabled = false;
    showNotice("Se cerró la conexión. Volvé a ingresar.");
    nameInput.focus();
  });

  return connection;
}

join.addEventListener("submit", (event) => {
  event.preventDefault();
  const message = { type: "join", name: nameInput.value };
  joinButton.disabled = true;
  showNotice("Conectando…");

  if (socket?.readyState === WebSocket.OPEN) {
    send(message);
    return;
  }

  socket = openSocket();
  socket.addEventListener("open", () => send(message), { once: true });
});

proposal.addEventListener("submit", (event) => {
  event.preventDefault();
  const tokens = idsInput.value.trim().split(/[\s,]+/).filter(Boolean);
  const ids = tokens.map(Number);
  if (!ids.length || ids.length > 10 || tokens.some((token, index) => {
    return !/^\d+$/.test(token) || !Number.isSafeInteger(ids[index]) || ids[index] < 1;
  })) {
    showNotice("Ingresá entre 1 y 10 IDs enteros positivos.");
    return;
  }

  if (send({ type: "propose", ids })) {
    showNotice("");
    proposeButton.disabled = true;
    result.hidden = false;
    summary.textContent = "Consultando el catálogo…";
    failures.replaceChildren();
    failures.hidden = true;
  }
});
