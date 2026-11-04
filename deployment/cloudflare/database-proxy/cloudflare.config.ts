import { bindings, defineConfig } from "cf/config";
import * as proxy from "../src/database-proxy.ts" with { type: "cf-worker" };

export default defineConfig({
  worker: {
    name: "web2-tp3-postgres",
    entrypoint: proxy,
    compatibilityDate: "2026-11-03",
    workersDev: true,
    observability: { enabled: true },
    env: {
      DB_NETWORK: bindings.vpcNetwork({ tunnelId: "0ae2c920-94c7-4484-9973-3117f7df4cdd" }),
      TUNNEL_SERVICE_TOKEN_ID: bindings.secret(),
      TUNNEL_SERVICE_TOKEN_SECRET: bindings.secret(),
    },
  },
});
