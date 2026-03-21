/**
 * 将 node_modules/vditor/dist 复制到 public/vditor/dist，
 * 使 Vditor 编辑器从同源加载 lute、i18n、katex 等资源，避免 CDN 跨域/Referrer 导致加载失败。
 */
const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..')
const src = path.join(projectRoot, 'node_modules', 'vditor', 'dist')
const dest = path.join(projectRoot, 'public', 'vditor', 'dist')

if (!fs.existsSync(src)) {
  console.warn('copy-vditor: node_modules/vditor/dist 不存在，跳过复制')
  process.exit(0)
}

fs.mkdirSync(path.dirname(dest), { recursive: true })
if (fs.existsSync(dest)) {
  fs.rmSync(dest, { recursive: true })
}
fs.cpSync(src, dest, { recursive: true })
console.log('copy-vditor: 已复制 vditor/dist 到 public/vditor/dist')
