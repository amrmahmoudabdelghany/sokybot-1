import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

import { nodePolyfills } from 'vite-plugin-node-polyfills'
import path from "path"

const viteUiBuildId = JSON.stringify(process.env.VITE_UI_BUILD_ID || 'dev')

// https://vite.dev/config/
export default defineConfig({
  define: {
    'import.meta.env.VITE_UI_BUILD_ID': viteUiBuildId,
  },
  optimizeDeps: {
    include: ['json-rules-engine'],
  },
  plugins: [
    react(),
    nodePolyfills({
      include: ['buffer', 'process', 'util', 'stream'],
      globals: {
        Buffer: true,
        global: true,
        process: true,
      },
    }),
  ],
  build: {
    outDir: '../../../target/frontend/dist',
    emptyOutDir: true,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "react": path.resolve(__dirname, "node_modules/react"),
      "react-dom": path.resolve(__dirname, "node_modules/react-dom"),
      // xstate / json-rules-engine pull these shims; explicit paths for Rollup
      "vite-plugin-node-polyfills/shims/global": path.resolve(
        __dirname,
        "node_modules/vite-plugin-node-polyfills/shims/global/dist/index.js"
      ),
      "vite-plugin-node-polyfills/shims/process": path.resolve(
        __dirname,
        "node_modules/vite-plugin-node-polyfills/shims/process/dist/index.js"
      ),
      "vite-plugin-node-polyfills/shims/buffer": path.resolve(
        __dirname,
        "node_modules/vite-plugin-node-polyfills/shims/buffer/dist/index.js"
      ),
    },
  },
  server: {
    allowedHosts: ['sokybot.local'],
    proxy: {
      '/rsocket': {
        target: 'ws://127.0.0.1:7000',
        ws: true,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/rsocket/, '')
      }
    }
  }
})
