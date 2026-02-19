<template>
  <div class="write-layout">
    <!-- 顶栏：Logo / 返回 + 发布文章 + 头像（仿 CSDN，BBC 黑白风） -->
    <header class="write-header">
      <div class="header-left">
        <router-link to="/recommend" class="logo-link">
          <img src="/logo3.png" alt="Logo" class="creator-logo" />
        </router-link>
        <router-link to="/creator" class="back-link">
          <el-icon><ArrowLeft /></el-icon>
        </router-link>
        <span class="publish-title">发布文章</span>
      </div>
      <div class="header-spacer"></div>
      <div class="header-right">
        <span class="write-avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
          <span v-else class="avatar-initial">{{ avatarInitial }}</span>
        </span>
      </div>
    </header>

    <!-- 第二行：工具栏（仿 CSDN 顶部按钮，调用 Vditor API） -->
    <div class="write-toolbar">
      <button type="button" class="tool-btn" @click="onUndo">
        <el-icon><RefreshLeft /></el-icon>
        <span class="tool-label">撤销</span>
      </button>
      <button type="button" class="tool-btn" @click="onRedo">
        <el-icon><RefreshRight /></el-icon>
        <span class="tool-label">重做</span>
      </button>
      <el-divider direction="vertical" />
      <el-dropdown @command="onHeadingCommand">
        <button type="button" class="tool-btn">
          <span class="tool-icon">H</span>
          <span class="tool-label">标题</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="1">一级标题</el-dropdown-item>
            <el-dropdown-item command="2">二级标题</el-dropdown-item>
            <el-dropdown-item command="3">三级标题</el-dropdown-item>
            <el-dropdown-item command="4">四级标题</el-dropdown-item>
            <el-dropdown-item command="5">五级标题</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button type="button" class="tool-btn" @click="onBold">
        <span class="tool-icon">B</span>
        <span class="tool-label">加粗</span>
      </button>
      <button type="button" class="tool-btn" @click="onItalic">
        <span class="tool-icon">I</span>
        <span class="tool-label">斜体</span>
      </button>
      <el-divider direction="vertical" />
      <el-dropdown @command="onListCommand">
        <button type="button" class="tool-btn">
          <span class="tool-icon">≡</span>
          <span class="tool-label">列表</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="bullet">无序列表</el-dropdown-item>
            <el-dropdown-item command="ordered">有序列表</el-dropdown-item>
            <el-dropdown-item command="task">任务列表</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button type="button" class="tool-btn" @click="onAlign">
        <span class="tool-icon">≡</span>
        <span class="tool-label">对齐</span>
      </button>
      <el-divider direction="vertical" />
      <button type="button" class="tool-btn" @click="onCode">
        <span class="tool-icon">&lt;/&gt;</span>
        <span class="tool-label">代码</span>
      </button>
      <button type="button" class="tool-btn" @click="onImage">
        <span class="tool-icon">🖼</span>
        <span class="tool-label">图片</span>
      </button>
      <button type="button" class="tool-btn" @click="onLink">
        <span class="tool-icon">🔗</span>
        <span class="tool-label">链接</span>
      </button>
    </div>

    <!-- 主体：目录 | 编辑区 | AI助手 -->
    <div class="write-body">
      <aside class="toc-sidebar" :class="{ 'toc-sidebar-collapsed': tocSidebarCollapsed }">
        <div class="toc-header">
          <span>目录</span>
          <button type="button" class="toc-toggle-panel" @click="tocSidebarCollapsed = !tocSidebarCollapsed" :aria-label="tocSidebarCollapsed ? '展开目录' : '收起目录'">
            <el-icon><DArrowRight v-if="tocSidebarCollapsed" /><DArrowLeft v-else /></el-icon>
          </button>
        </div>
        <div v-show="!tocSidebarCollapsed" class="toc-body">
          <template v-if="tocList.length">
            <div v-for="(item, index) in tocList" :key="index" v-show="isTocItemVisible(index)" class="toc-item" :style="{ paddingLeft: (item.level - 1) * 14 + 4 + 'px' }">
              <span v-if="hasTocChildren(index)" class="toc-arrow" @click="toggleToc(index)">{{ tocExpanded.has(sectionOwner(index)) ? '▼' : '▶' }}</span>
              <span v-else class="toc-arrow-placeholder"></span>
              <span class="toc-text">{{ item.text }}</span>
            </div>
          </template>
          <p v-else class="toc-placeholder">为文内增加标题，这里将生成目录</p>
        </div>
      </aside>
      <main class="editor-main">
        <div class="editor-paper">
          <el-input
            v-model="title"
            placeholder="请输入文章标题 (5~100个字)"
            class="title-input"
            maxlength="100"
            show-word-limit
          />
          <div class="title-hint" v-if="title.length > 0 && title.length < 5">还需输入{{ 5 - title.length }}个字</div>
          <div ref="vditorRef" class="vditor-wrap"></div>
        </div>
      </main>
      <aside class="ai-sidebar" :class="{ 'ai-sidebar-collapsed': aiSidebarCollapsed }">
        <div class="ai-card">
          <div class="ai-header" @click="aiSidebarCollapsed = !aiSidebarCollapsed">
            <div class="ai-logo">AI</div>
            <span class="ai-title-text">AI助手</span>
            <span class="ai-title-arrow">{{ aiSidebarCollapsed ? '〈' : '〉' }}</span>
          </div>
          <div v-show="!aiSidebarCollapsed" class="ai-list">
            <div class="ai-item">大纲生成</div>
            <div class="ai-item">代码生成</div>
            <div class="ai-item">学术搜索</div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 底栏 -->
    <footer class="write-footer">
      <div class="footer-left">
        <span class="word-count">共{{ wordCount }}字</span>
        <span class="footer-link">发文设置 <el-icon><ArrowDown /></el-icon></span>
      </div>
      <div class="footer-right">
        <el-button class="footer-btn">保存草稿 <el-icon><ArrowDown /></el-icon></el-button>
        <el-button class="footer-btn">定时发布 &gt;</el-button>
        <el-button type="primary" class="publish-btn">发布博客</el-button>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/stores/user'
import { ArrowLeft, ArrowDown, DArrowRight, DArrowLeft, RefreshLeft, RefreshRight } from '@element-plus/icons-vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const userStore = useUserStore()
const title = ref('')
const vditorRef = ref<HTMLElement | null>(null)
const wordCount = ref(0)

// 目录：从正文解析的标题列表；折叠状态（一级标题索引）；整栏收起
const tocList = ref<{ level: number; text: string }[]>([])
const tocExpanded = ref<Set<number>>(new Set())
const tocSidebarCollapsed = ref(false)
const aiSidebarCollapsed = ref(false)

const sectionOwners = computed(() => {
  const list = tocList.value
  const out: number[] = []
  let last = -1
  for (let i = 0; i < list.length; i++) {
    if (list[i].level === 1) last = i
    out.push(last)
  }
  return out
})

function sectionOwner(index: number): number {
  return sectionOwners.value[index] ?? -1
}

function isTocItemVisible(index: number): boolean {
  const s = sectionOwner(index)
  return s >= 0 && tocExpanded.value.has(s)
}

function hasTocChildren(index: number): boolean {
  const list = tocList.value
  return index + 1 < list.length && list[index + 1].level > list[index].level
}

function toggleToc(index: number) {
  const s = sectionOwner(index)
  if (s < 0) return
  const next = new Set(tocExpanded.value)
  if (next.has(s)) next.delete(s)
  else next.add(s)
  tocExpanded.value = next
}

function updateTocFromMarkdown(md: string) {
  const lines = md.split(/\r?\n/)
  const items: { level: number; text: string }[] = []
  const re = /^(#{1,6})\s+(.+)$/
  for (const line of lines) {
    const m = line.match(re)
    if (m) {
      const level = Math.min(m[1].length, 5)
      const text = m[2].trim() || '无标题'
      items.push({ level, text })
    }
  }
  tocList.value = items
  tocExpanded.value = new Set(items.map((_, i) => i).filter((i) => items[i].level === 1))
}

let vditor: Vditor | null = null

onMounted(() => {
  if (!vditorRef.value) return
  vditor = new Vditor(vditorRef.value, {
    height: 420,
    placeholder: '#创作灵感#\n记录工作实践、项目复盘\n写技术笔记巩固知识要点\n发表职场感悟心得',
    lang: 'zh_CN',
    mode: 'wysiwyg',
    theme: 'classic',
    cache: { enable: false },
    counter: {
      enable: true,
      type: 'markdown',
      after(length: number) {
        wordCount.value = length
      },
    },
    input(value: string) {
      updateTocFromMarkdown(value ?? '')
    },
    after() {
      if (vditor) updateTocFromMarkdown(vditor.getValue() ?? '')
    },
  })
})

onBeforeUnmount(() => {
  vditor?.destroy()
  vditor = null
})

function getMarkdownValue(): string {
  return vditor?.getValue() ?? ''
}

// 工具栏操作封装
function wrapSelection(prefix: string, suffix?: string) {
  if (!vditor) return
  const sel = vditor.getSelection()
  const end = suffix ?? prefix
  if (sel) {
    vditor.updateValue(`${prefix}${sel}${end}`)
  } else {
    vditor.insertValue(`${prefix}${end}`, true)
  }
}

function insertAtCursor(text: string) {
  if (!vditor) return
  vditor.insertValue(text, true)
}

function insertMD(md: string) {
  if (!vditor) return
  vditor.insertMD(md)
}

function onUndo() {
  // Vditor 未公开 undo/redo API，这里暂留占位，未来可考虑自定义历史栈
}

function onRedo() {
  // 同上，占位
}

function onBold() {
  if (!vditor) return
  const sel = vditor.getSelection() || '加粗文本'
  insertMD(`**${sel}**`)
}

function onItalic() {
  if (!vditor) return
  const sel = vditor.getSelection() || '斜体文本'
  insertMD(`*${sel}*`)
}

function onBulletList() {
  insertMD('\n- 列表项\n')
}

function onListCommand(type: string) {
  if (!vditor) return
  const sel = vditor.getSelection()
  if (sel) {
    const lines = sel.split(/\r?\n/).filter((l) => l.length > 0)
    if (!lines.length) return
    let md = ''
    if (type === 'bullet') {
      md = lines.map((line) => `- ${line.replace(/^[-*+]\s+/, '')}`).join('\n')
    } else if (type === 'ordered') {
      md = lines.map((line, idx) => `${idx + 1}. ${line.replace(/^\d+\.\s+/, '')}`).join('\n')
    } else if (type === 'task') {
      md = lines.map((line) => `- [ ] ${line.replace(/^(-\s+)?(\[.\]\s+)?/, '')}`).join('\n')
    }
    insertMD(`\n${md}\n`)
  } else {
    if (type === 'bullet') {
      insertMD('\n- 列表项 1\n- 列表项 2\n')
    } else if (type === 'ordered') {
      insertMD('\n1. 列表项 1\n2. 列表项 2\n')
    } else if (type === 'task') {
      insertMD('\n- [ ] 待办事项 1\n- [ ] 待办事项 2\n')
    }
  }
}

function onAlign() {
  // Markdown 本身没有对齐语义，这里仅保留占位
}

function onCode() {
  insertMD('\n```lang\n代码块\n```\n')
}

function onImage() {
  insertMD('![描述](https://example.com/image.png)\n')
}

function onLink() {
  if (!vditor) return
  const sel = vditor.getSelection() || '链接文本'
  insertMD(`[${sel}](https://example.com)\n`)
}

function onHeadingCommand(level: string | number) {
  const n = Number(level)
  if (!vditor) return
  const hashes = '#'.repeat(n >= 1 && n <= 6 ? n : 1)
  const sel = vditor.getSelection()
  if (sel) {
    const lines = sel.split(/\r?\n/)
    const md = lines
      .map((line) => {
        const pure = line.replace(/^(#{1,6})\s+/, '').trim()
        return `${hashes} ${pure || '标题'}`
      })
      .join('\n')
    insertMD(md)
  } else {
    insertMD(`\n${hashes} 标题\n`)
  }
}

const avatarUrl = computed(() => (userStore.userInfo as { avatar?: string })?.avatar || '')
const avatarInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || '用'
  return name.charAt(0).toUpperCase()
})
</script>

<style scoped>
.write-layout {
  min-height: 100vh;
  background: #f5f5f7;
  display: flex;
  flex-direction: column;
  /* 预留底部发布栏高度，避免内容被遮挡 */
  padding-bottom: 56px;
}

.logo-link {
  display: flex;
  align-items: center;
  margin-right: 4px;
}

.creator-logo {
  height: 40px; /* 与首页 Logo 一致 */
  width: auto;
}

.write-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 24px;
  background: #fff;
  z-index: 90;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-spacer {
  flex: 1;
}

.back-link {
  color: #333;
  display: flex;
  align-items: center;
}

.back-link:hover {
  color: #111;
}

.publish-title {
  font-size: 16px;
  font-weight: 600;
  color: #111;
}

.dropdown-arrow {
  font-size: 12px;
  color: #999;
}

.header-right {
  flex-shrink: 0;
}

/* 第二行工具栏，单独一行，类似 CSDN */
.write-toolbar {
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  height: 40px;
  border-bottom: 1px solid #e0e0e0;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
  background: #fff;
  z-index: 89;
  justify-content: center;
}

.tool-btn {
  padding: 4px 6px;
  min-width: 40px;
  height: auto;
  color: #666;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.tool-icon {
  font-size: 14px;
}

.tool-label {
  font-size: 12px;
}

.tool-btn:hover {
  color: #111 !important;
}

.write-toolbar :deep(.el-divider--vertical) {
  height: 16px;
  margin: 0 4px;
}

.write-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #111;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.write-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.write-avatar .avatar-initial {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.write-body {
  flex: 1;
  /* 顶部两行固定：64 顶栏 + 40 工具栏；底部 56 固定栏在 layout 上用 padding-bottom 预留 */
  margin-top: 104px;
  position: relative;
}

.toc-sidebar {
  position: fixed;
  top: 112px;
  left: 24px;
  width: 200px;
  border-right: 1px solid #e0e0e0;
  padding: 20px 16px;
  background: #fafafa;
  transition: padding 0.2s ease, box-shadow 0.2s ease;
  z-index: 30;
}

.toc-sidebar-collapsed {
  /* 收起时仅收紧内边距和隐藏文字，位置不变；右侧阴影增强层次 */
  padding: 20px 4px 20px 8px;
  box-shadow: 4px 0 12px rgba(0, 0, 0, 0.06), 2px 0 6px rgba(0, 0, 0, 0.04);
}

.toc-sidebar-collapsed .toc-header {
  justify-content: center;
  margin-bottom: 0;
}

.toc-sidebar-collapsed .toc-header span {
  display: none;
}

.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #111;
  margin-bottom: 16px;
}

.toc-toggle-panel {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border: none;
  background: transparent;
  color: #333;
  cursor: pointer;
  border-radius: 2px;
}

.toc-toggle-panel:hover {
  color: #111;
  background: #eee;
}

.toc-body {
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.toc-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  line-height: 1.5;
  padding: 4px 0;
  color: #333;
  cursor: default;
}

.toc-item .toc-text {
  color: #333;
}

.toc-arrow {
  flex-shrink: 0;
  width: 16px;
  font-size: 10px;
  color: #555;
  cursor: pointer;
  user-select: none;
}

.toc-arrow:hover {
  color: #111;
}

.toc-arrow-placeholder {
  flex-shrink: 0;
  width: 16px;
  display: inline-block;
}

.toc-placeholder {
  font-size: 13px;
  color: #999;
  line-height: 1.6;
  margin: 0;
}

.editor-main {
  min-width: 0;
  padding: 24px 32px;
  /* 预留左右浮窗位置，避免内容被遮挡 */
  margin: 0 220px 0 260px;
}

.editor-paper {
  max-width: 880px;
  margin: 0 auto 40px;
  padding: 24px 32px 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow:
    0 0 0 1px rgba(15, 23, 42, 0.04),
    0 18px 45px rgba(15, 23, 42, 0.12);
}

.title-input {
  font-size: 20px;
  margin-bottom: 8px;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 0;
  padding: 8px 0;
}

.title-input :deep(.el-input__inner) {
  font-size: 20px;
  font-weight: 500;
}

.title-hint {
  font-size: 12px;
  color: #999;
  margin-bottom: 16px;
}

.vditor-wrap {
  margin-top: 8px;
}

.ai-sidebar {
  position: fixed;
  top: 112px;
  right: 24px;
  width: 220px;
  padding: 0;
  border-left: none;
  background: transparent;
  transition: width 0.2s ease;
  z-index: 30;
}

.ai-sidebar-collapsed {
  width: 150px;
}

.ai-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 12px 14px;
  box-shadow:
    0 10px 30px rgba(15, 23, 42, 0.16),
    0 0 0 1px rgba(15, 23, 42, 0.04);
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.ai-logo {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(135deg, #7c3aed, #6366f1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.ai-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.ai-title-arrow {
  margin-left: auto;
  font-size: 14px;
  color: #9ca3af;
}

.ai-list {
  margin-top: 10px;
}

.ai-item {
  font-size: 13px;
  color: #666;
  padding: 6px 0;
  cursor: pointer;
}

.ai-item:hover {
  color: #111;
}

.write-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 56px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  z-index: 100;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.word-count {
  font-size: 14px;
  color: #666;
}

.footer-link {
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.footer-link:hover {
  color: #111;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-btn {
  color: #666 !important;
}

.publish-btn {
  background: #111 !important;
  border-color: #111 !important;
}

.publish-btn:hover {
  background: #333 !important;
  border-color: #333 !important;
}
</style>
