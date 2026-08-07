import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // 后端代理目标：默认生产环境；连本地后端时在 .env.development.local 中设置
  // VITE_API_TARGET=http://127.0.0.1:8001
  const apiTarget = env.VITE_API_TARGET || 'https://sms.ink-ism.vip'

  return {
    plugins: [
      vue(),
      // Element Plus API 按需自动引入（ElMessage / ElMessageBox 等）
      AutoImport({
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/auto-imports.d.ts'
      }),
      // Element Plus 组件按需自动引入（样式随组件一并引入）
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/components.d.ts'
      })
    ],
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
    },
  }
})
