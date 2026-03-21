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
      <button type="button" class="tool-btn" @click="onStrikethrough">
        <span class="tool-icon tool-icon-strike">S</span>
        <span class="tool-label">删除线</span>
      </button>
      <el-divider direction="vertical" />
      <button type="button" class="tool-btn" @click="onBulletList">
        <el-icon><List /></el-icon>
        <span class="tool-label">无序列表</span>
      </button>
      <button type="button" class="tool-btn" @click="onOrderedList">
        <el-icon><Rank /></el-icon>
        <span class="tool-label">有序列表</span>
      </button>
      <button type="button" class="tool-btn" @click="onTaskList">
        <el-icon><CircleCheck /></el-icon>
        <span class="tool-label">任务列表</span>
      </button>
      <button type="button" class="tool-btn" @click="onInsertBefore">
        <el-icon><Top /></el-icon>
        <span class="tool-label">前插入行</span>
      </button>
      <button type="button" class="tool-btn" @click="onInsertAfter">
        <el-icon><Bottom /></el-icon>
        <span class="tool-label">后插入行</span>
      </button>
      <el-divider direction="vertical" />
      <button type="button" class="tool-btn" @click="onQuote">
        <span class="tool-icon">″</span>
        <span class="tool-label">引用</span>
      </button>
      <button type="button" class="tool-btn" @click="onHorizontalRule">
        <span class="tool-icon tool-icon-hr">—</span>
        <span class="tool-label">分隔线</span>
      </button>
      <el-divider direction="vertical" />
      <button type="button" class="tool-btn" @click="onCode">
        <span class="tool-icon">&lt;/&gt;</span>
        <span class="tool-label">代码</span>
      </button>
      <button type="button" class="tool-btn" @click="onInlineCode">
        <span class="tool-icon tool-icon-inline-code" aria-hidden="true">&lt;&nbsp;&gt;</span>
        <span class="tool-label">行内代码</span>
      </button>
      <button type="button" class="tool-btn" @click="onTable">
        <span class="tool-icon">▦</span>
        <span class="tool-label">表格</span>
      </button>
      <input
        ref="imageInputRef"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp"
        class="hidden-input"
        @change="onImageFileChange"
      />
      <button type="button" class="tool-btn" :disabled="imageUploading" @click="triggerImageSelect">
        <span class="tool-icon">🖼</span>
        <span class="tool-label">{{ imageUploading ? '上传中…' : '图片' }}</span>
      </button>
      <button type="button" class="tool-btn" @click="onLink">
        <span class="tool-icon">🔗</span>
        <span class="tool-label">链接</span>
      </button>
    </div>

    <!-- 主体：目录 | 编辑区 | AI助手（展开时正文区预留右侧空间，不挡纸） -->
    <div class="write-body" :class="{ 'ai-panel-expanded': !aiSidebarCollapsed }">
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
          <div class="title-section">
            <div class="title-row">
              <el-input
                v-model="title"
                placeholder="请输入文章标题 (5~100个字)"
                class="title-input"
                maxlength="100"
                show-word-limit
              />
              <el-tooltip content="AI生成标题" placement="top">
                <button type="button" class="ai-title-btn" :disabled="aiTitleUsage >= AI_TITLE_QUOTA" @click="onAiGenerateTitle">
                  <span class="ai-title-btn-icon">✨</span>
                </button>
              </el-tooltip>
            </div>
            <div class="title-hint" v-if="title.length > 0 && title.length < 5">还需输入{{ 5 - title.length }}个字</div>
          </div>
          <div ref="vditorRef" class="vditor-wrap"></div>

          <!-- 发文设置：标签、封面、摘要等（不含 GitCode 备份） -->
          <section class="publish-settings">
            <div class="setting-row">
              <label class="setting-label">文章标签 <span class="required">*</span> <el-tooltip content="必选一个主标签；其他标签可从已有非主标签中多选或由 AI 生成，合计最多 5 个" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
            </div>
            <div class="tag-row">
              <el-select v-model="mainTagId" placeholder="请选择主标签（必选）" class="tag-main-select" clearable>
                <el-option v-for="t in mainTagList" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
              <div class="tag-other-row">
                <span class="tag-custom-label">其他标签：</span>
                <el-select v-model="selectedOtherIds" multiple placeholder="从已有非主标签选择（可选）" class="tag-other-select" clearable @change="onOtherTagSelectChange">
                  <el-option v-for="t in otherTagList" :key="t.id" :label="t.name" :value="t.id" />
                </el-select>
                <el-button type="primary" plain size="small" :loading="aiTagsLoading" :disabled="!getMarkdownValue().trim()" @click="onAiGenerateTags">
                  <span class="ai-summary-icon">✨</span> AI生成标签
                </el-button>
              </div>
              <div v-if="otherTagNamesForShow.length > 0" class="tag-chips">
                <el-tag v-for="(name, idx) in otherTagNamesForShow" :key="idx" size="small" closable @close="onRemoveOtherTag(idx)">{{ name }}</el-tag>
              </div>
            </div>
            <div class="setting-row">
              <label class="setting-label">添加封面 <el-tooltip content="文章封面图，用于列表和详情展示；可本地上传或根据正文 AI 生成" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
            </div>
            <div class="cover-row">
              <div class="cover-left">
                <div class="cover-upload" :class="{ uploading: coverUploading }" @click="!coverUploading && triggerCoverSelect()">
                  <el-icon v-if="coverUploading" class="cover-plus is-loading"><Loading /></el-icon>
                  <template v-else>
                    <el-icon class="cover-plus"><Plus /></el-icon>
                    <span>从本地上传</span>
                  </template>
                </div>
                <el-button type="primary" plain size="small" class="cover-ai-btn" :loading="coverAiLoading" :disabled="!getMarkdownValue().trim()" @click="onAiGenerateCover">
                  <span class="ai-summary-icon">✨</span> AI生成封面
                </el-button>
              </div>
              <div class="cover-placeholder">
                <template v-if="cover">
                  <img :src="cover" alt="封面" class="cover-preview-img" />
                  <button type="button" class="cover-remove-btn" @click="clearCover">移除封面</button>
                </template>
                <span v-else>暂无封面，点击左侧上传</span>
              </div>
            </div>
            <input ref="coverInputRef" type="file" accept="image/*" class="hidden-input" @change="onCoverFileChange" />

            <div class="setting-row">
              <label class="setting-label">文章摘要 <el-tooltip content="摘要会在推荐、列表等场景外露，可手动填写或使用 AI 提取" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
            </div>
            <p class="summary-desc">摘要会在推荐、列表等场景外露，帮助读者快速了解内容</p>
            <div class="summary-wrap">
              <el-input v-model="summary" type="textarea" :rows="4" maxlength="256" show-word-limit placeholder="请输入文章摘要" class="summary-input" />
              <el-button type="primary" plain size="small" class="ai-summary-btn" :loading="aiSummaryLoading" @click="onAiExtractSummary">
                <span class="ai-summary-icon">✨</span> AI提取摘要
              </el-button>
            </div>

            <div class="setting-row">
              <label class="setting-label">分类专栏 <el-tooltip content="将文章归类到某个专栏，便于读者按专题浏览（可选）" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
              <el-select v-model="columnId" placeholder="选择专栏（可选）" clearable class="column-select">
                <el-option v-for="col in columnList" :key="col.id" :label="col.name" :value="col.id" />
              </el-select>
            </div>

            <div class="setting-row">
              <label class="setting-label">文章类型 <el-tooltip content="原创、转载或翻译，用于标识内容来源" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
              <el-radio-group v-model="articleType" class="article-type-group">
                <el-radio value="original">原创</el-radio>
                <el-radio value="reprint">转载</el-radio>
                <el-radio value="translated">翻译</el-radio>
              </el-radio-group>
            </div>

            <div class="setting-row">
              <label class="setting-label">创作声明 <el-tooltip content="可选，不强制选择" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
              <el-select v-model="creationStatement" placeholder="部分内容由AI辅助生成（可选）" class="setting-select" clearable>
                <el-option label="无声明" value="none" />
                <el-option label="部分内容由AI辅助生成" value="ai-assisted" />
                <el-option label="内容来源网络，进行整合/再创作" value="network" />
                <el-option label="个人观点，仅供参考" value="personal" />
              </el-select>
            </div>

            <div class="setting-row">
              <label class="setting-label">可见范围 <el-tooltip content="全部可见：所有人可看；仅我可见：仅自己可见；粉丝可见：仅关注你的用户可见" placement="top"><el-icon class="setting-help"><QuestionFilled /></el-icon></el-tooltip></label>
              <el-radio-group v-model="visibility" class="visibility-group">
                <el-radio value="all">全部可见</el-radio>
                <el-radio value="self">仅我可见</el-radio>
                <el-radio value="fans">粉丝可见</el-radio>
              </el-radio-group>
            </div>
          </section>
        </div>
      </main>
      <aside class="ai-sidebar" :class="{ 'ai-sidebar-collapsed': aiSidebarCollapsed }">
        <div class="ai-card">
          <div class="ai-header" @click="aiSidebarCollapsed = !aiSidebarCollapsed">
            <div class="ai-logo">AI</div>
            <span class="ai-title-text">AI助手</span>
            <span class="ai-title-arrow">{{ aiSidebarCollapsed ? '〈' : '〉' }}</span>
          </div>
          <div v-show="!aiSidebarCollapsed" class="ai-body">
            <div class="ai-oneclick">
              <div class="ai-oneclick-title">一键生成</div>
              <el-select v-model="oneClickBotId" placeholder="选择机器人" class="ai-oneclick-select" clearable>
                <el-option v-for="b in botListForWrite" :key="b.id" :label="b.name" :value="b.id" />
              </el-select>
              <el-input v-model="oneClickPrompt" type="textarea" :rows="3" placeholder="输入主题或描述，如：写一篇关于机器学习入门的知识梳理" class="ai-oneclick-prompt" />
              <el-button type="primary" class="ai-oneclick-btn" :loading="oneClickGenerating" :disabled="!oneClickBotId || !oneClickPrompt.trim()" @click="onOneClickGenerate">
                一键生成
              </el-button>
              <div class="ai-oneclick-steps">
                <div v-for="(step, idx) in oneClickSteps" :key="step.key" class="ai-step" :class="step.status">
                  <span class="ai-step-icon">
                    <el-icon v-if="step.status === 'loading'" class="is-loading"><Loading /></el-icon>
                    <el-icon v-else-if="step.status === 'done'" class="ai-step-done"><CircleCheck /></el-icon>
                    <span v-else class="ai-step-pending">○</span>
                  </span>
                  <span class="ai-step-label">{{ step.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 底栏 -->
    <footer class="write-footer">
      <div class="footer-left">
        <span class="word-count">共{{ wordCount }}字</span>
      </div>
      <div class="footer-right">
        <el-button class="footer-btn" :loading="draftSaving" @click="onSaveDraft">保存草稿 <el-icon><ArrowDown /></el-icon></el-button>
        <el-button type="primary" class="publish-btn" :loading="publishLoading" @click="onPublish">发布博客</el-button>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowLeft, ArrowDown, DArrowRight, DArrowLeft, RefreshLeft, RefreshRight, List, Rank, CircleCheck, Top, Bottom, QuestionFilled, Plus, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { VDITOR_CDN } from '@/utils/vditorCdn'
import { uploadImage } from '@/api/upload'
import { generateCover } from '@/api/ai'
import { saveDraft, getContentForEdit, publishContent } from '@/api/content'
import { generateTitle, generateSummary, generateTags, oneClickGenerate } from '@/api/ai'
import { getColumnsMe, type ColumnItem } from '@/api/column'
import { getMainTags, getOtherTags, type TagItem } from '@/api/tag'
import { getBlogBotsMe, type BlogBotItem } from '@/api/blogBot'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const contentId = ref<number | undefined>(undefined)
const title = ref('')
const vditorRef = ref<HTMLElement | null>(null)
const wordCount = ref(0)

// 目录：从正文解析的标题列表；折叠状态（一级标题索引）；整栏收起
const tocList = ref<{ level: number; text: string }[]>([])
const tocExpanded = ref<Set<number>>(new Set())
const tocSidebarCollapsed = ref(false)
const aiSidebarCollapsed = ref(false)
const imageInputRef = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)

const AI_TITLE_QUOTA = 100
const aiTitleUsage = ref(0)
const aiTitleLoading = ref(false)
const aiSummaryLoading = ref(false)

const coverInputRef = ref<HTMLInputElement | null>(null)
const cover = ref('')
const coverUploading = ref(false)
const coverAiLoading = ref(false)
const summary = ref('')
const draftSaving = ref(false)
const publishLoading = ref(false)
const articleType = ref<'original' | 'reprint' | 'translated'>('original')
const creationStatement = ref('none')
const visibility = ref<'all' | 'self' | 'fans'>('all')
const columnList = ref<ColumnItem[]>([])
const columnId = ref<number | undefined>(undefined)
const mainTagList = ref<TagItem[]>([])
const mainTagId = ref<number | undefined>(undefined)
const otherTagList = ref<TagItem[]>([])
const selectedOtherIds = ref<number[]>([])
const aiGeneratedTagNames = ref<string[]>([])
const aiTagsLoading = ref(false)

// 一键生成：选择 bot、输入 prompt，依次生成正文/标题/封面/主标签，步骤打勾
const botListForWrite = ref<BlogBotItem[]>([])
const oneClickBotId = ref<number | undefined>(undefined)
const oneClickPrompt = ref('')
const oneClickGenerating = ref(false)
const ONE_CLICK_STEP_KEYS = ['body', 'title', 'summary', 'tagNames', 'cover', 'mainTag'] as const
const oneClickSteps = ref<{ key: string; label: string; status: 'pending' | 'loading' | 'done' }[]>([
  { key: 'body', label: '正文', status: 'pending' },
  { key: 'title', label: '标题', status: 'pending' },
  { key: 'summary', label: '摘要', status: 'pending' },
  { key: 'tagNames', label: '小标签', status: 'pending' },
  { key: 'cover', label: '封面', status: 'pending' },
  { key: 'mainTag', label: '主标签', status: 'pending' },
])

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

function triggerCoverSelect() {
  coverInputRef.value?.click()
}

async function onCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  coverUploading.value = true
  try {
    const meta = await uploadImage(file, 'covers')
    if (meta?.url) {
      cover.value = meta.url
      ElMessage.success('封面上传成功')
    }
  } finally {
    coverUploading.value = false
  }
}

function clearCover() {
  cover.value = ''
}

async function onAiGenerateCover() {
  const bodyText = getMarkdownValue().trim().slice(0, 2000)
  if (!bodyText) {
    ElMessage.info('请先输入正文内容')
    return
  }
  if (coverAiLoading.value) return
  coverAiLoading.value = true
  try {
    const res = await generateCover(bodyText)
    if (res?.url) {
      cover.value = res.url
      ElMessage.success('封面生成成功')
    } else {
      ElMessage.warning('生成失败，请重试')
    }
  } catch {
    ElMessage.error('AI 生成封面失败，请重试')
  } finally {
    coverAiLoading.value = false
  }
}

async function onAiExtractSummary() {
  const bodyText = getMarkdownValue().trim().slice(0, 2000)
  if (!bodyText) {
    ElMessage.info('请先输入正文内容')
    return
  }
  if (aiSummaryLoading.value) return
  aiSummaryLoading.value = true
  try {
    const res = await generateSummary(bodyText)
    const s = res?.summary?.trim()
    if (s !== undefined && s !== null) {
      summary.value = s.length > 100 ? s.slice(0, 100) : s
      ElMessage.success('摘要已生成（最多 100 字）')
    } else {
      ElMessage.warning('未能生成摘要，请重试')
    }
  } finally {
    aiSummaryLoading.value = false
  }
}

async function onAiGenerateTitle() {
  if (aiTitleUsage.value >= AI_TITLE_QUOTA) {
    ElMessage.warning('AI 生成标题次数已用尽')
    return
  }
  if (aiTitleLoading.value) return
  const bodyText = getMarkdownValue().trim().slice(0, 2000)
  if (!bodyText) {
    ElMessage.info('请先输入一些正文内容，再使用 AI 生成标题')
    return
  }
  aiTitleLoading.value = true
  try {
    const res = await generateTitle(bodyText)
    const t = res?.title?.trim()
    if (t) {
      title.value = t.length > 100 ? t.slice(0, 100) : t
      aiTitleUsage.value += 1
      ElMessage.success('标题已生成')
    } else {
      ElMessage.warning('未能生成标题，请重试')
    }
  } finally {
    aiTitleLoading.value = false
  }
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

const otherTagNamesForShow = computed(() => {
  if (aiGeneratedTagNames.value.length > 0) return aiGeneratedTagNames.value.slice(0, 5)
  return selectedOtherIds.value
    .map((id) => otherTagList.value.find((t) => t.id === id)?.name)
    .filter(Boolean) as string[]
})

function onOtherTagSelectChange() {
  aiGeneratedTagNames.value = []
  if (selectedOtherIds.value.length > 4) selectedOtherIds.value = selectedOtherIds.value.slice(0, 4)
}

function onRemoveOtherTag(idx: number) {
  if (aiGeneratedTagNames.value.length > 0) {
    aiGeneratedTagNames.value = aiGeneratedTagNames.value.filter((_, i) => i !== idx)
  } else {
    const id = selectedOtherIds.value[idx]
    if (id != null) selectedOtherIds.value = selectedOtherIds.value.filter((_, i) => i !== idx)
  }
}

async function onAiGenerateTags() {
  const bodyText = getMarkdownValue().trim().slice(0, 2000)
  if (!bodyText) {
    ElMessage.info('请先输入正文内容')
    return
  }
  aiTagsLoading.value = true
  selectedOtherIds.value = []
  try {
    const res = await generateTags(bodyText)
    const names = res?.tagNames
    if (Array.isArray(names) && names.length > 0) {
      aiGeneratedTagNames.value = names.slice(0, 5)
      ElMessage.success('已生成 ' + aiGeneratedTagNames.value.length + ' 个标签')
    } else {
      aiGeneratedTagNames.value = []
      ElMessage.warning('未能生成标签，请重试')
    }
  } finally {
    aiTagsLoading.value = false
  }
}

function setOneClickStepStatus(key: string, status: 'pending' | 'loading' | 'done') {
  const step = oneClickSteps.value.find((s) => s.key === key)
  if (step) step.status = status
}

async function onOneClickGenerate() {
  const botId = oneClickBotId.value
  const prompt = oneClickPrompt.value.trim()
  if (!botId || !prompt) return
  oneClickGenerating.value = true
  oneClickSteps.value = [
    { key: 'body', label: '正文', status: 'pending' },
    { key: 'title', label: '标题', status: 'pending' },
    { key: 'summary', label: '摘要', status: 'pending' },
    { key: 'tagNames', label: '小标签', status: 'pending' },
    { key: 'cover', label: '封面', status: 'pending' },
    { key: 'mainTag', label: '主标签', status: 'pending' },
  ]
  const delay = (ms: number) => new Promise((r) => setTimeout(r, ms))
  try {
    setOneClickStepStatus('body', 'loading')
    const res = await oneClickGenerate(botId, prompt)
    if (res.body && vditor) {
      vditor.setValue(res.body, true)
      updateTocFromMarkdown(res.body)
    }
    setOneClickStepStatus('body', 'done')
    await delay(120)

    setOneClickStepStatus('title', 'loading')
    await delay(80)
    if (res.title) title.value = res.title.length > 100 ? res.title.slice(0, 100) : res.title
    setOneClickStepStatus('title', 'done')
    await delay(120)

    setOneClickStepStatus('summary', 'loading')
    await delay(80)
    if (res.summary) summary.value = res.summary.length > 256 ? res.summary.slice(0, 256) : res.summary
    setOneClickStepStatus('summary', 'done')
    await delay(120)

    setOneClickStepStatus('tagNames', 'loading')
    await delay(80)
    if (Array.isArray(res.tagNames) && res.tagNames.length > 0) {
      selectedOtherIds.value = []
      aiGeneratedTagNames.value = res.tagNames.slice(0, 5)
    }
    setOneClickStepStatus('tagNames', 'done')
    await delay(120)

    setOneClickStepStatus('cover', 'loading')
    await delay(80)
    if (res.coverUrl) cover.value = res.coverUrl
    setOneClickStepStatus('cover', 'done')
    await delay(120)

    setOneClickStepStatus('mainTag', 'loading')
    await delay(80)
    if (res.mainTagId != null) mainTagId.value = res.mainTagId
    setOneClickStepStatus('mainTag', 'done')

    ElMessage.success('一键生成完成')
  } catch (e) {
    ElMessage.error('生成过程中出错，请重试')
    throw e
  } finally {
    oneClickGenerating.value = false
  }
}

/** 根据文章 id 拉取详情并填充表单，返回正文（供编辑器使用）。失败时返回空字符串。 */
async function fetchAndApplyArticle(id: number): Promise<string> {
  try {
    const data = await getContentForEdit(id)
    contentId.value = data.id
    title.value = data.title ?? ''
    summary.value = data.summary ?? ''
    cover.value = data.cover ?? ''
    columnId.value = data.columnId
    articleType.value = (data.articleType === 'REPRINT' ? 'reprint' : data.articleType === 'TRANSLATED' ? 'translated' : 'original') as 'original' | 'reprint' | 'translated'
    visibility.value = (data.visibility === 'SELF' ? 'self' : data.visibility === 'FANS' ? 'fans' : 'all') as 'all' | 'self' | 'fans'
    creationStatement.value = (data.creationStatement ?? 'none') as 'none' | 'ai-assisted' | 'network' | 'personal'
    const names = data.tagNames ?? []
    if (names.length > 0 && mainTagList.value.length > 0) {
      const mainTag = mainTagList.value.find((t) => t.name === names[0])
      if (mainTag) mainTagId.value = mainTag.id
    }
    if (names.length > 1) {
      selectedOtherIds.value = []
      aiGeneratedTagNames.value = names.slice(1).slice(0, 5)
    }
    return data.body ?? ''
  } catch {
    ElMessage.warning('加载文章失败，将使用空白编辑')
    return ''
  }
}

onMounted(async () => {
  await Promise.all([
    getColumnsMe().then((list) => { columnList.value = list ?? [] }),
    getMainTags().then((list) => { mainTagList.value = list ?? [] }),
    getOtherTags().then((list) => { otherTagList.value = list ?? [] }),
    getBlogBotsMe().then((list) => { botListForWrite.value = list ?? [] }),
  ])
  let initialBody = ''
  const idParam = route.query.id
  const columnIdQuery = route.query.columnId
  if (!idParam && columnIdQuery != null) {
    const cid = typeof columnIdQuery === 'string' ? parseInt(columnIdQuery, 10) : Number(columnIdQuery)
    if (!Number.isNaN(cid)) columnId.value = cid
  }
  if (idParam) {
    const id = typeof idParam === 'string' ? parseInt(idParam, 10) : Number(idParam)
    if (!Number.isNaN(id)) {
      initialBody = await fetchAndApplyArticle(id)
    }
  }
  if (!vditorRef.value) return
  vditor = new Vditor(vditorRef.value, {
    cdn: VDITOR_CDN,
    height: 420,
    value: initialBody,
    placeholder: '#创作灵感#\n记录工作实践、项目复盘\n写技术笔记巩固知识要点\n发表职场感悟心得',
    lang: 'zh_CN',
    mode: 'wysiwyg',
    theme: 'classic',
    cache: { enable: false },
    toolbarConfig: { hide: true },
    customWysiwygToolbar: () => [],
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

// 从列表点「编辑」进入时路由变为 /creator/write?id=xxx，组件会被复用，onMounted 不会再次执行，需监听 query.id 拉取并填充
watch(
  () => route.query.id,
  async (newId) => {
    const idParam = Array.isArray(newId) ? newId[0] : newId
    if (idParam == null || idParam === '') return
    const id = typeof idParam === 'string' ? parseInt(idParam, 10) : Number(idParam)
    if (Number.isNaN(id)) return
    const body = await fetchAndApplyArticle(id)
    if (vditor) {
      vditor.setValue(body, true)
      updateTocFromMarkdown(body)
    }
  }
)

onBeforeUnmount(() => {
  vditor?.destroy()
  vditor = null
})

function getMarkdownValue(): string {
  return vditor?.getValue() ?? ''
}

const articleTypeMap = { original: 'ORIGINAL', reprint: 'REPRINT', translated: 'TRANSLATED' } as const
const visibilityMap = { all: 'ALL', self: 'SELF', fans: 'FANS' } as const

function buildTagNames(): string[] | undefined {
  const main = mainTagId.value != null ? mainTagList.value.find((t) => t.id === mainTagId.value)?.name : undefined
  const others = otherTagNamesForShow.value
  const list = [main, ...others].filter(Boolean) as string[]
  return list.length > 0 ? list.slice(0, 5) : undefined
}

function onSaveDraft() {
  const body = getMarkdownValue().trim()
  if (!body) {
    ElMessage.warning('请先输入内容')
    return
  }
  if (mainTagId.value == null) {
    ElMessage.warning('请选择主标签')
    return
  }
  draftSaving.value = true
  saveDraft({
    id: contentId.value,
    title: title.value.trim() || undefined,
    body,
    summary: summary.value?.trim() || undefined,
    cover: cover.value?.trim() || undefined,
    columnId: columnId.value,
    articleType: articleTypeMap[articleType.value],
    creationStatement: creationStatement.value || undefined,
    visibility: visibilityMap[visibility.value],
    tagNames: buildTagNames(),
  })
    .then((res) => {
      if (res?.id != null) contentId.value = res.id
      ElMessage.success('草稿已保存')
    })
    .finally(() => {
      draftSaving.value = false
    })
}

function onPublish() {
  const body = getMarkdownValue().trim()
  if (!body) {
    ElMessage.warning('请先输入内容')
    return
  }
  if (mainTagId.value == null) {
    ElMessage.warning('请选择主标签')
    return
  }
  if (publishLoading.value) return
  publishLoading.value = true
  const draftBody = {
    id: contentId.value,
    title: title.value.trim() || undefined,
    body,
    summary: summary.value?.trim() || undefined,
    cover: cover.value?.trim() || undefined,
    columnId: columnId.value,
    articleType: articleTypeMap[articleType.value],
    creationStatement: creationStatement.value || undefined,
    visibility: visibilityMap[visibility.value],
    tagNames: buildTagNames(),
  }
  saveDraft(draftBody)
    .then((res) => {
      const id = res?.id
      if (id == null) return
      return publishContent(id)
    })
    .then((res) => {
      if (res?.id != null) {
        ElMessage.success('发布成功')
        router.push('/creator')
      }
    })
    .catch(() => {
      ElMessage.error('发布失败，请重试')
    })
    .finally(() => {
      publishLoading.value = false
    })
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

/** 检测当前选区/光标是否处于加粗、斜体或删除线；兼容 WYSIWYG 与 IR(data-type) */
function getSelectionInlineFormat(): { bold: boolean; italic: boolean; strike: boolean } {
  const v = vditor?.vditor
  if (!v) return { bold: false, italic: false, strike: false }
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return { bold: false, italic: false, strike: false }
  const range = sel.getRangeAt(0)
  const modeEl = v[v.currentMode]
  const editorEl = modeEl?.element
  if (!editorEl?.contains(range.startContainer)) return { bold: false, italic: false, strike: false }
  let node: Node | null = range.startContainer
  if (node.nodeType === Node.TEXT_NODE) node = (node as Text).parentElement
  const el = node as HTMLElement | null
  if (!el?.closest) return { bold: false, italic: false, strike: false }
  return {
    bold: !!(el.closest('strong') || el.closest('b') || el.closest('[data-type="strong"]')),
    italic: !!(el.closest('em') || el.closest('i') || el.closest('[data-type="em"]')),
    strike: !!(el.closest('s') || el.closest('strike') || el.closest('[data-type="s"]')),
  }
}

function onUndo() {
  // Vditor 内部有 undo 栈，通过实例的 vditor.undo.undo 调用
  if (!vditor?.vditor?.undo) return
  vditor.vditor.undo.undo(vditor.vditor)
}

function onRedo() {
  if (!vditor?.vditor?.undo) return
  vditor.vditor.undo.redo(vditor.vditor)
}

function onBold() {
  if (!vditor) return
  const sel = vditor.getSelection() || '加粗文本'
  const hasRealSelection = !!vditor.getSelection()
  const fmt = hasRealSelection ? getSelectionInlineFormat() : { bold: false, italic: false, strike: false }
  // 已是加粗则取消加粗（保留斜体或变回纯文本）
  if (fmt.bold) {
    insertMD(fmt.italic ? `*${sel}*` : sel)
    return
  }
  insertMD(fmt.italic ? `***${sel}***` : `**${sel}**`)
}

function onItalic() {
  if (!vditor) return
  const sel = vditor.getSelection() || '斜体文本'
  const hasRealSelection = !!vditor.getSelection()
  const fmt = hasRealSelection ? getSelectionInlineFormat() : { bold: false, italic: false, strike: false }
  // 已是斜体则取消斜体（保留加粗或变回纯文本）
  if (fmt.italic) {
    insertMD(fmt.bold ? `**${sel}**` : sel)
    return
  }
  insertMD(fmt.bold ? `***${sel}***` : `*${sel}*`)
}

function onStrikethrough() {
  if (!vditor) return
  const sel = vditor.getSelection() || '删除线文本'
  const hasRealSelection = !!vditor.getSelection()
  const fmt = hasRealSelection ? getSelectionInlineFormat() : { bold: false, italic: false, strike: false }
  // 已是删除线则取消删除线（Markdown 使用 ~~文本~~）
  if (fmt.strike) {
    insertMD(sel)
    return
  }
  insertMD(`~~${sel}~~`)
}

/** 触发 Vditor 内置工具栏按钮，返回是否触发成功 */
function triggerVditorToolbar(name: 'list' | 'ordered-list' | 'check' | 'insert-before' | 'insert-after' | 'table' | 'inline-code' | 'quote' | 'line'): boolean {
  const btn = vditor?.vditor?.toolbar?.elements?.[name]?.firstElementChild as HTMLElement | undefined
  if (btn) {
    btn.click()
    return true
  }
  return false
}

function onBulletList() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('list')
  if (!triggered) insertMD('\n- 列表项\n')
}

function onOrderedList() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('ordered-list')
  if (!triggered) insertMD('\n1. 列表项 1\n2. 列表项 2\n')
}

function onTaskList() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('check')
  if (!triggered) insertMD('\n- [ ] 待办事项\n')
}

function onInsertBefore() {
  if (!vditor) return
  triggerVditorToolbar('insert-before')
}

function onInsertAfter() {
  if (!vditor) return
  triggerVditorToolbar('insert-after')
}

function onCode() {
  insertMD('\n```lang\n代码块\n```\n')
}

function onInlineCode() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('inline-code')
  if (!triggered) {
    const sel = vditor.getSelection() || '行内代码'
    insertMD(`\`${sel}\``)
  }
}

function onTable() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('table')
  if (!triggered) {
    insertMD('\n| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n|  |  |  |\n')
  }
}

function onQuote() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('quote')
  if (!triggered) {
    const sel = vditor.getSelection()
    insertMD(sel ? `\n> ${sel.split(/\r?\n/).join('\n> ')}\n` : '\n> 引用内容\n')
  }
}

function onHorizontalRule() {
  if (!vditor) return
  const triggered = triggerVditorToolbar('line')
  if (!triggered) insertMD('\n---\n')
}

function triggerImageSelect() {
  if (imageUploading.value) return
  imageInputRef.value?.click()
}

async function onImageFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!vditor) return
  imageUploading.value = true
  try {
    const res = await uploadImage(file, 'images')
    const url = (res.url || '').replace(/\s/g, '%20')
    insertMD(`\n![image](${url})\n`)
    ElMessage.success('图片已插入')
  } finally {
    imageUploading.value = false
  }
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

.hidden-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
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

.tool-icon-strike {
  text-decoration: line-through;
}

.tool-icon-hr {
  font-weight: 600;
  letter-spacing: 0.02em;
}

.tool-label {
  font-size: 12px;
}

.tool-btn:hover {
  color: #111 !important;
}

.tool-btn .el-icon {
  font-size: 18px;
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
  /* 左侧预留目录；右侧预留 AI 助手（折叠 48px / 展开 300px，由 .ai-panel-expanded 控制） */
  margin: 0 48px 0 260px;
  transition: margin-right 0.2s ease;
}

.write-body.ai-panel-expanded .editor-main {
  margin-right: 300px;
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

/* 标题区：与正文用一条线分开（仿 CSDN / A4 纸） */
.title-section {
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 16px;
  margin-bottom: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.title-row .title-input {
  flex: 1;
  margin-bottom: 0;
  min-width: 0;
}

.ai-title-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: none;
  background: #f0f0f0;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}

.ai-title-btn:hover:not(:disabled) {
  background: #e5e5e5;
  color: #111;
}

.ai-title-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-title-btn-icon {
  font-size: 18px;
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
  margin-bottom: 0;
}

.vditor-wrap {
  margin-top: 0;
}

/* 隐藏 Vditor 自带工具栏，使用顶栏自定义工具栏 */
.editor-paper :deep(.vditor-toolbar) {
  display: none !important;
}

/* 去掉 Vditor 编辑框外框，使编辑区像一张 A4 纸 */
.editor-paper :deep(.vditor),
.editor-paper :deep(.vditor-content) {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  background: transparent !important;
}

.editor-paper :deep(.vditor-content) {
  background: #fff !important;
}

/* 编辑区选区样式：柔和灰底深字，替代浏览器默认蓝反色 */
.editor-paper ::selection {
  background: #e0e7eb;
  color: #111;
}

/* 发文设置区域（编辑区下方，不含 GitCode 备份） */
.publish-settings {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
}

.setting-row {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.setting-label {
  font-size: 14px;
  color: #333;
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.setting-label .required {
  color: #f56c6c;
}

.setting-help {
  color: #999;
  font-size: 14px;
  cursor: help;
}

.cover-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.cover-left {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cover-ai-btn {
  align-self: flex-start;
}

.cover-upload {
  width: 160px;
  height: 100px;
  border: 1px dashed #d0d0d0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.cover-upload:hover {
  border-color: #409eff;
  color: #409eff;
}

.cover-upload .cover-plus {
  font-size: 28px;
}

.cover-placeholder {
  flex: 1;
  min-height: 100px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
  padding: 0 16px;
  text-align: center;
  position: relative;
}

.cover-placeholder .cover-preview-img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 6px;
}

.cover-remove-btn {
  padding: 4px 12px;
  font-size: 12px;
  color: #666;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.cover-remove-btn:hover {
  color: #409eff;
  border-color: #409eff;
}

.cover-upload.uploading {
  pointer-events: none;
  opacity: 0.8;
}

.cover-upload .cover-plus.is-loading {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.summary-desc {
  font-size: 12px;
  color: #999;
  margin: -8px 0 10px 0;
  line-height: 1.5;
}

.summary-wrap {
  margin-bottom: 20px;
}

.summary-wrap .summary-input {
  margin-bottom: 10px;
}

.summary-wrap .summary-input :deep(.el-textarea__inner) {
  font-size: 13px;
}

.ai-summary-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ai-summary-icon {
  font-size: 14px;
}

.setting-select,
.column-select,
.tag-main-select {
  min-width: 200px;
}

.tag-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.tag-custom {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-custom-label {
  font-size: 13px;
  color: #606266;
}

.tag-custom-input {
  width: 220px;
}

.tag-other-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-other-select {
  min-width: 260px;
}

.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.article-type-group,
.visibility-group {
  flex-wrap: wrap;
}

/* AI 助手卡片：BBC 风格，折叠时不挡页面（仅留窄条） */
.ai-sidebar {
  position: fixed;
  top: 112px;
  right: 0;
  width: 300px;
  max-width: calc(100vw - 24px);
  padding: 0;
  background: transparent;
  transition: width 0.2s ease, right 0.2s ease;
  z-index: 30;
}

.ai-sidebar-collapsed {
  width: 48px;
}

.ai-sidebar-collapsed .ai-card {
  border-radius: 8px 0 0 8px;
  padding: 10px 8px;
}

.ai-sidebar-collapsed .ai-header {
  flex-direction: column;
  gap: 6px;
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.ai-sidebar-collapsed .ai-title-text,
.ai-sidebar-collapsed .ai-body {
  display: none;
}

.ai-sidebar-collapsed .ai-title-arrow {
  margin-left: 0;
  transform: rotate(180deg);
}

.ai-card {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-right: none;
  border-radius: 10px 0 0 10px;
  padding: 14px 16px;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.04);
  min-height: 120px;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 2px 0;
  margin-bottom: 12px;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 10px;
}

.ai-logo {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
  flex-shrink: 0;
}

.ai-title-text {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.01em;
}

.ai-title-arrow {
  margin-left: auto;
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.ai-body {
  margin-top: 0;
}

.ai-oneclick {
  margin-top: 0;
  padding-top: 0;
}
.ai-oneclick-title {
  font-size: 12px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 10px;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.ai-oneclick-select {
  width: 100%;
  margin-bottom: 8px;
}
.ai-oneclick-select :deep(.el-input__wrapper) {
  border-radius: 4px;
  border: 1px solid #b3b3b3;
  background: #fff;
  box-shadow: none;
}
.ai-oneclick-prompt {
  margin-bottom: 10px;
}
.ai-oneclick-prompt :deep(.el-textarea__inner) {
  border-radius: 4px;
  border: 1px solid #b3b3b3;
  font-size: 12px;
  min-height: 64px;
  background: #fff;
}
.ai-oneclick-btn {
  width: 100%;
  margin-bottom: 12px;
  border-radius: 4px;
  background: #1a1a1a;
  border-color: #1a1a1a;
}
.ai-oneclick-btn:hover {
  background: #333;
  border-color: #333;
}
.ai-oneclick-steps {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ai-step {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #4d4d4d;
}
.ai-step.loading .ai-step-label {
  color: #1a1a1a;
}
.ai-step.done .ai-step-label {
  color: #1a1a1a;
  font-weight: 500;
}
.ai-step-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.ai-step-icon .el-icon.is-loading {
  font-size: 14px;
  color: #1a1a1a;
}
.ai-step-done {
  font-size: 14px;
  color: #1a1a1a;
}
.ai-step-pending {
  font-size: 12px;
  color: #b3b3b3;
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
