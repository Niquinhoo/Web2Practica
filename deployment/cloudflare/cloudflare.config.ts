import { bindings, defineConfig, defineContainer, exports } from "cf/config";
import * as entrypoint from "./src/index.ts" with { type: "cf-worker" };
const api = defineContainer({
  name: "web2-practica-tp3-api",
  schedulingPolicy: "durable-object",
  images: { base: { dockerfile: "../../api-blank/Dockerfile", buildContext: "../../api-blank" } },
  observability: { enabled: true },
});
export default defineConfig({
  worker: {
    name: "web2-practica-tp3",
    entrypoint,
    compatibilityDate: "2026-10-29",
    workersDev: true,
    observability: { enabled: true },
    exports: { ApiContainer: exports.durableObject({ storage: "sqlite", container: api }) },
    env: {
      DATABASE_URL: bindings.secret(),
      POSTGRES_USER: bindings.secret(),
      POSTGRES_PASSWORD: bindings.secret(),
      JWT_SECRETO: bindings.secret(),
      PG_TUNNEL_HOSTNAME: bindings.secret(),
      TUNNEL_SERVICE_TOKEN_ID: bindings.secret(),
      TUNNEL_SERVICE_TOKEN_SECRET: bindings.secret(),
    },
  },
  containers: [api],
});
