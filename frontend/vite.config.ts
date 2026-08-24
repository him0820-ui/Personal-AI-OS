import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    allowedHosts:['www.u1639516.nyat.app'],
    port: 5175,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        configure(proxy) {
          proxy.on('proxyReqWs', (proxyReq, req) => {
            console.log('收到 WebSocket 请求:', req.url)
          })
        }
      }
    }
  },
  define: {
    'global': {}
  }
})
