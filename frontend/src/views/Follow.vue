<template>
  <div class="follow-page">
    <div class="follow-layout">
      <main class="follow-main">
        <!-- 顶部：关注的人横向条 -->
        <section class="follow-people-bar">
          <div class="follow-people-inner">
            <button
              type="button"
              class="follow-people-item follow-people-all"
              :class="{ active: currentFeedId === 'all' }"
              @click="setFeedId('all')"
            >
              <span class="follow-people-avatar follow-people-avatar-all">
                <el-icon><List /></el-icon>
              </span>
              <span class="follow-people-name">全部动态</span>
            </button>
            <template v-if="followListLoading">
              <span class="follow-people-loading">加载中…</span>
            </template>
            <router-link
              v-for="user in followedUsers"
              :key="user.id"
              :to="`/follow?user=${user.id}`"
              class="follow-people-item"
              :class="{ active: currentFeedId === user.id }"
              @click.prevent="setFeedId(user.id)"
            >
              <span class="follow-people-avatar-wrap">
                <img v-if="user.avatar" :src="user.avatar" :alt="user.name" class="follow-people-avatar" />
                <span v-else class="follow-people-avatar follow-people-avatar-ph">{{ user.name.charAt(0) }}</span>
                <span v-if="user.hasNew" class="follow-people-dot"></span>
              </span>
              <span class="follow-people-name">{{ user.name }}</span>
            </router-link>
          </div>
        </section>

        <!-- 下方：动态流（关注的人发布的文章，按时间排序，触底加载更多） -->
        <section class="follow-feed">
          <div v-if="feedLoading" class="feed-loading">加载中…</div>
          <template v-else>
            <article
              v-for="item in feedListFiltered"
              :key="item.id"
              class="feed-card"
            >
              <div class="feed-card-inner">
                <div class="feed-card-main">
                  <div class="feed-header">
                    <span class="feed-action">{{ item.actorName }} {{ item.actionText }}</span>
                    <span class="feed-dot">·</span>
                    <span class="feed-time">{{ item.timeAgo }}</span>
                  </div>
                  <div class="feed-author">
                    <img v-if="item.authorAvatar" :src="item.authorAvatar" :alt="item.authorName" class="feed-author-avatar" />
                    <span v-else class="feed-author-avatar feed-author-avatar-ph">{{ (item.authorName || ' ').charAt(0) }}</span>
                    <div class="feed-author-info">
                      <span class="feed-author-name">{{ item.authorName }}</span>
                      <span v-if="item.authorDesc" class="feed-author-desc">{{ item.authorDesc }}</span>
                    </div>
                  </div>
                  <router-link :to="`/article/${item.contentId}`" class="feed-title">{{ item.title }}</router-link>
                  <p class="feed-excerpt">{{ item.excerpt }}</p>
                  <router-link :to="`/article/${item.contentId}`" class="feed-read-more">
                    阅读全文
                    <el-icon><ArrowDown /></el-icon>
                  </router-link>
                  <div class="feed-actions">
                    <span class="feed-action-btn">阅读 {{ item.viewCount }}</span>
                    <span class="feed-action-btn">
                      <el-icon><CaretTop /></el-icon>
                      点赞 {{ item.likeCount }}
                    </span>
                    <span class="feed-action-btn">
                      <el-icon><Star /></el-icon>
                      收藏 {{ item.collectionCount }}
                    </span>
                    <span class="feed-action-btn">
                      <el-icon><ChatDotRound /></el-icon>
                      评论 {{ item.commentCount }}
                    </span>
                  </div>
                </div>
                <div class="feed-card-cover">
                  <img v-if="item.cover" :src="item.cover" :alt="item.title" class="feed-cover-img" />
                  <span v-else class="feed-cover-placeholder">{{ (item.title || ' ').charAt(0) }}</span>
                </div>
              </div>
            </article>
            <div v-if="feedListFiltered.length === 0" class="feed-empty">
              <p class="feed-empty-text">暂无动态</p>
            </div>
            <div
              v-show="feedListFiltered.length > 0 && hasMoreFeed"
              ref="feedSentinelRef"
              class="feed-sentinel"
            />
            <div v-if="feedLoadingMore" class="feed-load-more">加载更多…</div>
            <p v-if="feedListFiltered.length > 0 && !hasMoreFeed && !feedLoading" class="feed-no-more">没有更多了</p>
          </template>
        </section>
      </main>
      <aside class="follow-sidebar-wrap">
        <div class="follow-sidebar-inner">
          <CreationCenter />
          <HotSearch />
          <RecommendedFollows />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { List, ArrowDown, CaretTop, ChatDotRound, Share, Star } from '@element-plus/icons-vue'
import { getFollowList } from '@/api/follow'
import { getUsersBatch } from '@/api/user'
import { getContentsList, type ContentListItem } from '@/api/content'
import CreationCenter from '@/components/CreationCenter.vue'
import HotSearch from '@/components/HotSearch.vue'
import RecommendedFollows from '@/components/RecommendedFollows.vue'

interface FollowedUser {
  id: string
  name: string
  avatar?: string
  hasNew?: boolean
}

interface FeedItem {
  id: string
  actorName: string
  actionText: string
  timeAgo: string
  authorName: string
  authorDesc?: string
  authorAvatar?: string
  title: string
  excerpt: string
  contentId: string
  viewCount: string
  likeCount: string
  collectionCount: string
  commentCount: number
  cover?: string
}

const FEED_PAGE_SIZE = 10

function formatTimeAgo(createdAt: string): string {
  if (!createdAt) return ''
  const d = new Date(createdAt)
  const now = Date.now()
  const diff = now - d.getTime()
  const min = 60000
  const hour = 60 * min
  const day = 24 * hour
  if (diff < min) return '刚刚'
  if (diff < hour) return Math.floor(diff / min) + ' 分钟前'
  if (diff < day) return Math.floor(diff / hour) + ' 小时前'
  if (diff < 2 * day) return '1 天前'
  if (diff < 7 * day) return Math.floor(diff / day) + ' 天前'
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const dayNum = d.getDate()
  return `${y}-${m}-${dayNum}`
}

function formatFeedCount(n: number): string {
  if (n == null || Number.isNaN(n)) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(n)
}

const route = useRoute()
const router = useRouter()
const currentFeedId = ref('all')
const followedUsers = ref<FollowedUser[]>([])
const followeeIds = ref<number[]>([])
const followListLoading = ref(false)

const feedList = ref<FeedItem[]>([])
const feedPage = ref(0)
const feedTotal = ref(0)
const feedLoading = ref(false)
const feedLoadingMore = ref(false)
const feedSentinelRef = ref<HTMLElement | null>(null)

const hasMoreFeed = computed(() => feedList.value.length < feedTotal.value)

function setFeedId(id: string) {
  currentFeedId.value = id
  if (id === 'all') {
    router.replace({ path: '/follow' })
  } else {
    router.replace({ path: '/follow', query: { user: id } })
  }
}

async function loadFollowList() {
  followListLoading.value = true
  try {
    const ids = await getFollowList()
    followeeIds.value = ids
    if (!ids.length) {
      followedUsers.value = []
      return
    }
    const users = await getUsersBatch(ids)
    followedUsers.value = users.map((u) => ({
      id: String(u.id),
      name: u.nickname || u.username || '用户',
      avatar: u.avatar,
      hasNew: false,
    }))
  } catch {
    followedUsers.value = []
    followeeIds.value = []
  } finally {
    followListLoading.value = false
  }
}

function getFeedUserIds(): number[] {
  if (currentFeedId.value === 'all') return followeeIds.value
  const id = Number(currentFeedId.value)
  if (!Number.isFinite(id)) return followeeIds.value
  return [id]
}

async function loadFeed(append: boolean) {
  const userIds = getFeedUserIds()
  if (!userIds.length) {
    if (!append) {
      feedList.value = []
      feedTotal.value = 0
      feedPage.value = 0
    }
    return
  }
  if (append) {
    if (feedLoadingMore.value || !hasMoreFeed.value) return
    feedLoadingMore.value = true
  } else {
    feedLoading.value = true
    feedPage.value = 0
    feedList.value = []
  }
  const page = append ? feedPage.value + 1 : 1
  try {
    const res = await getContentsList({
      userIds,
      page,
      pageSize: FEED_PAGE_SIZE,
      sortBy: 'time',
      order: 'desc',
    })
    const list = res.list || []
    const authorIds = [...new Set((list as ContentListItem[]).map((c) => c.userId).filter((id): id is number => id != null))]
    const authors = authorIds.length ? await getUsersBatch(authorIds) : []
    const authorMap = new Map(authors.map((u) => [u.id, u]))
    const items: FeedItem[] = list.map((c) => {
      const u = c.userId != null ? authorMap.get(c.userId) : undefined
      const followed = c.userId != null ? followedUsers.value.find((f) => f.id === String(c.userId)) : undefined
      const authorName = (u?.nickname || u?.username || followed?.name || '用户').trim() || '用户'
      const authorAvatar = u?.avatar ?? followed?.avatar
      const authorDesc = u?.intro ?? undefined
      return {
        id: String(c.id),
        actorName: authorName,
        actionText: '发表了文章',
        timeAgo: formatTimeAgo(c.createdAt || ''),
        authorName,
        authorDesc,
        authorAvatar: authorAvatar || undefined,
        title: c.title || '无标题',
        excerpt: c.summary ?? '',
        contentId: String(c.id),
        viewCount: formatFeedCount(c.viewCount ?? 0),
        likeCount: formatFeedCount(c.likeCount ?? 0),
        collectionCount: formatFeedCount(c.collectionCount ?? 0),
        commentCount: c.commentCount ?? 0,
        cover: c.cover ?? undefined,
      }
    })
    if (append) {
      feedList.value = [...feedList.value, ...items]
      feedPage.value = page
    } else {
      feedList.value = items
      feedPage.value = 1
    }
    feedTotal.value = res.total ?? 0
  } catch {
    if (!append) feedList.value = []
  } finally {
    feedLoading.value = false
    feedLoadingMore.value = false
  }
}

const feedListFiltered = computed(() => feedList.value)

watch(
  () => route.query.user,
  (user) => {
    if (user && typeof user === 'string') {
      currentFeedId.value = user
    } else {
      currentFeedId.value = 'all'
    }
  },
  { immediate: true }
)

watch(currentFeedId, () => {
  loadFeed(false)
})

onMounted(async () => {
  await loadFollowList()
  loadFeed(false)
})

let feedSentinelObserver: IntersectionObserver | null = null
watch(
  [feedSentinelRef, () => feedList.value.length, feedLoading],
  () => {
    const el = feedSentinelRef.value
    feedSentinelObserver?.disconnect()
    feedSentinelObserver = null
    if (!el || feedLoading.value) return
    feedSentinelObserver = new IntersectionObserver(
      (entries) => {
        if (!entries[0]?.isIntersecting || !hasMoreFeed.value || feedLoadingMore.value) return
        loadFeed(true)
      },
      { rootMargin: '100px', threshold: 0 }
    )
    feedSentinelObserver.observe(el)
  },
  { flush: 'post' }
)
onBeforeUnmount(() => {
  feedSentinelObserver?.disconnect()
  feedSentinelObserver = null
})
</script>

<style scoped>
.follow-page {
  min-height: calc(100vh - 64px);
  background: var(--el-bg-color-page, #f5f5f5);
}

.follow-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
}

.follow-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-top: 3px solid #BB1919;
  border-radius: 8px;
}

/* 顶部：关注的人横向条，不随滚动条滚动 */
.follow-people-bar {
  position: sticky;
  top: 64px;
  z-index: 100;
  background: #fff;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  overflow-x: auto;
  scrollbar-width: none;
}

.follow-people-bar::-webkit-scrollbar {
  display: none;
}

.follow-people-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: max-content;
}

.follow-people-loading {
  font-size: 13px;
  color: #888;
  padding: 0 8px;
}

.follow-people-item {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  background: none;
  border: none;
  padding: 0;
}

.follow-people-item:hover .follow-people-name,
.follow-people-item.active .follow-people-name {
  color: #111;
  font-weight: 600;
}

.follow-people-all .follow-people-avatar-all {
  background: transparent;
  border: 2px solid #BB1919;
  color: #BB1919;
}

.follow-people-item.active .follow-people-avatar-all,
.follow-people-item.active .follow-people-avatar,
.follow-people-item.active .follow-people-avatar-ph {
  border-color: #BB1919;
  box-shadow: 0 0 0 2px #BB1919;
}

.follow-people-avatar-wrap {
  position: relative;
  display: block;
}

.follow-people-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  object-fit: cover;
  border: 2px solid #e0e0e0;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.follow-people-avatar-all {
  width: 56px;
  height: 56px;
}

.follow-people-avatar-all .el-icon {
  font-size: 24px;
}

.follow-people-avatar-ph {
  background: #e8e8e8;
  color: #666;
  font-size: 20px;
  font-weight: 600;
}

.follow-people-dot {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 12px;
  height: 12px;
  background: #BB1919;
  border: 2px solid #fff;
  border-radius: 50%;
}

.follow-people-name {
  font-size: 13px;
  color: #555;
  text-align: center;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 动态流 */
.follow-feed {
  padding: 0 24px 32px;
}

.feed-loading,
.feed-empty {
  padding: 48px 0;
  text-align: center;
  color: #888;
}

.feed-empty-text {
  margin: 0;
  font-size: 14px;
}

.feed-sentinel {
  height: 1px;
  visibility: hidden;
  pointer-events: none;
}

.feed-load-more,
.feed-no-more {
  padding: 16px 0;
  text-align: center;
  font-size: 13px;
  color: #888;
  margin: 0;
}

.feed-card {
  padding: 24px 0;
  border-bottom: 1px solid #eee;
}

.feed-card:last-child {
  border-bottom: none;
}

.feed-card-inner {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.feed-card-main {
  flex: 1;
  min-width: 0;
  padding-top: 2px;
}

/* 缩略图与标题起对齐，高度覆盖标题+部分摘要，减少下方空洞感 */
.feed-card-cover {
  flex-shrink: 0;
  width: 200px;
  height: 140px;
  border-radius: 10px;
  overflow: hidden;
  background: #eee;
  border: 1px solid #eee;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-top: 80px;
}

.feed-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  vertical-align: middle;
}

.feed-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #aaa;
  background: linear-gradient(145deg, #eee 0%, #e5e5e5 100%);
}

.feed-header {
  font-size: 14px;
  color: #888;
  margin-bottom: 12px;
}

.feed-dot {
  margin: 0 6px;
  color: #bbb;
}

.feed-time {
  color: #888;
}

.feed-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.feed-author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
}

.feed-author-avatar-ph {
  background: #e8e8e8;
  color: #666;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feed-author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feed-author-name {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.feed-author-desc {
  font-size: 13px;
  color: #888;
}

.feed-title {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #111;
  margin-bottom: 8px;
  text-decoration: none;
  line-height: 1.4;
}

.feed-title:hover {
  color: #BB1919;
  text-decoration: underline;
  text-decoration-color: #BB1919;
}

.feed-excerpt {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-read-more {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #BB1919;
  text-decoration: none;
  margin-bottom: 16px;
}

.feed-read-more:hover {
  text-decoration: underline;
}

.feed-read-more .el-icon {
  font-size: 12px;
}

.feed-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #888;
}

.feed-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.feed-action-btn:hover {
  color: #111;
}

.feed-action-btn .el-icon {
  font-size: 14px;
}

.feed-action-agree {
  padding: 6px 12px;
  background: #f0f0f0;
  border-radius: 6px;
  color: #111;
  font-weight: 500;
}

.feed-action-agree:hover {
  background: #e5e5e5;
}

/* 右侧栏：整列 sticky，粘在顶栏下，不随滚动滑没 */
.follow-sidebar-wrap {
  flex-shrink: 0;
  align-self: flex-start;
  position: sticky;
  top: 64px;
  max-height: calc(100vh - 64px);
  overflow-y: auto;
}

.follow-sidebar-inner {
  /* 仅作为内容容器，sticky 在外层 wrap 上 */
  max-height: inherit;
}
</style>
