import { marked } from 'marked'
import markedKatex from 'marked-katex-extension'

const options = { throwOnError: false, nonStandard: true }
marked.use(markedKatex(options))

/**
 * 将 Markdown 字符串转为 HTML（支持 GFM、数学公式 $ / $$）
 * 用于文章详情等需要正确渲染公式的场景。
 */
export function renderMarkdownWithMath(md: string): string {
  if (!md || typeof md !== 'string') return ''
  return marked.parse(md, { gfm: true, breaks: true }) as string
}
