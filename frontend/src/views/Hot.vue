<template>
  <div class="hot-page">
    <div class="hot-layout">
      <main class="hot-main">
        <!-- 排行榜顶部：红条 + 几秒切换一次的条 -->
        <div class="hot-top-bar">
          <router-link
            v-if="topBarItem"
            :to="`/article/${topBarItem.id}`"
            class="hot-top-bar-inner"
          >
            <span class="hot-top-bar-tag">{{ topBarItem.tag }}</span>
            <span class="hot-top-bar-title">{{ topBarItem.title }}</span>
          </router-link>
        </div>
        <section class="hot-list">
          <div v-if="hotLoading" class="hot-loading">加载中…</div>
          <template v-else>
          <router-link
            v-for="(item, index) in hotList"
            :key="item.id"
            :to="`/article/${item.id}`"
            class="hot-list-item"
          >
            <div class="hot-rank-wrap">
              <span
                class="hot-rank"
                :class="{
                  'rank-top': index < 3,
                  'rank-normal': index >= 3,
                }"
              >
                {{ index + 1 }}
              </span>
              <span v-if="item.isNew" class="hot-badge-new">新</span>
            </div>
            <div class="hot-item-body">
              <h3 class="hot-item-title">{{ item.title }}</h3>
              <p v-if="item.summary" class="hot-item-summary">{{ item.summary }}</p>
              <div class="hot-item-meta">
                <span class="hot-heat">
                  <span class="hot-heat-icon"></span>
                  {{ item.heatDisplay }}
                </span>
                <span class="hot-stats">
                  阅读 {{ item.viewDisplay }} · 点赞 {{ item.likeDisplay }} · 收藏 {{ item.collectionDisplay }} · 评论 {{ item.commentDisplay }}
                </span>
                <span class="hot-share" @click.prevent="onShare(item)">
                  <el-icon><Share /></el-icon>
                  分享
                </span>
              </div>
            </div>
            <div class="hot-item-cover">
              <img v-if="item.cover" :src="item.cover" :alt="item.title" />
              <span v-else class="hot-item-cover-ph">{{ item.title.charAt(0) }}</span>
            </div>
          </router-link>
          <p v-if="!hotLoading && hotList.length === 0" class="hot-empty">暂无热榜内容</p>
          </template>
        </section>
        <p v-if="!hotLoading && hotList.length > 0" class="hot-no-more">没有更多内容</p>
      </main>
      <aside class="hot-sidebar-wrap">
        <div class="hot-sidebar-inner">
          <CreationCenter />
          <HotSearch />
          <RecommendedFollows />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Share } from '@element-plus/icons-vue'
import { getHotList } from '@/api/content'
import CreationCenter from '@/components/CreationCenter.vue'
import HotSearch from '@/components/HotSearch.vue'
import RecommendedFollows from '@/components/RecommendedFollows.vue'

interface HotListItem {
  id: string
  title: string
  summary?: string
  /** 主标签（博客第一个标签，用于顶部条展示） */
  mainTag: string
  heatDisplay: string
  viewDisplay: string
  likeDisplay: string
  collectionDisplay: string
  commentDisplay: string
  cover?: string
  isNew?: boolean
}

function formatHeatDisplay(hotScore: number | undefined): string {
  if (hotScore == null || Number.isNaN(hotScore)) return '0 热度'
  if (hotScore >= 10000) return (hotScore / 10000).toFixed(1).replace(/\.0$/, '') + '万热度'
  return hotScore.toFixed(0) + ' 热度'
}

function formatCount(n: number | undefined): string {
  if (n == null || Number.isNaN(n)) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(n)
}

/** 去掉标签名中括号及括号内内容（与推荐页导航一致） */
function stripParentheses(name: string): string {
  if (!name || typeof name !== 'string') return name
  return name
    .replace(/\s*\([^)]*\)/g, '')
    .replace(/\s*（[^）]*）/g, '')
    .trim() || name
}

function isNewArticle(createdAt: string | undefined): boolean {
  if (!createdAt) return false
  const d = new Date(createdAt)
  const now = Date.now()
  const days = (now - d.getTime()) / (24 * 60 * 60 * 1000)
  return days <= 7
}

const hotList = ref<HotListItem[]>([])
const hotLoading = ref(false)

/** 顶部切换条：取热榜前 5 条，tag 用该条目的主标签 */
const topBarList = computed(() =>
  hotList.value.slice(0, 5).map((item) => ({
    id: item.id,
    tag: item.mainTag || '热',
    title: item.title,
    cover: item.cover,
  }))
)

const topBarIndex = ref(0)
const topBarItem = computed(() => topBarList.value[topBarIndex.value] ?? null)

async function loadHot() {
  hotLoading.value = true
  try {
    const res = await getHotList({ page: 1, pageSize: 50 })
    const list = res.list ?? []
    hotList.value = list.map((c) => ({
      id: String(c.id),
      title: c.title || '无标题',
      summary: c.summary ?? '',
      mainTag: stripParentheses((c.tagNames && c.tagNames[0]) ? c.tagNames[0] : '') || '热',
      heatDisplay: formatHeatDisplay(c.hotScore),
      viewDisplay: formatCount(c.viewCount),
      likeDisplay: formatCount(c.likeCount),
      collectionDisplay: formatCount(c.collectionCount),
      commentDisplay: formatCount(c.commentCount ?? 0),
      cover: c.cover ?? undefined,
      isNew: isNewArticle(c.createdAt),
    }))
  } catch {
    hotList.value = []
  } finally {
    hotLoading.value = false
  }
}

function onShare(item: HotListItem) {
  console.log('share', item.id)
}

const TOP_BAR_INTERVAL = 4000
let topBarTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadHot()
  topBarTimer = setInterval(() => {
    if (topBarList.value.length === 0) return
    topBarIndex.value = (topBarIndex.value + 1) % topBarList.value.length
  }, TOP_BAR_INTERVAL)
})

onUnmounted(() => {
  if (topBarTimer) clearInterval(topBarTimer)
})
</script>

<style scoped>
.hot-page {
  min-height: calc(100vh - 64px);
  background: var(--el-bg-color-page, #f5f5f5);
}

/* 主布局 */
.hot-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
}

.hot-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-top: 3px solid #BB1919;
  border-radius: 8px;
  padding: 0 0 32px;
}

/* 排行榜顶部：红条 + 切换条，不随滚动条滚动 */
.hot-top-bar {
  position: sticky;
  top: 64px;
  z-index: 100;
  background: #fff;
  padding: 16px 24px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.hot-top-bar-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  text-decoration: none;
  color: inherit;
  min-height: 56px;
}

.hot-top-bar-inner:hover .hot-top-bar-title {
  color: #BB1919;
  text-decoration: underline;
}

.hot-top-bar-tag {
  font-size: 16px;
  font-weight: 500;
  color: #BB1919;
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 4px;
  background-color: rgba(187, 25, 25, 0.1);
}

.hot-top-bar-title {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 700;
  color: #111;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 热榜列表 */
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.hot-list-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  text-decoration: none;
  color: inherit;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}

.hot-list-item:hover {
  background: rgba(0, 0, 0, 0.02);
}

.hot-rank {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  border-radius: 4px;
}

.hot-rank.rank-top {
  background: #BB1919;
  color: #fff;
}

.hot-rank.rank-normal {
  background: #f0f0f0;
  color: #666;
}

.hot-rank-wrap {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 36px;
}

.hot-badge-new {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 5px;
  background: #BB1919;
  color: #fff;
  border-radius: 2px;
}

.hot-item-body {
  flex: 1;
  min-width: 0;
}

.hot-item-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 8px;
  line-height: 1.4;
  color: #111;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-item-summary {
  font-size: 14px;
  color: #555;
  margin: 0 0 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #888;
}

.hot-stats {
  flex: 1;
  font-size: 13px;
  color: #888;
  margin: 0 8px;
}

.hot-heat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #666;
  font-weight: 500;
  font-size: 13px;
}

.hot-heat-icon {
  width: 12px;
  height: 12px;
  background: #BB1919;
  border-radius: 50%;
}

.hot-share {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #888;
}

.hot-share:hover {
  color: #111;
}

.hot-share .el-icon {
  font-size: 14px;
}

.hot-item-cover {
  flex-shrink: 0;
  width: 140px;
  height: 94px;
  border-radius: 6px;
  overflow: hidden;
  background: #e8e8e8;
  align-self: center;
}

.hot-item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hot-item-cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #bbb;
}

.hot-loading,
.hot-empty {
  padding: 48px 0;
  text-align: center;
  color: #888;
  font-size: 14px;
}

.hot-no-more {
  text-align: center;
  font-size: 13px;
  color: #bbb;
  margin: 24px 0 0;
  padding: 0;
}

/* 右侧栏：整列 sticky，粘在顶栏下，不随滚动滑没 */
.hot-sidebar-wrap {
  flex-shrink: 0;
  align-self: flex-start;
  position: sticky;
  top: 64px;
  max-height: calc(100vh - 64px);
  overflow-y: auto;
}

.hot-sidebar-inner {
  max-height: inherit;
}
</style>
