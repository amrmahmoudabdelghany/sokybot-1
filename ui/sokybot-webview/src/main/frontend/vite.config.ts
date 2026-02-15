import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

import { nodePolyfills } from 'vite-plugin-node-polyfills'
import path from "path"

// https://vite.dev/config/
export default defineConfig({
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
