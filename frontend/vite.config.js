import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/auth-api": {
        target: "http://localhost:8084",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/auth-api/, "")
      },
      "/customer-api": {
        target: "http://localhost:8085",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/customer-api/, "")
      },
      "/account-api": {
        target: "http://localhost:8082",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/account-api/, "")
      },
      "/transaction-api": {
        target: "http://localhost:8086",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/transaction-api/, "")
      },
      "/gateway-api": {
        target: "http://localhost:8083",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/gateway-api/, "")
      }
    }
  }
});
