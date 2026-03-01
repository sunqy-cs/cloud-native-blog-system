<template>
  <div class="blog-page">
    <!-- 顶部导航：HOME（默认左侧且默认选中）+ 该用户的专栏名；支持 userId 便于以后查看他人博客 -->
    <nav class="blog-sub-nav">
      <div class="blog-sub-nav-inner" ref="subNavWrapperRef">
        <router-link :to="blogNavQuery({ columnId: undefined })" class="sub-nav-item sub-nav-home" :class="{ active: !currentColumnId }">全部</router-link>
        <router-link
          v-for="col in navColumns"
          :key="col.id"
          :to="blogNavQuery({ columnId: col.id })"
          class="sub-nav-item"
          :class="{ active: currentColumnId === col.id }"
        >
          {{ col.name }}
        </router-link>
        <div class="sub-nav-indicator" :style="subNavIndicatorStyle"></div>
      </div>
    </nav>

    <div class="blog-layout">
      <!-- 分割线以上：轮播 + Most Popular，占满宽，无侧栏 -->
      <div class="blog-top">
        <main class="blog-main-full">
          <section class="carousel-section">
            <el-carousel v-if="carouselList.length" height="280px" :interval="5000">
              <el-carousel-item v-for="item in carouselList" :key="item.id">
                <div class="carousel-slide">
                  <div class="carousel-cover" :class="{ 'has-img': !!item.cover }">
                    <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                  </div>
                  <div class="carousel-content">
                    <h2 class="carousel-title">{{ item.title }}</h2>
                    <p class="carousel-summary">{{ item.summary || '暂无简介' }}</p>
                    <router-link :to="`/article/${item.id}`" class="carousel-read-btn">
                      阅读全文
                      <el-icon><ArrowRight /></el-icon>
                    </router-link>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
            <div v-else-if="!carouselLoading" class="carousel-placeholder">该专栏暂无文章</div>
          </section>
          <section class="section most-popular">
            <div class="section-head">
              <h3 class="section-title">人气最高</h3>
              <button type="button" class="section-link section-link-btn" @click="scrollToArticleList">查看全部</button>
            </div>
            <div v-if="popularList.length" class="popular-grid">
              <router-link
                v-for="item in popularList"
                :key="item.id"
                :to="`/article/${item.id}`"
                class="popular-card"
              >
                <div class="popular-card-body">
                  <h4 class="popular-card-title">{{ item.title }}</h4>
                  <div class="popular-card-meta">
                    {{ formatDate(item.createdAt) }}
                  </div>
                </div>
                <div class="popular-card-cover">
                  <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                  <span v-else class="popular-card-cover-placeholder">{{ item.title.charAt(0) }}</span>
                </div>
              </router-link>
            </div>
            <p v-else-if="!carouselLoading && !popularLoading" class="section-empty">该专栏暂无更多文章</p>
          </section>
        </main>
        <el-divider class="blog-divider" />
      </div>

      <!-- 分割线以下：文章列表 + 右侧边栏（订阅 + 专栏） -->
      <div class="blog-bottom">
        <main class="blog-main">
          <section id="article-list-section" class="section article-list-section">
            <div class="list-section-header">
              <el-tabs v-model="listTab" class="list-tabs">
                <el-tab-pane label="最新" name="latest" />
                <el-tab-pane label="最热" name="top" />
              </el-tabs>
              <div class="list-search-wrap" :class="{ expanded: listSearchExpanded }">
                <button type="button" class="list-search-trigger" title="搜索" aria-label="搜索" @click="expandListSearch">
                  <el-icon><Search /></el-icon>
                </button>
                <div class="list-search-expand">
                  <input
                    ref="listSearchInputRef"
                    v-model="listSearchInputBuffer"
                    type="text"
                    class="list-search-input"
                    placeholder="搜索文章、标签或作者"
                    @keyup.enter="onListSearchSubmit"
                  />
                  <el-icon class="list-search-input-icon"><Search /></el-icon>
                  <button type="button" class="list-search-cancel" @click="collapseListSearch">取消</button>
                </div>
              </div>
            </div>
            <p v-if="listSearchKeyword" class="blog-search-hint">
              搜索「{{ listSearchKeyword }}」
              <button type="button" class="blog-search-clear" @click="clearListSearch">清除</button>
            </p>
            <p v-if="listSearchKeyword && articleList.length === 0" class="article-list-empty">未找到匹配的文章</p>
            <div v-else class="article-list">
              <router-link
                v-for="item in articleList"
                :key="item.id"
                :to="`/article/${item.id}`"
                class="article-item"
              >
                <div class="article-item-main">
                  <h4 class="article-item-title">
                    <span v-for="(frag, i) in highlightFragments(item.title, listSearchKeyword)" :key="i">
                      <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                      <template v-else>{{ frag.value }}</template>
                    </span>
                  </h4>
                  <p v-if="item.summary" class="article-item-summary">
                    <span v-for="(frag, i) in highlightFragments(item.summary, listSearchKeyword)" :key="i">
                      <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                      <template v-else>{{ frag.value }}</template>
                    </span>
                  </p>
                  <div v-if="listSearchKeyword && item.tagNames?.length" class="article-item-tags">
                    <span v-for="(tagName, ti) in item.tagNames" :key="ti" class="article-item-tag">
                      <span v-for="(frag, fi) in highlightFragments(tagName, listSearchKeyword)" :key="fi">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                  </div>
                  <div class="article-item-stats">
                    <span class="stat"><el-icon><View /></el-icon> 阅读 {{ formatBlogCount(item.viewCount) }}</span>
                    <span class="stat"><el-icon><Star /></el-icon> 赞 {{ formatBlogCount(item.likeCount) }}</span>
                    <span class="stat"><el-icon><Collection /></el-icon> 收藏 {{ formatBlogCount(item.collectionCount) }}</span>
                    <span class="stat article-item-time">发布时间 {{ formatListDate(item.createdAt) }}</span>
                  </div>
                </div>
                <div class="article-item-cover">
                  <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                  <span v-else class="article-item-cover-placeholder">{{ item.title.charAt(0) }}</span>
                </div>
              </router-link>
            </div>
          </section>
        </main>
        <aside class="blog-sidebar-wrap">
          <div class="blog-sidebar-inner">
        <div class="sidebar-block author-card-block">
          <div class="author-card-header">
            <div class="author-card-avatar">
              <img v-if="blogAuthor?.avatar" :src="blogAuthor.avatar" :alt="blogAuthor.nickname || blogAuthor.username" class="author-avatar-img" />
              <span v-else class="avatar-initial">{{ authorInitial }}</span>
            </div>
            <div class="author-card-info">
              <span class="author-card-name">{{ blogAuthor?.nickname || blogAuthor?.username || '—' }}</span>
              <template v-if="authorDetailCollapsed">
                <span v-if="blogAuthor?.intro" class="author-card-desc">{{ blogAuthor.intro }}</span>
                <span v-else-if="blogAuthor?.bio" class="author-card-desc">{{ blogAuthor.bio }}</span>
                <span v-else class="author-card-desc">欢迎来到我的博客</span>
              </template>
              <template v-else>
                <p v-if="blogAuthor?.residence" class="author-detail-line">
                  <el-icon class="author-detail-icon"><Location /></el-icon>
                  <span class="author-detail-label">居住地</span>{{ blogAuthor.residence }}
                </p>
                <p v-if="blogAuthor?.industry" class="author-detail-line">
                  <el-icon class="author-detail-icon"><Briefcase /></el-icon>
                  <span class="author-detail-label">所在行业</span>{{ blogAuthor.industry }}
                </p>
                <p v-if="blogAuthor?.gender" class="author-detail-line">
                  <el-icon class="author-detail-icon"><User /></el-icon>
                  <span class="author-detail-label">性别</span>{{ blogAuthor.gender }}
                </p>
                <p v-if="blogAuthor?.bio" class="author-detail-line author-detail-bio">
                  <el-icon class="author-detail-icon"><Document /></el-icon>
                  <span class="author-detail-label">简介</span>{{ blogAuthor.bio }}
                </p>
              </template>
              <button v-if="blogAuthor" type="button" class="author-toggle-detail" @click="authorDetailCollapsed = !authorDetailCollapsed">
                <el-icon><component :is="authorDetailCollapsed ? ArrowDown : ArrowUp" /></el-icon>
                {{ authorDetailCollapsed ? '查看' : '收起' }}详细资料
              </button>
            </div>
          </div>
          <router-link :to="profileLink" class="btn-goto-profile">进入个人主页</router-link>
        </div>

        <div class="sidebar-block recommendations-block">
          <div class="section-head">
            <h3 class="section-title">专栏</h3>
            <router-link :to="blogNavQuery({ columnId: undefined })" class="section-link">全部</router-link>
          </div>
          <ul class="column-list">
            <li v-for="col in navColumns" :key="col.id" class="column-item">
              <router-link :to="blogNavQuery({ columnId: col.id })">
                <span class="column-name">{{ col.name }}</span>
                <span v-if="col.articleCount != null" class="column-count">{{ col.articleCount }} 篇</span>
              </router-link>
            </li>
          </ul>
        </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowRight, Search, ArrowDown, ArrowUp, Location, Briefcase, User, Document, View, Star, Collection } from '@element-plus/icons-vue'
import { getColumnsMe, type ColumnItem } from '@/api/column'
import { getContentsMe, type ContentListItem } from '@/api/content'
import { getMe, type UserMe } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前博客所属用户 id（今后查看他人博客时用 query.userId，未传则为自己） */
const blogUserId = computed(() => {
  const q = route.query.userId as string | undefined
  if (q) return Number(q)
  return userStore.userInfo?.id ?? null
})

const carouselList = ref<ContentListItem[]>([])
const popularList = ref<ContentListItem[]>([])
const articleList = ref<ContentListItem[]>([])
const carouselLoading = ref(false)
const popularLoading = ref(false)
const navColumns = ref<ColumnItem[]>([])
const blogAuthor = ref<UserMe | null>(null)
const authorDetailCollapsed = ref(true)
const listTab = ref('latest')

/** 博客作者名称首字（无头像时展示） */
const authorInitial = computed(() => {
  const a = blogAuthor.value
  if (!a) return '—'
  const name = a.nickname || a.username || ''
  return name.charAt(0) || '—'
})

/** 个人主页链接（当前为自己；今后可扩展为他人 /profile?userId=） */
const profileLink = computed(() => {
  const uid = blogUserId.value
  if (uid != null && route.query.userId != null) return { path: '/profile', query: { userId: String(uid) } }
  return { path: '/profile' }
})

/** 当前选中的专栏 id（来自 query.columnId），空表示 HOME */
const currentColumnId = computed(() => {
  const q = route.query.columnId as string | undefined
  if (!q) return undefined
  const n = Number(q)
  return Number.isNaN(n) ? undefined : n
})

/** 生成博客顶栏/侧栏链接的 query，便于扩展 userId */
function blogNavQuery(opts: { columnId?: number }) {
  const query: Record<string, string> = {}
  if (blogUserId.value != null && route.query.userId != null) query.userId = String(blogUserId.value)
  if (opts.columnId != null) query.columnId = String(opts.columnId)
  return { path: '/blog', query: Object.keys(query).length ? query : {} }
}

function formatDate(str: string) {
  if (!str) return ''
  const d = new Date(str)
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const day = d.getDate()
  return `${y}年${m}月${day}日`
}
/** 列表项阅读/赞/收藏数量展示（与个人主页一致：≥1000 显示 1.2k） */
function formatBlogCount(n: number) {
  if (n == null || Number.isNaN(n)) return '0'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
/** 列表项发布时间：YYYY-MM-DD */
function formatListDate(str: string) {
  if (!str) return ''
  return str.slice(0, 10)
}

// 列表区搜索：与个人主页一致，点击图标展开输入框，回车提交，取消只收起（不清已应用关键词）
const listSearchExpanded = ref(false)
const listSearchInputBuffer = ref('')  // 输入框内容
const listSearchKeyword = ref('')     // 已应用的搜索词（用于请求与提示）
const listSearchInputRef = ref<HTMLInputElement | null>(null)
function expandListSearch() {
  listSearchExpanded.value = true
  nextTick(() => listSearchInputRef.value?.focus())
}
function collapseListSearch() {
  listSearchExpanded.value = false
  listSearchInputBuffer.value = ''
}
function onListSearchSubmit() {
  const q = listSearchInputBuffer.value.trim()
  if (!q) return
  listSearchKeyword.value = q
  collapseListSearch()
  loadArticleList()
}
function clearListSearch() {
  listSearchKeyword.value = ''
  loadArticleList()
}
/** 将文本按关键词拆成片段用于高亮；与个人主页 highlightFragments 一致 */
function highlightFragments(
  text: string | undefined,
  keyword: string
): { type: 'text' | 'match'; value: string }[] {
  if (!text) return []
  const k = keyword.trim()
  if (!k) return [{ type: 'text', value: text }]
  const escaped = k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const re = new RegExp(`(${escaped})`, 'gi')
  const parts = text.split(re)
  return parts.map((value, i) => ({
    type: i % 2 === 1 ? 'match' : 'text',
    value,
  }))
}

function scrollToArticleList() {
  document.getElementById('article-list-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** 请求参数：仅在有专栏时传 columnId；列表请求可带搜索 q */
function contentsMeParams(overrides: { sortBy: string; order: string; page: number; pageSize: number; q?: string }) {
  const params: Record<string, string | number | undefined> = {
    status: 'PUBLISHED',
    sortBy: overrides.sortBy,
    order: overrides.order,
    page: overrides.page,
    pageSize: overrides.pageSize,
  }
  if (currentColumnId.value != null) params.columnId = currentColumnId.value
  if (overrides.q != null && overrides.q !== '') params.q = overrides.q
  return params
}

/** 轮播：当前专栏下最新 3 篇（按时间） */
async function loadCarousel() {
  if (!userStore.isLoggedIn) {
    carouselList.value = []
    return
  }
  carouselLoading.value = true
  try {
    const res = await getContentsMe(contentsMeParams({ sortBy: 'time', order: 'desc', page: 1, pageSize: 3 }))
    carouselList.value = res.list
  } finally {
    carouselLoading.value = false
  }
}

/** 人气最高：当前专栏下按点赞排序，排除轮播中已展示的，最多 4 篇 */
async function loadPopular() {
  if (!userStore.isLoggedIn) {
    popularList.value = []
    return
  }
  popularLoading.value = true
  try {
    const carouselIds = new Set(carouselList.value.map((c) => c.id))
    const res = await getContentsMe(contentsMeParams({ sortBy: 'likes', order: 'desc', page: 1, pageSize: 20 }))
    popularList.value = res.list.filter((item) => !carouselIds.has(item.id)).slice(0, 4)
  } finally {
    popularLoading.value = false
  }
}

/** 轮播 + 人气最高 一起加载（先轮播再人气，避免重复） */
async function loadTopSections() {
  await loadCarousel()
  await loadPopular()
}

function loadData() {
  if (userStore.isLoggedIn) {
    getColumnsMe().then((list) => { navColumns.value = list })
  }
  loadTopSections()
}

function loadArticleList() {
  if (!userStore.isLoggedIn) {
    articleList.value = []
    return
  }
  const sortBy = listTab.value === 'top' ? 'likes' : 'time'
  const q = listSearchKeyword.value.trim() || undefined
  getContentsMe(contentsMeParams({ sortBy, order: 'desc', page: 1, pageSize: 30, q })).then((res) => {
    articleList.value = res.list
  })
}

const subNavWrapperRef = ref<HTMLElement | null>(null)
const subNavIndicatorStyle = ref({ left: '0px', width: '0px' })

function updateSubNavIndicator() {
  nextTick(() => {
    const wrapper = subNavWrapperRef.value
    const activeEl = wrapper?.querySelector('.sub-nav-item.active') as HTMLElement | null
    if (!wrapper || !activeEl) {
      subNavIndicatorStyle.value = { left: '0px', width: '0px' }
      return
    }
    // 使用 offsetLeft/offsetWidth 相对父容器定位，避免居中/滚动后错位
    const left = activeEl.offsetLeft
    const width = activeEl.offsetWidth
    subNavIndicatorStyle.value = {
      left: `${left}px`,
      width: `${width}px`,
    }
  })
}

watch([listTab, currentColumnId], loadArticleList)
watch(currentColumnId, () => { loadTopSections() }, { flush: 'post' })
watch(currentColumnId, updateSubNavIndicator)
watch(navColumns, () => updateSubNavIndicator(), { flush: 'post' })
async function loadBlogAuthor() {
  if (!userStore.isLoggedIn || blogUserId.value == null) return
  if (blogUserId.value !== userStore.userInfo?.id) {
    // 今后查看他人博客时可在此根据 blogUserId 拉取对应用户信息
    return
  }
  try {
    blogAuthor.value = await getMe()
  } catch {
    blogAuthor.value = null
  }
}

onMounted(() => {
  loadData()
  loadArticleList()
  loadBlogAuthor()
  updateSubNavIndicator()
  window.addEventListener('resize', updateSubNavIndicator)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateSubNavIndicator)
})
</script>

<style scoped>
.blog-page {
  min-height: calc(100vh - 64px);
  background-color: var(--el-bg-color-page, #f5f5f5);
}

/* 顶部栏下方：Home + 标签，不随滚动条滚动 */
.blog-sub-nav {
  position: sticky;
  top: 64px;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.blog-sub-nav-inner {
  position: relative;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 52px;
}

.sub-nav-indicator {
  position: absolute;
  bottom: 0;
  height: 4px;
  background: #BB1919;
  transition: left 0.3s ease, width 0.3s ease;
  pointer-events: none;
}

.sub-nav-item {
  padding: 10px 18px;
  color: #444;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
}

.sub-nav-item:hover {
  color: #000;
  background: rgba(0, 0, 0, 0.05);
}

.sub-nav-item.active {
  color: #000;
  font-weight: 700;
  text-decoration: none;
}

.sub-nav-home {
  margin-right: 8px;
}

.sub-nav-more {
  cursor: pointer;
  user-select: none;
}

/* 主布局：上块（轮播+Most Popular+分割线）全宽，下块（文章列表+侧栏）左右并排 */
.blog-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 40px 48px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.blog-top {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.blog-main-full {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.blog-main-full .section {
  width: 100%;
}

/* 分割线以下：文章列表（左）+ 侧栏（右） */
.blog-bottom {
  width: 100%;
  display: flex;
  gap: 40px;
  margin-top: 0;
}

.blog-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

/* 轮播：与博客封面同比例 1200:280，浅色背景与整页统一 */
.carousel-section {
  width: 100%;
  max-width: 100%;
  margin-bottom: 48px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(145deg, #f8f9fa 0%, #eef0f2 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.carousel-section :deep(.el-carousel),
.carousel-section :deep(.el-carousel__container) {
  height: 280px;
}

.carousel-section :deep(.el-carousel__arrow) {
  background-color: rgba(0, 0, 0, 0.35);
}
.carousel-section :deep(.el-carousel__arrow:hover) {
  background-color: rgba(0, 0, 0, 0.5);
}
.carousel-section :deep(.el-carousel__indicator .el-carousel__button) {
  background-color: rgba(0, 0, 0, 0.2);
  width: 24px;
  border-radius: 2px;
}
.carousel-section :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  background-color: #b31b1b;
  width: 28px;
}

.carousel-placeholder {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #f8f9fa 0%, #eef0f2 100%);
  border-radius: 12px;
  color: #888;
  font-size: 15px;
}

.carousel-slide {
  position: relative;
  height: 100%;
  width: 100%;
}

/* 封面图：与博客封面同尺寸比例 1200:280，铺满轮播区域 */
.carousel-cover {
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, #e8eaed 0%, #dde0e4 100%);
}

.carousel-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 文字浮在左侧，带渐变遮罩保证可读 */
.carousel-content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 48px 0 56px;
  max-width: 52%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.96) 0%, rgba(255, 255, 255, 0.88) 65%, transparent 100%);
}

.carousel-title {
  font-size: 26px;
  font-weight: 700;
  color: #111;
  margin: 0 0 12px;
  line-height: 1.35;
}

.carousel-summary {
  font-size: 14px;
  color: #1a1a1a;
  margin: 0 0 20px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-weight: 500;
  text-shadow: 0 1px 1px rgba(255, 255, 255, 0.8);
}

.carousel-read-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: #b31b1b;
  color: #fff;
  border: none;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border-radius: 6px;
  transition: background 0.2s;
  align-self: flex-start;
}

.carousel-read-btn:hover {
  background: #8b0000;
  color: #fff;
}

/* 通用 section：主内容区宽度 */
.blog-main .section,
.blog-main-full .section {
  width: 100%;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: #111;
}

.section-link {
  font-size: 13px;
  color: #666;
  text-decoration: none;
}

.section-link:hover {
  color: #000;
}

.section-link-btn {
  padding: 0;
  font-size: 13px;
  color: #666;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
}
.section-link-btn:hover {
  color: #000;
}

/* Most Popular */
.most-popular {
  margin-bottom: 0;
  padding-bottom: 0;
}

.section-empty {
  margin: 0;
  padding: 24px;
  text-align: center;
  color: #888;
  font-size: 14px;
}

.popular-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.popular-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s, border-color 0.2s;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  min-height: 100px;
}

.popular-card:hover {
  border-color: #ccc;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.popular-card-body {
  flex: 1;
  min-width: 0;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.popular-card-cover {
  width: 100px;
  flex-shrink: 0;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.popular-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.popular-card-cover-placeholder {
  font-size: 28px;
  font-weight: 700;
  color: #bbb;
  user-select: none;
}

.popular-card-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 6px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.popular-card-meta {
  font-size: 12px;
  color: #888;
}

/* 分割线：上下严格分开，留白明显；在 blog-top 内占满宽 */
.blog-top .blog-divider {
  align-self: stretch;
  margin: 48px 0 40px;
  width: 100%;
}

/* 文章列表（分割线下方独立区块） */
.article-list-section {
  width: 100%;
  padding-top: 0;
}

.list-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.list-section-header .list-tabs {
  flex: 1;
}

/* 列表区搜索：与个人主页一致，点击图标展开为输入框 */
.list-search-wrap {
  display: flex;
  align-items: center;
  overflow: hidden;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.list-search-wrap.expanded {
  width: 260px;
}
.list-search-trigger {
  flex: 0 0 40px;
  min-width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  background: transparent;
  color: #555;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: flex 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s ease;
}
.list-search-trigger:hover {
  color: #111;
  background: rgba(0, 0, 0, 0.06);
}
.list-search-trigger .el-icon {
  font-size: 20px;
}
.list-search-wrap.expanded .list-search-trigger {
  flex: 0 0 0;
  min-width: 0;
  padding: 0;
  opacity: 0;
  pointer-events: none;
  overflow: hidden;
}
.list-search-expand {
  flex: 1 1 0;
  min-width: 0;
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 4px;
  opacity: 0;
  transform: translateX(-8px);
  transition: opacity 0.25s ease 0.06s, transform 0.28s cubic-bezier(0.4, 0, 0.2, 1) 0.06s;
}
.list-search-wrap.expanded .list-search-expand {
  opacity: 1;
  transform: translateX(0);
}
.list-search-input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: #999;
  pointer-events: none;
}
.list-search-input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 12px 0 36px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 14px;
  color: #111;
  background: #fff;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.list-search-input::placeholder {
  color: #999;
}
.list-search-input:focus {
  border-color: #333;
  box-shadow: 0 0 0 1px #333;
}
.list-search-cancel {
  flex-shrink: 0;
  padding: 0 10px;
  height: 34px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}
.list-search-cancel:hover {
  color: #111;
  background: rgba(0, 0, 0, 0.05);
}
/* 搜索提示与高亮（与个人主页一致） */
.blog-search-hint {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.blog-search-clear {
  padding: 2px 8px;
  font-size: 13px;
  color: #666;
  background: transparent;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.blog-search-clear:hover {
  color: #BB1919;
  border-color: #BB1919;
}
.search-highlight {
  background: rgba(187, 25, 25, 0.14);
  padding: 0 2px;
  border-radius: 2px;
}
.article-item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}
.article-item-tag {
  font-size: 12px;
  color: #666;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 4px;
}
.article-list-empty {
  font-size: 14px;
  color: #666;
  margin: 12px 0;
}

.list-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.list-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.list-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}

.list-tabs :deep(.el-tabs__active-bar) {
  background: #111;
}

.list-tabs :deep(.el-tabs__item.is-active) {
  color: #111;
  font-weight: 600;
}

.list-tabs :deep(.el-tabs__ink-bar) {
  background: #111;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.article-item {
  padding: 24px 0;
  border-bottom: 1px solid #eee;
  text-decoration: none;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 24px;
  transition: background 0.1s;
}

.article-item:hover {
  background: rgba(0, 0, 0, 0.02);
}

.article-item:last-child {
  border-bottom: none;
}

.article-item-main {
  flex: 1;
  min-width: 0;
}

.article-item-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 10px;
  line-height: 1.35;
}

.article-item-summary {
  font-size: 16px;
  color: #555;
  margin: 0 0 10px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-item-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 8px;
  font-size: 14px;
  color: #555;
}
.article-item-stats .stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.article-item-stats .stat :deep(.el-icon) {
  font-size: 14px;
  width: 14px;
  height: 14px;
  color: #999;
  flex-shrink: 0;
}
.article-item-stats .article-item-time {
  color: #888;
  margin-left: auto;
}

.article-item-cover {
  flex-shrink: 0;
  width: 180px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: #e8e8e8;
}

.article-item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-item-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  color: #bbb;
  user-select: none;
}

/* 侧栏：整列 sticky，粘在顶栏下，不随滚动滑没 */
.blog-sidebar-wrap {
  width: 300px;
  flex-shrink: 0;
  align-self: flex-start;
  position: sticky;
  top: 64px;
  max-height: calc(100vh - 64px);
  overflow-y: auto;
}

.blog-sidebar-inner {
  max-height: inherit;
}

.sidebar-block {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
}

.author-card-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.author-card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.author-card-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.author-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initial {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.author-card-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.author-card-name {
  font-weight: 700;
  font-size: 15px;
  color: #111;
}

.author-card-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.author-detail-line {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: #555;
  margin: 4px 0;
}

.author-detail-icon {
  flex-shrink: 0;
  font-size: 14px;
  color: #888;
  margin-top: 1px;
}

.author-detail-label {
  color: #888;
  margin-right: 4px;
  flex-shrink: 0;
}

.author-detail-bio {
  white-space: pre-wrap;
  word-break: break-word;
}

.author-toggle-detail {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding: 0;
  font-size: 13px;
  color: #666;
  background: none;
  border: none;
  cursor: pointer;
}

.author-toggle-detail:hover {
  color: #b31b1b;
}

.btn-goto-profile {
  display: block;
  width: 100%;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  background: #b31b1b;
  border: none;
  border-radius: 6px;
  text-align: center;
  text-decoration: none;
  transition: background 0.2s;
}

.btn-goto-profile:hover {
  background: #8b0000;
  color: #fff;
}

.recommendations-block .section-head {
  margin-bottom: 12px;
}

.column-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.column-item {
  border-bottom: 1px solid #eee;
}

.column-item:last-child {
  border-bottom: none;
}

.column-item a {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  color: #333;
  text-decoration: none;
  font-size: 14px;
}

.column-item a:hover {
  color: #000;
}

.column-name {
  font-weight: 500;
}

.column-count {
  font-size: 12px;
  color: #888;
}

@media (max-width: 1024px) {
  .popular-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .blog-bottom {
    flex-direction: column;
  }

  .blog-sidebar-wrap {
    width: 100%;
  }

  .carousel-slide {
    flex-direction: column;
    padding: 24px;
    text-align: center;
  }

  .popular-grid {
    grid-template-columns: 1fr;
  }
}
</style>
