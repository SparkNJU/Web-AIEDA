import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
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
  build: {
    // 生产环境不生成sourcemap（减小体积）
    sourcemap: false,
    // 启用 terser 压缩（移除无用代码）
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 移除console语句
        drop_debugger: true, // 移除debugger语句
        pure_funcs: ['console.log'], // 移除指定函数调用
      },
    } as any,
    // 代码分割优化
    rollupOptions: {
      output: {
        // 手动分割代码块
        manualChunks: {
          // 将Vue相关库单独打包
          'vue-vendor': ['vue', 'vue-router'],
          // 将UI库单独打包
          'ui-vendor': ['element-plus'],
          // 将工具库单独打包
          'utils-vendor': ['axios', 'markdown-it', 'katex'],
        },
        // 静态资源命名（带hash，便于长期缓存）
        assetFileNames: (assetInfo) => {
          const name = assetInfo.name || 'unknown';
          const info = name.split('.');
          let extType = info[info.length - 1];
          if (/\.(mp4|webm|ogg|mp3|wav|flac|aac)$/.test(name)) {
            extType = 'media';
          } else if (/\.(png|jpe?g|gif|svg)$/.test(name)) {
            extType = 'img';
          } else if (/\.(woff2?|eot|ttf|otf)$/.test(name)) {
            extType = 'fonts';
          }
          return `assets/${extType}/[name].[hash:8].[ext]`;
        },
        chunkFileNames: 'js/[name].[hash:8].js',
        entryFileNames: 'js/[name].[hash:8].js',
      },
    },
    // 设置chunk大小警告限制
    chunkSizeWarningLimit: 1000, // 1MB
  },
})
