import {defineConfig} from "vite";

export default defineConfig({
  base: "/",
  publicDir: "public",
  plugins: [],
  build: {
    outDir: "dist",
    assetsDir: "assets",
    cssCodeSplit: false,
    rollupOptions: {
      plugins: [],
    },
    sourcemap: true
  },
  server: {
    port: 3333,
    strictPort: true,
    logLevel: "debug"
  }
})
