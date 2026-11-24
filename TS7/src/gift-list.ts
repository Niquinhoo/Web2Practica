import type { CatalogProduct, RankedProduct, Ranking } from "./protocol.js";

interface ListedProduct {
  product: CatalogProduct;
  proposedBy: string;
  voters: Set<string>;
}

export class GiftList {
  private readonly participants = new Map<string, string>();
  private readonly products = new Map<number, ListedProduct>();

  join(id: string, name: string): boolean {
    const key = name.toLocaleLowerCase("es");
    for (const connectedName of this.participants.values()) {
      if (connectedName.toLocaleLowerCase("es") === key) {
        return false;
      }
    }
    this.participants.set(id, name);
    return true;
  }

  leave(id: string): boolean {
    if (!this.participants.delete(id)) {
      return false;
    }

    for (const { voters } of this.products.values()) {
      voters.delete(id);
    }
    return true;
  }

  hasProduct(id: number): boolean {
    return this.products.has(id);
  }

  addProduct(product: CatalogProduct, proposedBy: string): boolean {
    if (this.products.has(product.id)) {
      return false;
    }

    this.products.set(product.id, { product, proposedBy, voters: new Set() });
    return true;
  }

  vote(participantId: string, productId: number, voted: boolean): "updated" | "unchanged" | "unknown_product" {
    const item = this.products.get(productId);
    if (!item) {
      return "unknown_product";
    }

    if (!voted) {
      return item.voters.delete(participantId) ? "updated" : "unchanged";
    }
    if (item.voters.has(participantId)) {
      return "unchanged";
    }

    item.voters.add(participantId);
    return "updated";
  }

  remove(productId: number): "removed" | "unknown_product" | "product_has_votes" {
    const item = this.products.get(productId);
    if (!item) {
      return "unknown_product";
    }
    if (item.voters.size > 0) {
      return "product_has_votes";
    }
    this.products.delete(productId);
    return "removed";
  }

  ranking(): Ranking {
    const products: RankedProduct[] = Array.from(this.products.values(), ({ product, proposedBy, voters }) => ({
      ...product,
      proposedBy,
      votes: voters.size,
    }));
    products.sort((left, right) => right.votes - left.votes || left.id - right.id);
    return { products, participantCount: this.participants.size };
  }

  names(): string[] {
    return Array.from(this.participants.values());
  }

  votesBy(participantId: string): number[] {
    const ids: number[] = [];
    for (const [id, { voters }] of this.products) {
      if (voters.has(participantId)) {
        ids.push(id);
      }
    }
    return ids;
  }
}
