import { existsSync } from "node:fs";
import { env, loadEnvFile } from "node:process";

if (existsSync(".env")) {
  loadEnvFile(".env");
}

function integer(name: string, fallback: number, min: number, max: number): number {
  const raw = env[name];
  if (raw === undefined) {
    return fallback;
  }

  const value = Number(raw);
  if (!/^\d+$/.test(raw) || !Number.isSafeInteger(value) || value < min || value > max) {
    throw new Error(`${name} debe ser un entero entre ${min} y ${max}.`);
  }

  return value;
}

const catalogUrl = new URL(env.CATALOG_URL ?? "https://dummyjson.com");
if (
  !["http:", "https:"].includes(catalogUrl.protocol) ||
  catalogUrl.username || catalogUrl.password || catalogUrl.search || catalogUrl.hash
) {
  throw new Error("CATALOG_URL debe ser una URL base HTTP o HTTPS sin credenciales, consulta ni fragmento.");
}

export const config = {
  host: env.HOST ?? "127.0.0.1",
  port: integer("PORT", 3000, 1, 65535),
  catalogUrl,
  catalogTimeoutMs: integer("CATALOG_TIMEOUT_MS", 2500, 1, 30_000),
  catalogDelayMs: integer("CATALOG_DELAY_MS", 0, 0, 5000),
};
