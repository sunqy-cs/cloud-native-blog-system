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
              @click="currentFeedId = 'all'"
            >
              <span class="follow-people-avatar follow-people-avatar-all">
                <el-icon><List /></el-icon>
              </span>
              <span class="follow-people-name">全部动态</span>
            </button>
            <router-link
              v-for="user in followedUsers"
              :key="user.id"
              :to="`/follow?user=${user.id}`"
              class="follow-people-item"
              :class="{ active: currentFeedId === user.id }"
              @click.prevent="currentFeedId = user.id"
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

        <!-- 下方：动态流（带时间，BBC 风格） -->
        <section class="follow-feed">
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
                  <span v-else class="feed-author-avatar feed-author-avatar-ph">{{ item.authorName.charAt(0) }}</span>
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
                  <span class="feed-action-btn feed-action-agree">
                    <el-icon><CaretTop /></el-icon>
                    已赞同 {{ item.agreeCount }}
                  </span>
                  <span class="feed-action-btn">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ item.commentCount }} 条评论
                  </span>
                  <span class="feed-action-btn">
                    <el-icon><Share /></el-icon>
                    分享
                  </span>
                  <span class="feed-action-btn">
                    <el-icon><Star /></el-icon>
                    收藏
                  </span>
                </div>
              </div>
              <div class="feed-card-cover">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" class="feed-cover-img" />
                <span v-else class="feed-cover-placeholder">{{ item.title.charAt(0) }}</span>
              </div>
            </div>
          </article>
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
import { ref, computed } from 'vue'
import { List, ArrowDown, CaretTop, ChatDotRound, Share, Star } from '@element-plus/icons-vue'
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
  agreeCount: string
  commentCount: number
  cover?: string
}

const currentFeedId = ref('all')

const followedUsers = ref<FollowedUser[]>([
  { id: '1', name: '马督工', hasNew: true },
  { id: '2', name: '摩的司机徐师傅', hasNew: true },
  { id: '3', name: '累渠', hasNew: false },
  { id: '4', name: '神秘园', hasNew: true },
  { id: '5', name: '水论文的程序猿', hasNew: true },
  { id: '6', name: '地上足球888', hasNew: false },
  { id: '7', name: '毕导', hasNew: true },
  { id: '8', name: 'LexBurr', hasNew: false },
])

const feedList = ref<FeedItem[]>([
  {
    id: '1',
    actorName: 'Goose',
    actionText: '赞同了文章',
    timeAgo: '15 小时前',
    authorName: '李新野',
    authorDesc: '全域网红，清华姚班毕业',
    title: '《人约》圣地巡礼攻略潮汕篇',
    excerpt: '鉴于春节期间很多人到潮汕旅游，在这里写一下《人约》潮汕的圣地巡礼攻略。如果还需要什么介绍地方可以随时留言。',
    contentId: '1',
    agreeCount: '667',
    commentCount: 152,
    cover: 'https://picsum.photos/seed/follow1/320/200',
  },
  {
    id: '2',
    actorName: 'Goose',
    actionText: '赞同了回答',
    timeAgo: '21 小时前',
    authorName: '南极有雨',
    title: '我曾经在知乎听某乎友说美国有分辨率达到1mm的卫星，这个消息是真的么？',
    excerpt: '是真的。小时候我参加过中日夏令营，当时日本的孩子每天要负重20公斤急行军100公里…',
    contentId: '2',
    agreeCount: '2万',
    commentCount: 1605,
    cover: 'https://picsum.photos/seed/follow2/320/200',
  },
  {
    id: '3',
    actorName: 'Tammy',
    actionText: '赞同了文章',
    timeAgo: '1 天前',
    authorName: '云原生实践者',
    authorDesc: '技术博客作者',
    title: 'Kubernetes 生产环境可观测性实践',
    excerpt: '从指标、日志到链路追踪，搭建一套可用的可观测性体系。',
    contentId: '3',
    agreeCount: '328',
    commentCount: 42,
    cover: 'https://picsum.photos/seed/follow3/320/200',
  },
  {
    id: '4',
    actorName: '马督工',
    actionText: '赞同了文章',
    timeAgo: '2 天前',
    authorName: '累渠',
    authorDesc: '专栏作者',
    title: '如何用系统思维分析热点事件',
    excerpt: '从多信源、时间线与利益相关方入手，建立基本分析框架。',
    contentId: '4',
    agreeCount: '1.2万',
    commentCount: 380,
    // 无 cover，展示首字占位
  },
])

const feedListFiltered = computed(() => {
  if (currentFeedId.value === 'all') return feedList.value
  const user = followedUsers.value.find((u) => u.id === currentFeedId.value)
  if (!user) return feedList.value
  return feedList.value.filter((item) => item.actorName === user.name)
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
  border: 2px solid transparent;
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
