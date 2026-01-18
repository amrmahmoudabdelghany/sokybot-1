import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

import path from "path"

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // nodePolyfills temporarily removed - was overwriting source files
  ],
  build: {
    outDir: '../../../target/frontend/dist',
    emptyOutDir: true,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})
