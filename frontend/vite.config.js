import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em dev, faz proxy de /api e /ws para o backend Spring Boot (porta 8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', ws: true, changeOrigin: true },
      '/ws-sockjs': { target: 'http://localhost:8080', ws: true, changeOrigin: true },
    },
  },
})
