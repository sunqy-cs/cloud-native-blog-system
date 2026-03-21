<template>
  <div class="audit-layout">
    <header class="audit-header">
      <div class="header-left">
        <router-link to="/recommend" class="logo-link">
          <img src="/logo3.png" alt="Logo" class="audit-logo" />
        </router-link>
        <span class="audit-title-bar">审核中心</span>
      </div>
      <div class="header-right">
        <span class="audit-avatar" title="当前管理员">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-img" />
          <span v-else class="avatar-initial">{{ avatarInitial }}</span>
        </span>
      </div>
    </header>

    <div class="audit-body">
      <aside class="audit-sidebar-left">
        <router-link to="/recommend" custom v-slot="{ navigate }">
          <el-button class="back-site-btn back-site-btn-full" type="primary" @click="navigate">
            <el-icon><HomeFilled /></el-icon>
            返回站点
          </el-button>
        </router-link>
        <nav class="audit-nav">
          <router-link to="/audit" :class="['nav-item', { active: route.path === '/audit' }]">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </router-link>
          <div class="nav-group">
            <button type="button" class="nav-item nav-group-title" @click="contentAuditOpen = !contentAuditOpen">
              <el-icon><Folder /></el-icon>
              <span>内容审核</span>
              <el-icon class="nav-chevron" :class="{ open: contentAuditOpen }"><ArrowDown /></el-icon>
            </button>
            <div v-show="contentAuditOpen" class="nav-sub">
              <router-link
                to="/audit/articles"
                :class="['nav-item nav-sub-item', { active: route.path === '/audit/articles' }]"
              >
                <span>待审博客</span>
              </router-link>
              <router-link
                to="/audit/comments"
                :class="['nav-item nav-sub-item', { active: route.path === '/audit/comments' }]"
              >
                <span>评论</span>
              </router-link>
              <router-link
                to="/audit/knowledge"
                :class="['nav-item nav-sub-item', { active: route.path === '/audit/knowledge' }]"
              >
                <span>知识库文件</span>
              </router-link>
            </div>
          </div>
          <div class="nav-group">
            <button type="button" class="nav-item nav-group-title" @click="assetAuditOpen = !assetAuditOpen">
              <el-icon><Document /></el-icon>
              <span>资料审核</span>
              <el-icon class="nav-chevron" :class="{ open: assetAuditOpen }"><ArrowDown /></el-icon>
            </button>
            <div v-show="assetAuditOpen" class="nav-sub">
              <router-link
                to="/audit/columns"
                :class="['nav-item nav-sub-item', { active: route.path === '/audit/columns' }]"
              >
                <span>专栏</span>
              </router-link>
              <router-link
                to="/audit/knowledge-base"
                :class="['nav-item nav-sub-item', { active: route.path === '/audit/knowledge-base' }]"
              >
                <span>知识库</span>
              </router-link>
              <router-link
                to="/audit/profile"
                :class="['nav-item nav-sub-item', { active: route.path === '/audit/profile' }]"
              >
                <span>个人资料</span>
              </router-link>
            </div>
          </div>
          <router-link to="/audit/logs" :class="['nav-item', { active: route.path === '/audit/logs' }]">
            <el-icon><Document /></el-icon>
            <span>审核记录</span>
          </router-link>
        </nav>
      </aside>

      <main class="audit-main">
        <template v-if="isAuditHome">
          <div class="card profile-card">
            <div class="profile-left">
              <span class="profile-avatar">
                <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
                <span v-else class="avatar-initial">{{ avatarInitial }}</span>
              </span>
              <div class="profile-info">
                <div class="profile-name">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}
                  <span class="admin-badge">ADMIN</span>
                </div>
                <div class="profile-stats">
                  <span>统一审核队列：AI 预审 + 人工复核</span>
                </div>
              </div>
            </div>
          </div>

          <div class="card metrics-card metrics-card-top">
            <div class="metric-item">
              <div class="metric-label">待处理（含待 AI）</div>
              <div class="metric-value">{{ stats.pending }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">待人工</div>
              <div class="metric-value">{{ stats.pendingHuman }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">今日已结单</div>
              <div class="metric-value">{{ stats.todayFinished }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">近 7 日驳回</div>
              <div class="metric-value">{{ stats.rejected7d }}</div>
            </div>
          </div>

          <div class="card audit-queue-card">
            <div class="blog-header-row">
              <h2 class="section-title">待审概览</h2>
              <span class="audit-hint-inline">按业务类型查看待 AI / 待人工</span>
            </div>
            <div class="overview-grid">
              <div class="overview-item">
                <div class="overview-name">博客</div>
                <div class="overview-values">
                  <span>待AI {{ homeBreakdown.article.pending }}</span>
                  <span>待人工 {{ homeBreakdown.article.human }}</span>
                </div>
              </div>
              <div class="overview-item">
                <div class="overview-name">公开知识库</div>
                <div class="overview-values">
                  <span>待AI {{ homeBreakdown.knowledge.pending }}</span>
                  <span>待人工 {{ homeBreakdown.knowledge.human }}</span>
                </div>
              </div>
              <div class="overview-item">
                <div class="overview-name">专栏</div>
                <div class="overview-values">
                  <span>待AI {{ homeBreakdown.column.pending }}</span>
                  <span>待人工 {{ homeBreakdown.column.human }}</span>
                </div>
              </div>
              <div class="overview-item">
                <div class="overview-name">知识库</div>
                <div class="overview-values">
                  <span>待AI {{ homeBreakdown.knowledgeBase.pending }}</span>
                  <span>待人工 {{ homeBreakdown.knowledgeBase.human }}</span>
                </div>
              </div>
              <div class="overview-item">
                <div class="overview-name">评论</div>
                <div class="overview-values">
                  <span>待AI {{ homeBreakdown.comment.pending }}</span>
                  <span>待人工 {{ homeBreakdown.comment.human }}</span>
                </div>
              </div>
              <div class="overview-item">
                <div class="overview-name">个人资料</div>
                <div class="overview-values">
                  <span>待AI {{ homeBreakdown.profile.pending }}</span>
                  <span>待人工 {{ homeBreakdown.profile.human }}</span>
                </div>
              </div>
            </div>
            <div v-if="recentTasks.length" class="recent-list">
              <h3 class="recent-title">最近任务</h3>
              <div
                v-for="task in recentTasks"
                :key="task.id"
                class="recent-item"
              >
                <span class="recent-type">{{ moderationResourceLabel(task.resourceType) }}</span>
                <span class="recent-id" :title="displayResourceTitle(task)">{{ displayResourceTitle(task) }}</span>
                <span class="recent-status">{{ moderationStatusLabel(task.status) }}</span>
                <span class="recent-time">{{ formatTime(task.createdAt) }}</span>
              </div>
            </div>
            <div v-else class="blog-empty">
              <el-icon class="blog-empty-icon"><Document /></el-icon>
              <p class="blog-empty-text">当前没有审核任务，系统运行正常。</p>
            </div>
          </div>
        </template>

        <div v-else class="audit-task-page">
          <div class="card audit-task-card">
            <div class="audit-task-head">
              <div>
                <h2 class="audit-task-title">{{ pageTitle }}</h2>
                <p class="audit-task-desc">{{ pageDesc }}</p>
              </div>
              <router-link to="/audit" class="audit-back-link">← 返回审核首页</router-link>
            </div>

            <div class="audit-toolbar">
              <el-radio-group v-if="isLogsPage" v-model="statusFilter" size="small" @change="reloadList">
                <el-radio-button label="ALL">全部</el-radio-button>
                <el-radio-button label="APPROVED">已通过</el-radio-button>
                <el-radio-button label="REJECTED">已驳回</el-radio-button>
              </el-radio-group>
              <el-radio-group v-else v-model="statusFilter" size="small" @change="reloadList">
                <el-radio-button label="ALL">全部</el-radio-button>
                <el-radio-button label="PENDING">待 AI</el-radio-button>
                <el-radio-button label="NEEDS_HUMAN">待人工</el-radio-button>
                <el-radio-button label="APPROVED">已通过</el-radio-button>
                <el-radio-button label="REJECTED">已驳回</el-radio-button>
              </el-radio-group>
            </div>

            <el-table v-loading="loading" :data="tasks" stripe class="audit-table" empty-text="暂无数据">
              <el-table-column prop="id" label="任务 ID" width="88" />
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  {{ moderationResourceLabel(row.resourceType) }}
                </template>
              </el-table-column>
              <el-table-column label="标题" min-width="180">
                <template #default="{ row }">
                  <div class="table-title-cell" :title="displayResourceTitle(row)">
                    {{ displayResourceTitle(row) }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="提交者" width="150">
                <template #default="{ row }">
                  <div class="table-user-cell">
                    <img v-if="row.ownerAvatar" :src="row.ownerAvatar" alt="avatar" class="table-user-avatar" />
                    <span v-else class="table-user-avatar table-user-avatar-fallback">
                      {{ ((row.ownerUsername || `用户${row.ownerUserId}`) as string).slice(0, 1).toUpperCase() }}
                    </span>
                    <span class="table-user-name">{{ row.ownerUsername || `用户${row.ownerUserId}` }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <span :class="['status-pill', statusPillClass(row.status)]">
                    {{ moderationStatusLabel(row.status) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="AI" width="100">
                <template #default="{ row }">
                  <span v-if="row.aiDecision" :class="['status-pill', aiPillClass(row.aiDecision)]">
                    {{ aiDecisionLabel(row.aiDecision) }}
                  </span>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" min-width="160">
                <template #default="{ row }">
                  {{ formatTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column v-if="isLogsPage" label="审核员" width="130">
                <template #default="{ row }">
                  {{ reviewerName(row) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-dropdown
                    trigger="click"
                    popper-class="audit-action-dropdown"
                    @command="(cmd: string | number) => onTaskAction(row, String(cmd))"
                  >
                    <el-button type="primary" link size="small">处理</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="view">查看内容</el-dropdown-item>
                        <el-dropdown-item command="ai">重跑AI</el-dropdown-item>
                        <el-dropdown-item command="approve">人工通过</el-dropdown-item>
                        <el-dropdown-item command="reject">人工驳回</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="total > 0" class="audit-pagination">
              <el-pagination
                v-model:current-page="page"
                :page-size="pageSize"
                :total="total"
                layout="total, prev, pager, next"
                @current-change="reloadList"
              />
            </div>
          </div>

          <el-dialog
            v-model="previewVisible"
            title="审核内容预览"
            width="720px"
            destroy-on-close
            class="audit-preview-dialog"
          >
            <div v-if="previewTask" class="preview-wrap">
              <div class="preview-meta">
                <span>类型：{{ moderationResourceLabel(previewTask.resourceType) }}</span>
                <span>状态：{{ moderationStatusLabel(previewTask.status) }}</span>
                <span>AI：{{ previewTask.aiDecision ? aiDecisionLabel(previewTask.aiDecision) : '—' }}</span>
              </div>

              <div class="preview-section">
                <div class="preview-label">标题</div>
                <div class="preview-value">{{ previewData.title || '（无）' }}</div>
              </div>

              <div class="preview-section" v-if="previewData.summary">
                <div class="preview-label">摘要</div>
                <div class="preview-value">{{ previewData.summary }}</div>
              </div>

              <div class="preview-section" v-if="previewData.cover">
                <div class="preview-label">封面</div>
                <img :src="previewData.cover" alt="cover" class="preview-cover" />
              </div>

              <div class="preview-section">
                <div class="preview-label">正文</div>
                <div v-if="previewData.body" class="preview-markdown markdown-body" v-html="previewBodyHtml"></div>
                <div v-else class="preview-value">（无）</div>
              </div>

              <div class="preview-section" v-if="previewData.reason">
                <div class="preview-label">审核原因</div>
                <div class="preview-value preview-reason">{{ previewData.reason }}</div>
              </div>
            </div>
          </el-dialog>
        </div>
      </main>

      <aside v-if="isAuditHome" class="audit-sidebar-right">
        <div class="card tip-card">
          <h3 class="tip-title">审核提示</h3>
          <ul class="tip-list">
            <li>先由 AI 完成初审，再按结果流转到通过、驳回或待人工处理。</li>
            <li>带图片的博客、专栏、知识库资料，AI 通过后会进入人工二次审核。</li>
            <li>AI 明确判定不通过的内容会直接驳回，并附上对应原因。</li>
            <li>管理员发布的内容默认不进入审核队列。</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { House, Folder, Document, ArrowDown, HomeFilled, User } from '@element-plus/icons-vue'
import {
  listModerationTasks,
  getModerationStats,
  humanReviewModerationTask,
  rerunAiReview,
  moderationResourceLabel,
  moderationStatusLabel,
  type ModerationResourceType,
  type ModerationTask,
} from '@/api/moderation'
import { getUsersBatch } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { renderMarkdownWithMath } from '@/utils/markdown'

const route = useRoute()
const userStore = useUserStore()

const contentAuditOpen = ref(true)
const assetAuditOpen = ref(true)
const isAuditHome = computed(() => route.path === '/audit')
const isLogsPage = computed(() => route.path === '/audit/logs')

const stats = ref({ pending: 0, pendingHuman: 0, todayFinished: 0, rejected7d: 0 })
const homeBreakdown = ref({
  article: { pending: 0, human: 0 },
  knowledge: { pending: 0, human: 0 },
  column: { pending: 0, human: 0 },
  knowledgeBase: { pending: 0, human: 0 },
  comment: { pending: 0, human: 0 },
  profile: { pending: 0, human: 0 },
})
const recentTasks = ref<ModerationTask[]>([])

const tasks = ref<ModerationTask[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(false)
const statusFilter = ref('ALL')
const previewVisible = ref(false)
const previewTask = ref<ModerationTask | null>(null)
const previewData = ref({ title: '', summary: '', cover: '', body: '', reason: '' })
const previewBodyHtml = computed(() => renderMarkdownWithMath(previewData.value.body || ''))

const pageTitle = computed(() => {
  const m: Record<string, string> = {
    '/audit/articles': '待审博客',
    '/audit/knowledge': '公开知识库文档',
    '/audit/columns': '专栏审核',
    '/audit/knowledge-base': '知识库审核',
    '/audit/comments': '评论审核',
    '/audit/profile': '个人资料审核',
    '/audit/logs': '审核记录',
  }
  return m[route.path] ?? '审核'
})

const pageDesc = computed(() => {
  const m: Record<string, string> = {
    '/audit/articles': '处理社区博客内容，AI 初审通过后进入人工复核。',
    '/audit/knowledge': '处理公开知识库文件，重点检查正文、封面与导向信息。',
    '/audit/columns': '处理专栏资料，审核封面与简介是否符合发布规范。',
    '/audit/knowledge-base': '处理知识库资料，审核封面与简介后再对外展示。',
    '/audit/comments': '处理全站评论，重点拦截违规言论与不当引导。',
    '/audit/profile': '处理用户资料变更，确保公开信息真实、合规、可展示。',
    '/audit/logs': '查看近期审核结果与处理记录，支持复盘与追踪。',
  }
  return m[route.path] ?? ''
})

function resourceTypeForPath(): ModerationResourceType | undefined {
  if (route.path === '/audit/articles') return 'ARTICLE'
  if (route.path === '/audit/knowledge') return 'KNOWLEDGE_DOC'
  if (route.path === '/audit/columns') return 'COLUMN'
  if (route.path === '/audit/knowledge-base') return 'KNOWLEDGE_BASE'
  if (route.path === '/audit/comments') return 'COMMENT'
  if (route.path === '/audit/profile') return 'USER_PROFILE'
  return undefined
}

async function reloadList() {
  if (isAuditHome.value) return
  loading.value = true
  try {
    const rt = resourceTypeForPath()
    const res = await listModerationTasks({
      page: page.value,
      pageSize,
      resourceType: rt,
      status: statusFilter.value,
      finishedOnly: isLogsPage.value,
    })
    tasks.value = res.records
    await enrichTaskUsers(tasks.value)
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function enrichTaskUsers(list: ModerationTask[]) {
  const missingIds = Array.from(
    new Set(
      list
        .filter((t) => t?.ownerUserId && (!t.ownerUsername || !t.ownerAvatar))
        .map((t) => Number(t.ownerUserId))
        .filter((id) => Number.isFinite(id) && id > 0)
    )
  )
  if (!missingIds.length) return
  try {
    const users = await getUsersBatch(missingIds)
    const byId = new Map<number, { nickname?: string; username?: string; avatar?: string }>()
    users.forEach((u) => {
      if (u?.id) byId.set(Number(u.id), { nickname: u.nickname, username: u.username, avatar: u.avatar })
    })
    list.forEach((t) => {
      const u = byId.get(Number(t.ownerUserId))
      if (!u) return
      if (!t.ownerUsername) t.ownerUsername = (u.nickname || u.username || '').trim()
      if (!t.ownerAvatar) t.ownerAvatar = u.avatar || ''
    })
  } catch {
    // ignore fallback fetch errors
  }
}

async function loadStats() {
  stats.value = await getModerationStats()
}

async function loadHomePanels() {
  if (!isAuditHome.value) return
  try {
    const [
      aPending, aHuman,
      kPending, kHuman,
      colPending, colHuman,
      kbPending, kbHuman,
      cPending, cHuman,
      pPending, pHuman,
      recent,
    ] = await Promise.all([
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'ARTICLE', status: 'PENDING' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'ARTICLE', status: 'NEEDS_HUMAN' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'KNOWLEDGE_DOC', status: 'PENDING' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'KNOWLEDGE_DOC', status: 'NEEDS_HUMAN' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'COLUMN', status: 'PENDING' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'COLUMN', status: 'NEEDS_HUMAN' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'KNOWLEDGE_BASE', status: 'PENDING' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'KNOWLEDGE_BASE', status: 'NEEDS_HUMAN' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'COMMENT', status: 'PENDING' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'COMMENT', status: 'NEEDS_HUMAN' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'USER_PROFILE', status: 'PENDING' }),
      listModerationTasks({ page: 1, pageSize: 1, resourceType: 'USER_PROFILE', status: 'NEEDS_HUMAN' }),
      listModerationTasks({ page: 1, pageSize: 5, status: 'ALL' }),
    ])
    homeBreakdown.value = {
      article: { pending: aPending.total, human: aHuman.total },
      knowledge: { pending: kPending.total, human: kHuman.total },
      column: { pending: colPending.total, human: colHuman.total },
      knowledgeBase: { pending: kbPending.total, human: kbHuman.total },
      comment: { pending: cPending.total, human: cHuman.total },
      profile: { pending: pPending.total, human: pHuman.total },
    }
    recentTasks.value = recent.records || []
  } catch {
    homeBreakdown.value = {
      article: { pending: 0, human: 0 },
      knowledge: { pending: 0, human: 0 },
      column: { pending: 0, human: 0 },
      knowledgeBase: { pending: 0, human: 0 },
      comment: { pending: 0, human: 0 },
      profile: { pending: 0, human: 0 },
    }
    recentTasks.value = []
  }
}

watch(
  () => route.path,
  () => {
    page.value = 1
    statusFilter.value = 'ALL'
    reloadList()
  },
  { immediate: true }
)

watch(statusFilter, () => {
  if (!isAuditHome.value) {
    page.value = 1
    reloadList()
  }
})

watch(isAuditHome, (home) => {
  if (home) {
    loadStats()
    loadHomePanels()
  }
})

onMounted(() => {
  loadStats()
  loadHomePanels()
})

const avatarUrl = computed(() => (userStore.userInfo as { avatar?: string })?.avatar || '')
const avatarInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || '管'
  return name.charAt(0).toUpperCase()
})

function formatTime(s: string | undefined) {
  if (!s) return '—'
  return s.replace('T', ' ').slice(0, 19)
}

function safeJsonParse<T = Record<string, unknown>>(raw: unknown): T | null {
  if (!raw || typeof raw !== 'string') return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

function extractFromLinePayload(payload: string) {
  const lines = payload.split('\n')
  const map: Record<string, string> = {}
  let currentKey = ''
  const allowedKeys = new Set([
    'title',
    'summary',
    'cover',
    'body',
    'nickname',
    'intro',
    'residence',
    'industry',
    'bio',
    'name',
    'content',
  ])
  lines.forEach((line) => {
    const idx = line.indexOf('=')
    if (idx > 0) {
      const k = line.slice(0, idx).trim()
      if (allowedKeys.has(k)) {
        const v = line.slice(idx + 1)
        map[k] = v
        currentKey = k
        return
      }
    }
    if (currentKey) {
      map[currentKey] = `${map[currentKey] ?? ''}\n${line}`
    }
  })
  return map
}

function parseStructuredPayload(payload: string) {
  const text = (payload || '').trim()
  const result = { title: '', summary: '', cover: '', body: '' }
  if (!text) return result

  const assignFromKv = (kv: Record<string, string>) => {
    result.title = (kv.title || kv.nickname || result.title).trim()
    result.summary = (kv.summary || kv.intro || result.summary).trim()
    result.cover = (kv.cover || result.cover).trim()
    const mergedBody = [kv.body, kv.bio, kv.residence, kv.industry].filter(Boolean).join('\n').trim()
    if (mergedBody) {
      result.body = mergedBody
      return
    }
    const profileLines = [
      kv.nickname ? `昵称：${kv.nickname}` : '',
      kv.intro ? `简介：${kv.intro}` : '',
      kv.residence ? `居住地：${kv.residence}` : '',
      kv.industry ? `行业：${kv.industry}` : '',
      kv.bio ? `个人资料：${kv.bio}` : '',
    ].filter(Boolean)
    if (profileLines.length) {
      result.body = profileLines.join('\n')
    }
  }

  const payloadJson = safeJsonParse<Record<string, unknown>>(text)
  if (payloadJson) {
    result.title = String(payloadJson.title || payloadJson.name || '').trim()
    result.summary = String(payloadJson.summary || payloadJson.intro || '').trim()
    result.cover = String(payloadJson.cover || '').trim()
    result.body = String(payloadJson.body || payloadJson.content || '').trim()
    const textField = String(payloadJson.text || '').trim()
    if (textField) {
      if (textField.includes('\n') && textField.includes('=')) {
        assignFromKv(extractFromLinePayload(textField))
        if (!result.body) result.body = textField
      } else if (!result.body) {
        result.body = textField
      }
    }
    if (!result.body) result.body = text
    return result
  }

  if (text.includes('\n') && text.includes('=')) {
    assignFromKv(extractFromLinePayload(text))
    if (!result.body) result.body = text
    return result
  }

  result.body = text
  return result
}

function openPreview(row: ModerationTask) {
  previewTask.value = row
  let title = ''
  let summary = ''
  let cover = ''
  let body = ''
  let reason = row.humanNote || ''

  if (row.aiDetail) {
    if (typeof row.aiDetail === 'object') {
      const r = String((row.aiDetail as Record<string, unknown>).reason || '').trim()
      if (r) reason = r
    } else {
      const parsed = safeJsonParse<Record<string, unknown>>(row.aiDetail)
      const r = String(parsed?.reason || '').trim()
      if (r) reason = r
    }
  }

  const payload = (row.payloadSnapshot || '').trim()
  const sections = parseStructuredPayload(payload)
  title = sections.title
  summary = sections.summary
  cover = sections.cover
  body = sections.body
  if (row.resourceType === 'COMMENT' && !title) title = '评论内容'
  if (!title) title = moderationResourceLabel(row.resourceType)

  previewData.value = { title, summary, cover, body, reason }
  previewVisible.value = true
}

function displayResourceTitle(row: ModerationTask): string {
  const backendTitle = String(row.resourceTitle || '').trim()
  if (backendTitle) return backendTitle
  const parsed = parseStructuredPayload(String(row.payloadSnapshot || ''))
  if (parsed.title) return parsed.title
  return `#${row.resourceId}`
}

function aiDecisionLabel(decision?: string | null): string {
  const d = String(decision || '').toUpperCase()
  if (d === 'PASS') return '通过'
  if (d === 'REJECT') return '未通过'
  if (d === 'NEEDS_HUMAN') return '待人工'
  return d || '—'
}

function aiPillClass(decision?: string | null): string {
  const d = String(decision || '').toUpperCase()
  if (d === 'PASS') return 'status-ok'
  if (d === 'REJECT') return 'status-bad'
  return 'status-warn'
}

function statusPillClass(status?: string | null): string {
  const s = String(status || '').toUpperCase()
  if (s === 'APPROVED') return 'status-ok'
  if (s === 'REJECTED') return 'status-bad'
  return 'status-warn'
}

function reviewerName(row: ModerationTask): string {
  const name = String(row.humanReviewerName || '').trim()
  if (name) return name
  if (row.status === 'APPROVED' || row.status === 'REJECTED') return 'AI审核'
  return '—'
}

async function onTaskAction(row: ModerationTask, command: string) {
  if (!row?.id) return
  try {
    if (command === 'view') {
      openPreview(row)
      return
    } else if (command === 'ai') {
      await rerunAiReview(row.id)
      ElMessage.success('已重跑 AI 审核')
    } else if (command === 'approve') {
      await humanReviewModerationTask(row.id, 'APPROVE')
      ElMessage.success('已人工通过')
    } else if (command === 'reject') {
      const promptResult = await ElMessageBox.prompt('请输入驳回原因（可选）', '人工驳回', {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：包含违规导向信息，请删除后再提交',
        inputValue: '',
        distinguishCancelAndClose: true,
      }).catch(() => null)
      if (!promptResult || typeof promptResult !== 'object' || !('value' in promptResult)) return
      const note = String(promptResult.value || '')
      await humanReviewModerationTask(row.id, 'REJECT', note)
      ElMessage.success('已人工驳回')
    }
    await Promise.all([reloadList(), loadStats()])
    await loadHomePanels()
  } catch (e) {
    // 全局拦截器已提示
  }
}
</script>

<style scoped>
.audit-layout {
  min-height: 100vh;
  background: #f5f5f5;
  --el-color-primary: #bb1919;
  --el-color-primary-light-3: #c74848;
  --el-color-primary-light-5: #d77171;
  --el-color-primary-light-7: #e69f9f;
  --el-color-primary-light-8: #efbaba;
  --el-color-primary-light-9: #f8dede;
  --el-color-primary-dark-2: #8f1515;
}

.audit-header {
  height: 64px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  text-decoration: none;
}

.audit-logo {
  height: 40px;
  width: auto;
  transition: transform 0.2s ease;
}

.logo-link:hover .audit-logo {
  transform: scale(1.08);
}

.audit-title-bar {
  font-size: 18px;
  font-weight: 600;
  color: #111;
}

.audit-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #111;
  overflow: hidden;
}

.audit-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.audit-avatar .avatar-initial {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.audit-body {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
}

.audit-sidebar-left {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  height: fit-content;
}

.back-site-btn-full {
  width: 100%;
  margin-bottom: 20px;
}

.back-site-btn {
  background: #b31b1b !important;
  border-color: #b3061b !important;
}

.back-site-btn:hover {
  background: #8b0000 !important;
  border-color: #8b0000 !important;
}

.audit-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  color: #444;
  text-decoration: none;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.nav-item:hover {
  background: #f0f0f0;
  color: #111;
}

.nav-item.active {
  background: #fff5f5;
  color: #111;
  font-weight: 500;
  border-left: 3px solid #b31b1b;
  margin-left: -3px;
  padding-left: 19px;
}

.nav-group {
  margin-top: 4px;
}

.nav-group-title {
  width: 100%;
  text-align: left;
  cursor: pointer;
  border: none;
  background: transparent;
  font: inherit;
}

.nav-group-title span {
  flex: 1;
}

.nav-chevron {
  margin-left: auto;
  font-size: 12px;
  transition: transform 0.2s;
}

.nav-chevron.open {
  transform: rotate(180deg);
}

.nav-sub {
  padding-left: 8px;
  margin-top: 2px;
  margin-left: 18px;
}

.nav-sub-item {
  padding: 10px 16px 10px 12px !important;
  font-size: 14px;
}

.audit-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.audit-sidebar-right {
  width: 280px;
  flex-shrink: 0;
  align-self: flex-start;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.profile-card .profile-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: #111;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar .avatar-initial {
  color: #fff;
  font-size: 22px;
  font-weight: 600;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: #111;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-badge {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #b31b1b;
  background: #fff5f5;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #f0d0d0;
}

.profile-stats {
  font-size: 14px;
  color: #666;
}

.metrics-card-top {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.metrics-card-top .metric-item {
  flex: 1;
  min-width: 0;
  text-align: left;
  padding: 16px 20px;
  border-left: 1px solid #eee;
}

.metrics-card-top .metric-item:first-child {
  border-left: none;
}

.metrics-card-top .metric-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.metrics-card-top .metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}

.metric-sub {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  word-break: break-word;
}

.audit-queue-card .blog-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.audit-hint-inline {
  font-size: 13px;
  color: #999;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.overview-item {
  border: 1px solid #f0f0f0;
  border-left: 3px solid #bb1919;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.overview-name {
  font-size: 14px;
  font-weight: 700;
  color: #111;
  margin-bottom: 8px;
}

.overview-values {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #666;
}

.recent-list {
  margin-top: 8px;
}

.recent-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: #222;
  font-weight: 700;
}

.recent-item {
  display: grid;
  grid-template-columns: 120px 1fr 90px 130px;
  gap: 8px;
  padding: 8px 0;
  border-top: 1px dashed #eee;
  font-size: 13px;
  align-items: center;
}

.recent-type {
  color: #333;
  font-weight: 600;
}

.recent-id {
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-status {
  color: #bb1919;
}

.recent-time {
  color: #999;
  text-align: right;
}

.audit-queue-card .section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111;
}

.blog-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

.blog-empty-icon {
  font-size: 80px;
  color: #d0d0d0;
  margin-bottom: 16px;
}

.blog-empty-text {
  margin: 0;
  font-size: 15px;
  color: #999;
  text-align: center;
  max-width: 420px;
  line-height: 1.6;
}

.audit-task-page {
  width: 100%;
}

.audit-task-card {
  padding-bottom: 16px;
}

.audit-task-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.audit-task-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #111;
}

.audit-task-desc {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.audit-back-link {
  font-size: 14px;
  font-weight: 600;
  color: #b31b1b;
  text-decoration: none;
  flex-shrink: 0;
}

.audit-back-link:hover {
  text-decoration: underline;
}

.audit-toolbar {
  margin-bottom: 16px;
}

.audit-toolbar :deep(.el-radio-button__inner) {
  border-color: #e3e3e3;
  color: #555;
}

.audit-toolbar :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: #bb1919;
  border-color: #bb1919;
  color: #fff;
  box-shadow: -1px 0 0 0 #bb1919;
}

.audit-table {
  width: 100%;
}

.audit-task-card :deep(.el-table__header th) {
  background: #fafafa;
  color: #606266;
  font-weight: 600;
}

.table-title-cell {
  color: #222;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-user-cell {
  color: #555;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.table-user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.table-user-avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #bb1919;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.table-user-name {
  color: #444;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
}

.status-pill.status-ok {
  background: #eaf7ef;
  color: #1f8f4d;
}

.status-pill.status-warn {
  background: #fff4e5;
  color: #b76a00;
}

.status-pill.status-bad {
  background: #fdecec;
  color: #bb1919;
}

.audit-table :deep(.el-button--primary.is-link) {
  color: #bb1919;
}

.audit-table :deep(.el-button--primary.is-link:hover) {
  color: #8f1515;
}

.audit-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.audit-pagination :deep(.el-pager li.is-active) {
  color: #bb1919;
}

.audit-pagination :deep(.el-pagination button:hover),
.audit-pagination :deep(.el-pager li:hover) {
  color: #bb1919;
}

:global(.audit-action-dropdown) {
  border: 1px solid #f0d6d6;
  border-radius: 10px;
  padding: 4px;
  box-shadow: 0 10px 26px rgba(187, 25, 25, 0.16);
}

:global(.audit-action-dropdown .el-dropdown-menu__item) {
  border-radius: 6px;
  color: #333;
  font-weight: 500;
}

:global(.audit-action-dropdown .el-dropdown-menu__item:hover) {
  background: #fff1f1;
  color: #bb1919;
}

:global(.audit-action-dropdown .el-dropdown-menu__item:not(.is-disabled):last-child) {
  color: #bb1919;
}

.preview-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #666;
}

.preview-section {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fafafa;
}

.preview-label {
  font-size: 12px;
  color: #888;
  margin-bottom: 6px;
}

.preview-value {
  color: #222;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-content {
  margin: 0;
  max-height: 320px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.7;
  color: #222;
  font-family: inherit;
}

.preview-markdown {
  max-height: 360px;
  overflow: auto;
  color: #222;
  line-height: 1.7;
}

.preview-markdown :deep(p) {
  margin: 0 0 10px;
}

.preview-markdown :deep(pre) {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 10px;
  overflow: auto;
}

.preview-cover {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.preview-reason {
  color: #b31b1b;
}

.tip-card {
  position: sticky;
  top: 88px;
}

.tip-title {
  margin: 0 0 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

.tip-list {
  margin: 0;
  padding-left: 18px;
  color: #555;
  font-size: 14px;
  line-height: 1.7;
}

.tip-list li {
  margin-bottom: 8px;
}

</style>
