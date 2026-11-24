import { config } from "./config.js";
import { isRecord, type CatalogProduct, type ProposalFailure } from "./protocol.js";

type LookupResult = { product: CatalogProduct } | { failure: ProposalFailure };

function readProduct(value: unknown, id: number): CatalogProduct | undefined {
  if (
    !isRecord(value) ||
    value.id !== id ||
    typeof value.title !== "string" ||
    !value.title.trim() ||
    typeof value.price !== "number" ||
    !Number.isFinite(value.price) ||
    value.price < 0 ||
    typeof value.thumbnail !== "string"
  ) {
    return;
  }

  const imageUrl = URL.parse(value.thumbnail);
  if (!imageUrl || !["http:", "https:"].includes(imageUrl.protocol)) {
    return;
  }

  return { id, title: value.title, price: value.price, thumbnail: imageUrl.href };
}

export async function lookupProduct(id: number, disconnected: AbortSignal): Promise<LookupResult> {
  const url = new URL(config.catalogUrl);
  url.pathname = `${url.pathname.replace(/\/+$/, "")}/products/${id}`;
  if (config.catalogDelayMs > 0) {
    url.searchParams.set("delay", String(config.catalogDelayMs));
  }

  const timeout = AbortSignal.timeout(config.catalogTimeoutMs);
  const signal = AbortSignal.any([timeout, disconnected]);

  try {
    const response = await fetch(url, { signal });
    if (!response.ok) {
      await response.body?.cancel();
      const code = response.status === 404 ? "not_found" : "catalog_unavailable";
      return { failure: { id, code } };
    }

    const product = readProduct(await response.json(), id);
    return product ? { product } : { failure: { id, code: "catalog_unavailable" } };
  } catch {
    return { failure: { id, code: timeout.aborted ? "timeout" : "catalog_unavailable" } };
  }
}
