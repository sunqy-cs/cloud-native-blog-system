/**
 * Vditor 静态资源基地址。
 * 使用同源地址（public/vditor/dist），避免从外网 CDN 加载 lute、i18n、katex 时
 * 因 CORS / Referrer 策略导致 zh_CN.js、lute.min.js 等加载失败，进而编辑器无法初始化、不显示光标。
 * 需在 dev/build 前执行 npm run copy:vditor 将 node_modules/vditor/dist 复制到 public/vditor/dist。
 */
export const VDITOR_CDN =
  typeof window !== 'undefined'
    ? `${window.location.origin}/vditor`
    : '/vditor'
