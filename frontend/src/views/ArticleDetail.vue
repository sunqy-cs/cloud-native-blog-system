<template>
  <div class="article-detail article-detail--bbc">
    <el-button class="back" text @click="router.push('/')">← 返回首页</el-button>
    <el-card v-if="article" shadow="never" class="article-card">
      <!-- 标题 -->
      <h1 class="article-title">{{ article.title }}</h1>

      <!-- 简介 -->
      <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>

      <!-- 标签 -->
      <div v-if="article.tagNames?.length" class="article-tags">
        <el-tag v-for="t in article.tagNames" :key="t" size="small" type="info">{{ t }}</el-tag>
      </div>

      <!-- 文章类型、创作声明：引用块样式，增强层次感 -->
      <blockquote v-if="articleMetaText" class="article-meta-quote">
        <p v-if="articleTypeLabel" class="meta-row"><span class="meta-label">类型</span><span class="meta-value">{{ articleTypeLabel }}</span></p>
        <p v-if="showCreationStatement" class="meta-row"><span class="meta-label">创作声明</span><span class="meta-value">{{ creationStatementText }}</span></p>
      </blockquote>

      <!-- 作者区 -->
      <div class="author-block">
        <template v-if="authorInfo">
          <el-avatar :src="authorInfo.avatar" :size="44">
            {{ (authorInfo.nickname || authorInfo.username || '用').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="author-info">
            <span class="author-name">{{ authorInfo.nickname || authorInfo.username || '用户' }}</span>
            <p v-if="authorInfo.intro || authorInfo.bio" class="author-intro">{{ authorInfo.intro || authorInfo.bio }}</p>
          </div>
        </template>
        <template v-else>
          <el-avatar :size="44">作</el-avatar>
          <div class="author-info">
            <span class="author-name">作者</span>
          </div>
        </template>
      </div>

      <!-- 阅读数、点赞数、评论数 -->
      <div class="article-stats">
        <span>阅读 {{ article.viewCount }}</span>
        <span>点赞 {{ article.likeCount }}</span>
        <span>评论 {{ article.commentCount }}</span>
        <span class="article-date">{{ formatDate(article.createdAt) }}</span>
      </div>

      <!-- 正文 Markdown -->
      <div ref="previewRef" class="article-body vditor-reset"></div>

      <!-- 底部操作：点赞、收藏、评论（暂不实装） -->
      <div class="article-actions">
        <el-button class="action-btn" :icon="Star">点赞 {{ article.likeCount }}</el-button>
        <el-button class="action-btn" :icon="Collection">收藏</el-button>
        <el-button class="action-btn" :icon="ChatDotRound">评论 {{ article.commentCount }}</el-button>
      </div>

      <!-- 评论区 -->
      <section class="comment-section">
        <div class="comment-input-row">
          <el-avatar :size="40" class="comment-input-avatar" :src="commentAvatarSrc">
            {{ commentUserInitial }}
          </el-avatar>
          <div class="comment-input-wrap">
            <el-input
              v-model="commentInput"
              type="textarea"
              :rows="3"
              placeholder="理性发言, 友善互动"
              class="comment-input"
              maxlength="500"
              show-word-limit
            />
            <div class="comment-input-toolbar">
              <el-popover
                v-model:visible="emojiPickerVisible"
                placement="top-start"
                :width="360"
                trigger="manual"
              >
                <template #reference>
                  <button type="button" class="comment-emoji-btn" title="插入表情" @click="emojiPickerVisible = !emojiPickerVisible">
                    <span class="comment-emoji-icon">😀</span> 表情
                  </button>
                </template>
                <EmojiPicker native :static-texts="{ placeholder: '搜索表情' }" @select="onSelectEmoji" />
              </el-popover>
              <el-button
                type="primary"
                class="comment-submit-btn"
                :loading="commentSubmitting"
                @click="submitComment"
              >
                发表评论
              </el-button>
            </div>
          </div>
        </div>
        <div class="comment-header">
          <h3 class="comment-count">{{ displayCommentCount }}条评论</h3>
          <div class="comment-sort">
            <button
              type="button"
              class="comment-sort-btn"
              :class="{ active: commentSort === 'default' }"
              @click="commentSort = 'default'"
            >
              默认
            </button>
            <button
              type="button"
              class="comment-sort-btn"
              :class="{ active: commentSort === 'latest' }"
              @click="commentSort = 'latest'"
            >
              最新
            </button>
          </div>
        </div>
        <div v-if="commentLoading" class="comment-loading">加载评论中…</div>
        <div v-else-if="displayComments.length === 0" class="comment-empty">暂无评论，快来抢沙发～</div>
        <div v-else class="comment-list">
          <template v-for="c in displayComments" :key="c.id">
            <article class="comment-item" :class="{ 'comment-item--reply': c.parentId }">
              <el-avatar :size="36" class="comment-avatar" :src="c.userAvatar">
                {{ (c.userNickname || '用').charAt(0) }}
              </el-avatar>
              <div class="comment-main">
                <div class="comment-head">
                  <span class="comment-username">{{ c.userNickname || '用户' }}</span>
                  <el-tag v-if="c.isAuthor" type="danger" size="small" class="comment-tag-author">作者</el-tag>
                  <span class="comment-meta">{{ timeAgo(c.createdAt) }}</span>
                  <el-icon class="comment-more" title="更多"><MoreFilled /></el-icon>
                </div>
                <div class="comment-body">{{ c.body }}</div>
                <div class="comment-actions">
                  <button type="button" class="comment-action">
                    <el-icon><ChatDotRound /></el-icon> 回复
                  </button>
                  <button type="button" class="comment-action">
                    <el-icon><Star /></el-icon> 喜欢
                  </button>
                </div>
              </div>
            </article>
          </template>
        </div>
      </section>
    </el-card>
    <el-empty v-else-if="!loading" description="文章不存在或加载失败" />
    <div v-else class="loading-wrap"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentView, type ContentView } from '@/api/content'
import { getMe, type UserMe } from '@/api/user'
import { useUserStore } from '@/stores/user'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { Loading, Star, Collection, ChatDotRound, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getContentComments, createComment, type CommentItem } from '@/api/comment'
import EmojiPicker from 'vue3-emoji-picker'
import 'vue3-emoji-picker/css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref<ContentView | null>(null)
const loading = ref(true)
const authorInfo = ref<UserMe | null>(null)
const previewRef = ref<HTMLDivElement | null>(null)

const commentList = ref<CommentItem[]>([])
const commentLoading = ref(false)
const commentInput = ref('')
const commentSort = ref<'default' | 'latest'>('default')
const emojiPickerVisible = ref(false)
const commentSubmitting = ref(false)

const id = computed(() => {
  const p = route.params.id
  return typeof p === 'string' ? p : Array.isArray(p) ? p[0] : String(p)
})

// 文章类型：ORIGINAL→原创 / REPRINT→转载 / TRANSLATED→翻译，不显示英文枚举
const articleTypeLabel = computed(() => {
  const t = article.value?.articleType
  const map: Record<string, string> = {
    ORIGINAL: '原创',
    REPRINT: '转载',
    TRANSLATED: '翻译',
    original: '原创',
    reprint: '转载',
    translated: '翻译',
  }
  return t ? (map[t] ?? map[t.toUpperCase?.()] ?? t) : ''
})

// 创作声明：没写不显示（空、none、纯空格都不显示）
const showCreationStatement = computed(() => {
  const s = article.value?.creationStatement
  if (s == null || s === '') return false
  const t = String(s).trim()
  return t !== '' && t.toLowerCase() !== 'none'
})
const creationStatementText = computed(() => (article.value?.creationStatement ?? '').trim())

const articleMetaText = computed(() => !!articleTypeLabel.value || showCreationStatement.value)

/** 评论区当前用户头像：URL 与首字（无头像时显示首字） */
const commentAvatarSrc = computed(() => (userStore.userInfo as { avatar?: string } | null)?.avatar)
const commentUserInitial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '我').charAt(0))

/** 展示的评论条数（与接口同步，无则用文章 commentCount） */
const displayCommentCount = computed(() => {
  if (commentList.value.length > 0) return commentList.value.length
  return article.value?.commentCount ?? 0
})

/** 排序后的评论列表（先按时间排序，再展平：顶级在前，回复紧跟其下并缩进） */
const displayComments = computed(() => {
  const list = [...commentList.value]
  const order = commentSort.value === 'latest' ? -1 : 1
  list.sort((a, b) => {
    const t1 = new Date(a.createdAt).getTime()
    const t2 = new Date(b.createdAt).getTime()
    return (t1 - t2) * order
  })
  const top = list.filter((c) => !c.parentId)
  const byParent = new Map<number, CommentItem[]>()
  list.filter((c) => c.parentId).forEach((c) => {
    const pid = c.parentId!
    if (!byParent.has(pid)) byParent.set(pid, [])
    byParent.get(pid)!.push(c)
  })
  const result: CommentItem[] = []
  top.forEach((c) => {
    result.push(c)
    byParent.get(c.id)?.forEach((r) => result.push(r))
  })
  return result
})

function timeAgo(iso: string) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = Date.now()
  const diff = Math.floor((now - d.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

async function loadComments() {
  const aid = article.value?.id
  if (aid == null) return
  commentLoading.value = true
  try {
    commentList.value = await getContentComments(aid)
  } catch {
    commentList.value = []
  } finally {
    commentLoading.value = false
  }
}

function onSelectEmoji(emoji: { i: string }) {
  commentInput.value += emoji.i
  emojiPickerVisible.value = false
}

async function submitComment() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再发表评论')
    return
  }
  const body = commentInput.value.trim()
  if (!body) {
    ElMessage.warning('请输入评论内容')
    return
  }
  const contentId = article.value?.id
  if (contentId == null) return
  commentSubmitting.value = true
  try {
    await createComment({ contentId, body })
    commentInput.value = ''
    ElMessage.success('评论发表成功')
    await loadComments()
    if (article.value) article.value.commentCount = (article.value.commentCount ?? 0) + 1
  } catch {
    // 错误信息已由 request 拦截器统一提示
  } finally {
    commentSubmitting.value = false
  }
}

function formatDate(iso?: string) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
  } catch {
    return iso
  }
}

async function loadArticle() {
  const numId = Number(id.value)
  if (!numId || Number.isNaN(numId)) {
    article.value = null
    loading.value = false
    return
  }
  loading.value = true
  article.value = null
  authorInfo.value = null
  try {
    const data = await getContentView(numId)
    article.value = data
    if (data.userId && userStore.userInfo?.id === data.userId) {
      try {
        authorInfo.value = await getMe()
      } catch {
        authorInfo.value = null
      }
    }
    loadComments()
  } catch {
    article.value = null
  } finally {
    loading.value = false
  }
}

async function renderMarkdown() {
  const a = article.value
  const el = previewRef.value
  if (!a?.body || !el) return
  el.innerHTML = ''
  try {
    await Vditor.preview(el, a.body, { mode: 'light', lang: 'zh_CN' })
  } catch (e) {
    el.textContent = a.body || '暂无正文'
    console.warn('Vditor.preview error', e)
  }
}

onMounted(() => loadArticle())

watch(
  () => article.value?.body,
  () => renderMarkdown(),
  { flush: 'post' }
)
watch(
  () => id.value,
  () => loadArticle()
)
</script>

<style scoped>
/* BBC 风格：红色点缀、编辑感 */
.article-detail {
  padding: 1rem 2rem;
  max-width: 960px;
  margin: 0 auto;
}
.article-detail--bbc .back {
  margin-bottom: 1rem;
  color: #666;
  font-size: 0.95rem;
}
.article-detail--bbc .back:hover {
  color: #bb1919;
}
.article-card {
  padding: 1.5rem 2rem;
}
.article-title {
  margin: 0 0 0.75rem;
  padding-bottom: 0.75rem;
  font-size: 1.85rem;
  font-weight: 700;
  line-height: 1.35;
  border-bottom: 2px solid #bb1919;
  color: #1a1a1a;
}
.article-summary {
  color: #555;
  margin: 0 0 1rem;
  line-height: 1.65;
  font-size: 1rem;
}
.article-tags {
  margin-bottom: 1rem;
}
.article-tags .el-tag {
  margin-right: 0.5rem;
  margin-bottom: 0.25rem;
  border-color: #bb1919;
  color: #bb1919;
  background: rgba(187, 25, 25, 0.06);
}
/* 类型/创作声明：类似 Markdown 引用块，左侧竖线 + 浅底，层次更清晰 */
.article-meta-quote {
  margin: 0 0 1.25rem;
  padding: 0.75rem 1rem 0.75rem 1rem;
  border-left: 4px solid #bb1919;
  background: #f8f8f8;
  border-radius: 0 6px 6px 0;
  color: #444;
  font-size: 0.9rem;
  line-height: 1.6;
}
.article-meta-quote .meta-row {
  margin: 0;
}
.article-meta-quote .meta-row + .meta-row {
  margin-top: 0.35rem;
}
.article-meta-quote .meta-label {
  color: #bb1919;
  font-weight: 600;
  margin-right: 0.5rem;
}
.article-meta-quote .meta-value {
  color: #333;
}
.author-block {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 1rem 0;
  border-top: 1px solid #e5e5e5;
  border-bottom: 1px solid #e5e5e5;
  margin-bottom: 1rem;
}
.author-info {
  flex: 1;
  min-width: 0;
}
.author-name {
  font-weight: 600;
  display: block;
  margin-bottom: 0.25rem;
  color: #1a1a1a;
}
.author-intro {
  margin: 0;
  font-size: 0.9rem;
  color: #555;
  line-height: 1.5;
}
.article-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 1rem;
  color: #555;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}
.article-stats span:not(.article-date) {
  color: #333;
}
.article-stats .article-date {
  margin-left: auto;
  color: #888;
}
.article-body {
  line-height: 1.8;
  margin-top: 0.5rem;
}
.article-body :deep(.vditor-reset) {
  color: #333;
}
.article-actions {
  margin-top: 2rem;
  padding-top: 1.25rem;
  border-top: 1px solid #e5e5e5;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.article-actions .action-btn {
  color: #555;
  border-color: #ddd;
}
.article-actions .action-btn:hover {
  color: #bb1919;
  border-color: #bb1919;
}

/* 评论区 */
.comment-section {
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e5e5;
}
.comment-input-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}
.comment-input-avatar {
  flex-shrink: 0;
}
.comment-input-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.comment-input {
  flex: 1;
}
.comment-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: vertical;
}
.comment-input-toolbar {
  display: flex;
  align-items: center;
}
.comment-emoji-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 13px;
  color: #666;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.comment-emoji-btn:hover {
  color: #bb1919;
  background: rgba(187, 25, 25, 0.06);
}
.comment-emoji-icon {
  font-size: 16px;
  line-height: 1;
}
.comment-submit-btn {
  margin-left: auto;
}
.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.comment-count {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a1a;
}
.comment-sort {
  display: flex;
  gap: 0;
}
.comment-sort-btn {
  padding: 4px 12px;
  font-size: 13px;
  color: #666;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.comment-sort-btn:hover {
  color: #bb1919;
}
.comment-sort-btn.active {
  color: #bb1919;
  font-weight: 500;
}
.comment-loading,
.comment-empty {
  padding: 1.5rem;
  text-align: center;
  color: #888;
  font-size: 14px;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 1rem 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-item--reply {
  margin-left: 48px;
  padding-left: 12px;
  border-left: 2px solid #eee;
}
.comment-avatar {
  flex-shrink: 0;
}
.comment-main {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}
.comment-username {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 14px;
}
.comment-tag-author {
  font-size: 12px;
  padding: 0 6px;
  line-height: 18px;
}
.comment-meta {
  font-size: 13px;
  color: #999;
  margin-right: auto;
}
.comment-more {
  font-size: 16px;
  color: #999;
  cursor: pointer;
}
.comment-more:hover {
  color: #666;
}
.comment-body {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-actions {
  display: flex;
  gap: 16px;
}
.comment-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  font-size: 13px;
  color: #999;
  background: none;
  border: none;
  cursor: pointer;
}
.comment-action:hover {
  color: #bb1919;
}

.loading-wrap {
  padding: 2rem;
  text-align: center;
  color: #555;
}
</style>
