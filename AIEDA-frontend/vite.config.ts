import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: 'localhost', // 开发时只允许本地访问
    strictPort: false,
  },
  preview: {
    port: 5174, // 使用5174端口，避免与开发端口5173冲突
    host: '0.0.0.0', // 生产环境允许外部访问
    strictPort: true,
  },
  
})
