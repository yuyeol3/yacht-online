import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    global: "globalThis"
  },
  server: {
    proxy : {
      '/auth' : {
        target : "http://localhost:8080",
        changeOrigin : true
      },
      '/api' : {
        target : "http://localhost:8080",
        ws : true,
        changeOrigin : true
      }
    },
    port: 5174
  }
})
