const prices = new Intl.NumberFormat("es-AR", { style: "currency", currency: "USD" });
const products = document.querySelector("#products");
const empty = document.querySelector("#empty");

export function renderRanking(items, actions) {
  const focused = document.activeElement;
  const focusKey = products.contains(focused) ? focused.dataset.focusKey : undefined;
  let restoreFocus;

  const entries = items.map((product) => {
    const row = document.createElement("li");
    row.className = "product";
    row.dataset.id = String(product.id);

    const image = document.createElement("img");
    image.src = product.thumbnail;
    image.alt = "";
    image.width = 54;
    image.height = 54;
    image.referrerPolicy = "no-referrer";

    const details = document.createElement("div");
    const title = document.createElement("strong");
    title.textContent = product.title;
    const detail = document.createElement("small");
    detail.textContent = `ID ${product.id} · ${prices.format(product.price)} · ${product.proposedBy}`;
    details.append(title, detail);

    const votes = document.createElement("span");
    votes.className = "votes";
    votes.textContent = product.votes === 1 ? "1 voto" : `${product.votes} votos`;
    row.append(image, details, votes);

    if (actions) {
      const controls = document.createElement("div");
      controls.className = "controls";
      for (const button of actions(product)) {
        if (focusKey && button.dataset.focusKey === focusKey && !button.disabled) {
          restoreFocus = button;
        }
        controls.append(button);
      }
      row.append(controls);
    }
    return row;
  });

  products.replaceChildren(...entries);
  empty.hidden = items.length > 0;
  restoreFocus?.focus({ preventScroll: true });
}
