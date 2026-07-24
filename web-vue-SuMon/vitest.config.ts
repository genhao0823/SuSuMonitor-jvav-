import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'

/**
 * Vitest 配置:jsdom 环境 + Vue 3 SFC 支持 + @ 别名 + 覆盖率。
 *
 * 与现有 vite.config.ts 保持环境隔离(本文件用 vitest/config 而非 vite/config)。
 */

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.spec.ts'],
    exclude: ['node_modules/**', 'dist/**'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'text-summary', 'html'],
      include: ['src/stores/**', 'src/utils/**', 'src/composables/**'],
      exclude: ['**/*.spec.ts', '**/types/**'],
      reportsDirectory: './coverage'
    }
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  }
})
