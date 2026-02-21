<template>
  <div class="blog-page">
    <!-- 顶部栏下方：Home + 标签行（最多 5 个 + ... 折叠） -->
    <nav class="blog-sub-nav">
      <div class="blog-sub-nav-inner" ref="subNavWrapperRef">
        <router-link to="/blog" class="sub-nav-item sub-nav-home" :class="{ active: !currentTagId }">Home</router-link>
        <template v-for="(tag, index) in visibleTags" :key="tag.id">
          <router-link
            :to="tagRoute(tag)"
            class="sub-nav-item"
            :class="{ active: currentTagId === tag.id }"
          >
            {{ tag.name }}
          </router-link>
        </template>
        <el-dropdown v-if="moreTags.length > 0" trigger="click" @command="onTagCommand">
          <span class="sub-nav-item sub-nav-more">...</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="tag in moreTags"
                :key="tag.id"
                :command="tag"
              >
                {{ tag.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="sub-nav-indicator" :style="subNavIndicatorStyle"></div>
      </div>
    </nav>

    <div class="blog-layout">
      <!-- 分割线以上：轮播 + Most Popular，占满宽，无侧栏 -->
      <div class="blog-top">
        <main class="blog-main-full">
          <section class="carousel-section">
            <el-carousel v-if="carouselList.length" height="420px" :interval="5000">
              <el-carousel-item v-for="item in carouselList" :key="item.id">
                <div class="carousel-slide">
                  <div class="carousel-content">
                    <h2 class="carousel-title">{{ item.title }}</h2>
                    <p class="carousel-summary">{{ item.summary || '暂无简介' }}</p>
                    <router-link :to="`/article/${item.id}`" class="carousel-read-btn">
                      READ THE LATEST
                      <el-icon><ArrowRight /></el-icon>
                    </router-link>
                  </div>
                  <div class="carousel-cover" v-if="item.cover">
                    <img :src="item.cover" :alt="item.title" />
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </section>
          <section class="section most-popular">
            <div class="section-head">
              <h3 class="section-title">Most Popular</h3>
              <router-link :to="{ path: '/blog', query: { tab: 'popular' } }" class="section-link">VIEW ALL</router-link>
            </div>
            <div class="popular-grid">
            <router-link
              v-for="item in popularList"
              :key="item.id"
              :to="`/article/${item.id}`"
              class="popular-card"
            >
              <div class="popular-card-body">
                <h4 class="popular-card-title">{{ item.title }}</h4>
                <div class="popular-card-meta">
                  {{ formatDate(item.createdAt) }} · {{ item.author.nickname }}
                </div>
              </div>
              <div class="popular-card-cover">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                <span v-else class="popular-card-cover-placeholder">{{ item.title.charAt(0) }}</span>
              </div>
            </router-link>
            </div>
          </section>
        </main>
        <el-divider class="blog-divider" />
      </div>

      <!-- 分割线以下：文章列表 + 右侧边栏（订阅 + 专栏） -->
      <div class="blog-bottom">
        <main class="blog-main">
          <section class="section article-list-section">
            <div class="list-section-header">
              <el-tabs v-model="listTab" class="list-tabs">
                <el-tab-pane label="Latest" name="latest" />
                <el-tab-pane label="Top" name="top" />
                <el-tab-pane label="Discussions" name="discussions" />
              </el-tabs>
              <button type="button" class="list-search-btn" title="搜索" @click="onListSearch">
                <el-icon><Search /></el-icon>
              </button>
            </div>
            <div class="article-list">
              <router-link
                v-for="item in articleList"
                :key="item.id"
                :to="`/article/${item.id}`"
                class="article-item"
              >
                <div class="article-item-main">
                  <h4 class="article-item-title">{{ item.title }}</h4>
                  <p v-if="item.summary" class="article-item-summary">{{ item.summary }}</p>
                  <div class="article-item-meta">
                    {{ item.author.nickname }} · {{ formatDate(item.createdAt) }}
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
        <div class="sidebar-block subscribe-block">
          <div class="subscribe-header">
            <div class="subscribe-avatar">
              <span class="avatar-initial">博</span>
            </div>
            <div class="subscribe-info">
              <span class="subscribe-name">云原生博客</span>
              <span class="subscribe-desc">stay relevant</span>
            </div>
          </div>
          <div class="subscribe-form">
            <el-input v-model="subscribeEmail" placeholder="输入邮箱" size="default" />
            <el-button type="primary" class="subscribe-btn" @click="onSubscribe">Subscribe</el-button>
          </div>
          <el-button text class="unsubscribe-btn">Unsubscribe</el-button>
        </div>

        <div class="sidebar-block recommendations-block">
          <div class="section-head">
            <h3 class="section-title">专栏</h3>
            <router-link to="/knowledge" class="section-link">VIEW ALL {{ columns.length }}</router-link>
          </div>
          <ul class="column-list">
            <li v-for="col in columns" :key="col.id" class="column-item">
              <router-link :to="`/knowledge?column=${col.id}`">
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
import { ArrowRight, Search } from '@element-plus/icons-vue'
import {
  getBlogTags,
  getCarouselArticles,
  getPopularArticles,
  getArticleList,
  getColumns,
  type ArticleItem,
  type TagItem,
  type ColumnItem,
} from '@/api/blog'

const route = useRoute()
const router = useRouter()
const MAX_VISIBLE_TAGS = 5

const tags = ref<TagItem[]>([])
const carouselList = ref<ArticleItem[]>([])
const popularList = ref<ArticleItem[]>([])
const articleList = ref<ArticleItem[]>([])
const columns = ref<ColumnItem[]>([])
const subscribeEmail = ref('')
const listTab = ref('latest')

const currentTagId = computed(() => (route.query.tag as string) || '')

const visibleTags = computed(() => tags.value.slice(0, MAX_VISIBLE_TAGS))
const moreTags = computed(() => tags.value.slice(MAX_VISIBLE_TAGS))

function tagRoute(tag: TagItem) {
  return { path: '/blog', query: { tag: tag.id } }
}

function onTagCommand(tag: TagItem) {
  router.push(tagRoute(tag))
}

function formatDate(str: string) {
  if (!str) return ''
  const d = new Date(str)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function onSubscribe() {
  if (!subscribeEmail.value) return
  // 后续对接订阅接口
  subscribeEmail.value = ''
}

function onListSearch() {
  // 后续可打开搜索弹窗或跳转搜索页
  router.push({ path: '/blog', query: { ...route.query, search: '1' } })
}

function loadData() {
  getBlogTags().then((list) => { tags.value = list })
  getCarouselArticles().then((list) => { carouselList.value = list })
  getPopularArticles(4).then((list) => { popularList.value = list })
  getColumns(6).then((list) => { columns.value = list })
}

function loadArticleList() {
  getArticleList({ tagId: currentTagId.value || undefined, tab: listTab.value }).then((res) => {
    articleList.value = res.list
  })
}

const subNavWrapperRef = ref<HTMLElement | null>(null)
const subNavIndicatorStyle = ref({ left: '0px', width: '0px' })

function updateSubNavIndicator() {
  nextTick(() => {
    const wrapper = subNavWrapperRef.value
    const activeEl = wrapper?.querySelector('.sub-nav-item.active')
    if (!wrapper || !activeEl) {
      subNavIndicatorStyle.value = { left: '0px', width: '0px' }
      return
    }
    const rect = activeEl.getBoundingClientRect()
    const wrapperRect = wrapper.getBoundingClientRect()
    subNavIndicatorStyle.value = {
      left: `${rect.left - wrapperRect.left}px`,
      width: `${rect.width}px`,
    }
  })
}

watch([listTab, currentTagId], loadArticleList)
watch(currentTagId, updateSubNavIndicator)
onMounted(() => {
  loadData()
  loadArticleList()
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

/* 轮播：变大且居中 */
.carousel-section {
  width: 100%;
  max-width: 100%;
  margin-bottom: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: #2d3a3a;
}

.carousel-section :deep(.el-carousel),
.carousel-section :deep(.el-carousel__container) {
  height: 420px;
}

.carousel-slide {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 56px;
  gap: 40px;
}

.carousel-content {
  flex: 1;
  max-width: 480px;
}

.carousel-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 16px;
  line-height: 1.3;
}

.carousel-summary {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 24px;
  line-height: 1.6;
}

.carousel-read-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  color: #fff;
  border: 2px solid #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.carousel-read-btn:hover {
  background: #fff;
  color: #2d3a3a;
}

.carousel-cover {
  width: 200px;
  height: 260px;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.1);
}

.carousel-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

/* Most Popular */
.most-popular {
  margin-bottom: 0;
  padding-bottom: 0;
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

.list-search-btn {
  width: 40px;
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
  transition: color 0.2s, background 0.2s;
}

.list-search-btn:hover {
  color: #111;
  background: rgba(0, 0, 0, 0.06);
}

.list-search-btn .el-icon {
  font-size: 20px;
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

.article-item-meta {
  font-size: 14px;
  color: #888;
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

.subscribe-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.subscribe-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-initial {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.subscribe-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.subscribe-name {
  font-weight: 700;
  font-size: 15px;
  color: #111;
}

.subscribe-desc {
  font-size: 13px;
  color: #666;
}

.subscribe-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.subscribe-btn {
  width: 100%;
  background: #111 !important;
  border-color: #111 !important;
}

.unsubscribe-btn {
  font-size: 12px;
  color: #888;
  padding: 0;
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
