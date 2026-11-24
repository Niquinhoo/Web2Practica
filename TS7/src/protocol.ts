export const MAX_PROPOSAL_IDS = 10;
export const MAX_NAME_LENGTH = 30;

export type ClientMessage =
  | { type: "join"; name: string }
  | { type: "propose"; ids: number[] }
  | { type: "vote"; productId: number; voted: boolean }
  | { type: "remove"; productId: number };

export interface CatalogProduct {
  id: number;
  title: string;
  price: number;
  thumbnail: string;
}

export interface RankedProduct extends CatalogProduct {
  proposedBy: string;
  votes: number;
}

export interface Ranking {
  products: RankedProduct[];
  participantCount: number;
}

export type ProposalFailure = {
  id: number;
  code: "already_listed" | "repeated_id" | "not_found" | "timeout" | "catalog_unavailable";
};

export type ActionError =
  | "invalid_message"
  | "join_required"
  | "already_joined"
  | "name_taken"
  | "unknown_product"
  | "product_has_votes";

export type ServerMessage =
  | ({ type: "state"; name: string; participants: string[]; votedProductIds: number[] } & Ranking)
  | { type: "proposal_result"; addedIds: number[]; failures: ProposalFailure[] }
  | { type: "error"; code: ActionError; message: string };

export type ScreenEvent = { type: "ranking" } & Ranking;

type ParsedMessage =
  | { ok: true; message: ClientMessage }
  | { ok: false; error: string };

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isId(value: unknown): value is number {
  return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}

export function parseMessage(text: string): ParsedMessage {
  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch {
    return { ok: false, error: "El mensaje debe ser JSON válido." };
  }

  if (!isRecord(value)) {
    return { ok: false, error: "El mensaje debe ser un objeto JSON." };
  }

  switch (value.type) {
    case "join": {
      const name = typeof value.name === "string" ? value.name.trim().normalize("NFC") : "";
      if (!name || Array.from(name).length > MAX_NAME_LENGTH || /\p{Cc}/u.test(name)) {
        return { ok: false, error: `El nombre debe tener entre 1 y ${MAX_NAME_LENGTH} caracteres y carecer de caracteres de control.` };
      }
      return { ok: true, message: { type: "join", name } };
    }
    case "propose": {
      const ids: unknown = value.ids;
      if (!Array.isArray(ids) || ids.length < 1 || ids.length > MAX_PROPOSAL_IDS || !ids.every(isId)) {
        return { ok: false, error: `Enviá entre 1 y ${MAX_PROPOSAL_IDS} IDs enteros positivos.` };
      }
      return { ok: true, message: { type: "propose", ids } };
    }
    case "vote":
      if (!isId(value.productId) || typeof value.voted !== "boolean") {
        return { ok: false, error: "El voto necesita un productId válido y un valor voted booleano." };
      }
      return { ok: true, message: { type: "vote", productId: value.productId, voted: value.voted } };
    case "remove":
      if (!isId(value.productId)) {
        return { ok: false, error: "Indicá qué producto querés quitar con un productId válido." };
      }
      return { ok: true, message: { type: "remove", productId: value.productId } };
    default:
      return { ok: false, error: "Tipo de mensaje desconocido." };
  }
}
