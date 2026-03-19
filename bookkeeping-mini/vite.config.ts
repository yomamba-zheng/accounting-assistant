import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    host: '0.0.0.0',
    port: 5173
  }
})

// 【核心修复】在插件加载前，强制初始化 global.uniPlugin
// 防止旧版 uni-cli-shared 读取 undefined 报错
if (typeof global !== 'undefined' && !global.uniPlugin) {
  global.uniPlugin = {
    options: {},
    defaultMode: 'component',
    extNames: {}
  }
}
// 兼容 window 对象 (某些环境下)
if (typeof window !== 'undefined' && !window.uniPlugin) {
  window.uniPlugin = global.uniPlugin
}
