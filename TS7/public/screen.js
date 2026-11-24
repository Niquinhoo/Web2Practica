import { renderRanking } from "./ranking.js";

const presence = document.querySelector("#presence");
const connection = document.querySelector("#connection");
const events = new EventSource("/events");

events.addEventListener("ranking", ({ data }) => {
  const { products, participantCount } = JSON.parse(data);
  presence.textContent = participantCount === 1
    ? "1 participante conectado"
    : `${participantCount} participantes conectados`;
  connection.hidden = true;
  renderRanking(products);
});

events.addEventListener("error", () => {
  connection.textContent = "Se interrumpió la conexión. Reconectando…";
  connection.hidden = false;
});
