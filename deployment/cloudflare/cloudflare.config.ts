import { bindings, defineConfig, defineContainer, exports } from "cf/config";
import * as entrypoint from "./src/index.ts" with { type: "cf-worker" };
const api = defineContainer({
  name: "web2-practica-tp3-api",
  image: { dockerfile: "../../api-blank/Dockerfile", buildContext: "../../api-blank" },
  schedulingPolicy: "default",
  instanceType: "basic",
  maxInstances: 1,
});
export default defineConfig({
  worker: {
    name: "web2-practica-tp3",
    entrypoint,
    compatibilityDate: "2026-10-29",
    workersDev: true,
    exports: { ApiContainer: exports.durableObject({ storage: "sqlite", container: api }) },
    env: {
      API: bindings.durableObject({ worker: "web2-practica-tp3", exportName: "ApiContainer" }),
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
