import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

const proxy = {
  '/api': {
    target: process.env.TASKTRACKER_API_TARGET ?? 'http://127.0.0.1:8080',
    changeOrigin: true,
    rewrite: path => path.replace(/^\/api(?=\/|$)/, ''),
  },
}

export default defineConfig({
  plugins: [react()],
  server: { proxy },
  preview: { proxy },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    restoreMocks: true,
  },
})
