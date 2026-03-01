<template>
  <div class="search-page">
    <div class="search-layout">
      <main class="search-main">
        <!-- 搜索分类导航：综合、专栏、用户 -->
        <nav class="search-nav">
          <button
            type="button"
            class="search-nav-item"
            :class="{ active: searchType === 'all' }"
            @click="setSearchType('all')"
          >
            综合
          </button>
          <button
            type="button"
            class="search-nav-item"
            :class="{ active: searchType === 'column' }"
            @click="setSearchType('column')"
          >
            专栏
          </button>
          <button
            type="button"
            class="search-nav-item"
            :class="{ active: searchType === 'user' }"
            @click="setSearchType('user')"
          >
            用户
          </button>
          <!-- 综合时显示筛选：可折叠，排序方式一行、时间一行 -->
          <template v-if="searchType === 'all'">
            <button
              type="button"
              class="search-nav-filter-trigger"
              @click="filterExpanded = !filterExpanded"
            >
              <el-icon class="search-nav-filter-icon"><Filter /></el-icon>
              <span>筛选</span>
              <el-icon class="search-nav-filter-arrow" :class="{ expanded: filterExpanded }">
                <ArrowDown />
              </el-icon>
            </button>
            <div v-show="filterExpanded" class="search-nav-filters">
              <div class="search-filter-row">
                <span class="search-filter-group-label">排序方式</span>
                <div class="search-filter-chips">
                  <button
                    v-for="opt in sortOptions"
                    :key="opt.value"
                    type="button"
                    class="search-filter-chip"
                    :class="{ active: sortBy === opt.value }"
                    @click="sortBy = opt.value; applyQuery()"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              </div>
              <div class="search-filter-row">
                <span class="search-filter-group-label">时间</span>
                <div class="search-filter-chips">
                  <button
                    v-for="opt in timeOptions"
                    :key="opt.value"
                    type="button"
                    class="search-filter-chip"
                    :class="{ active: timeRange === opt.value }"
                    @click="timeRange = opt.value; applyQuery()"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              </div>
            </div>
          </template>
        </nav>

        <div class="search-card">
          <p v-if="keyword.trim()" class="search-keyword-hint">搜索「{{ keyword.trim() }}」</p>

          <!-- 专栏搜索结果 -->
          <template v-if="searchType === 'column'">
            <div v-if="searchLoading" class="search-loading">加载中…</div>
            <div v-else-if="keyword.trim() && columnResults.length === 0" class="search-empty">暂无匹配专栏</div>
            <ul v-else-if="keyword.trim() && columnResults.length > 0" class="search-result-list search-result-columns">
              <li v-for="col in columnResults" :key="col.id" class="search-result-item">
                <router-link
                  :to="col.userId ? { path: '/blog', query: { userId: col.userId, columnId: col.id } } : { path: '/blog' }"
                  class="search-result-link search-result-column-link"
                >
                  <span class="search-result-column-cover">
                    <img v-if="col.cover" :src="col.cover" :alt="col.name" />
                    <span v-else class="search-result-cover-ph">{{ col.name.charAt(0) }}</span>
                  </span>
                  <span class="search-result-column-body">
                    <span class="search-result-title">
                      <span v-for="(frag, i) in highlightFragments(col.name, keyword.trim())" :key="i">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                    <span v-if="col.description" class="search-result-desc">
                      <span v-for="(frag, i) in highlightFragments(col.description, keyword.trim())" :key="i">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                    <span class="search-result-meta">
                      {{ col.articleCount }} 篇
                      <template v-if="col.updatedAt"> · 更新于 {{ col.updatedAt }}</template>
                      · 进入专栏
                    </span>
                  </span>
                </router-link>
              </li>
            </ul>
            <p v-else class="search-placeholder-hint">输入关键词搜索专栏名称或描述</p>
          </template>

          <!-- 用户搜索结果 -->
          <template v-else-if="searchType === 'user'">
            <div v-if="searchLoading" class="search-loading">加载中…</div>
            <div v-else-if="keyword.trim() && userResults.length === 0" class="search-empty">暂无匹配用户</div>
            <ul v-else-if="keyword.trim() && userResults.length > 0" class="search-result-list search-result-users">
              <li v-for="u in userResults" :key="u.id" class="search-result-item">
                <router-link :to="{ path: '/blog', query: { userId: u.id } }" class="search-result-link search-result-user-link">
                  <span class="search-result-avatar">
                    <img v-if="u.avatar" :src="u.avatar" :alt="u.nickname || u.username" />
                    <span v-else class="search-result-avatar-ph">{{ (u.nickname || u.username || '?').charAt(0) }}</span>
                  </span>
                  <span class="search-result-user-info">
                    <span class="search-result-title">
                      <span v-for="(frag, i) in highlightFragments(u.nickname || u.username || '', keyword.trim())" :key="i">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                    <span v-if="u.intro" class="search-result-desc">
                      <span v-for="(frag, i) in highlightFragments(u.intro, keyword.trim())" :key="i">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                  </span>
                </router-link>
              </li>
            </ul>
            <p v-else class="search-placeholder-hint">输入关键词搜索用户昵称或用户名</p>
          </template>

          <!-- 综合：文章搜索（标题、摘要、全文、标签） -->
          <template v-else>
            <div v-if="searchLoading" class="search-loading">加载中…</div>
            <div v-else-if="keyword.trim() && articleResults.length === 0" class="search-empty">暂无匹配文章</div>
            <ul v-else-if="keyword.trim() && articleResults.length > 0" class="search-result-list search-result-articles">
              <li v-for="art in articleResults" :key="art.id" class="search-result-item">
                <router-link :to="`/article/${art.id}`" class="search-result-link search-result-article-link">
                  <span v-if="art.cover" class="search-result-article-cover">
                    <img :src="art.cover" :alt="art.title" />
                  </span>
                  <span class="search-result-article-body">
                    <span class="search-result-title">
                      <span v-for="(frag, i) in highlightFragments(art.title, keyword.trim())" :key="i">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                    <span v-if="art.summary" class="search-result-desc">
                      <span v-for="(frag, i) in highlightFragments(art.summary, keyword.trim())" :key="i">
                        <span v-if="frag.type === 'match'" class="search-highlight">{{ frag.value }}</span>
                        <template v-else>{{ frag.value }}</template>
                      </span>
                    </span>
                    <span class="search-result-meta">
                      阅读 {{ art.viewCount ?? 0 }} · 点赞 {{ art.likeCount ?? 0 }} · 收藏 {{ art.collectionCount ?? 0 }} · 评论 {{ art.commentCount ?? 0 }}
                      <template v-if="art.publishedAt || art.createdAt"> · {{ formatDateTime(art.publishedAt ?? art.createdAt) }}</template>
                    </span>
                  </span>
                </router-link>
              </li>
            </ul>
            <p v-else class="search-placeholder-hint">在顶栏输入关键词，搜索文章标题、摘要、正文与标签。</p>
          </template>
        </div>
      </main>
      <aside class="search-sidebar-wrap">
        <div class="search-sidebar-inner">
          <CreationCenter />
          <HotSearch />
          <RecommendedFollows />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Filter, ArrowDown } from '@element-plus/icons-vue'
import { searchColumns } from '@/api/column'
import { searchUsers } from '@/api/user'
import { searchContents } from '@/api/content'
import CreationCenter from '@/components/CreationCenter.vue'
import HotSearch from '@/components/HotSearch.vue'
import RecommendedFollows from '@/components/RecommendedFollows.vue'
import type { ColumnItem } from '@/api/column'
import type { UserMe } from '@/api/user'
import type { ContentListItem } from '@/api/content'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const columnResults = ref<ColumnItem[]>([])
const userResults = ref<UserMe[]>([])
const articleResults = ref<ContentListItem[]>([])
const searchLoading = ref(false)

type SearchType = 'all' | 'column' | 'user'
const searchType = ref<SearchType>('all')

const sortOptions = [
  { value: 'comprehensive', label: '综合排序' },
  { value: 'likes', label: '最多赞同' },
  { value: 'newest', label: '最新发布' },
]
const timeOptions = [
  { value: 'all', label: '不限时间' },
  { value: '1d', label: '一天内' },
  { value: '1w', label: '一周内' },
  { value: '1m', label: '一月内' },
  { value: '3m', label: '三月内' },
  { value: '6m', label: '半年内' },
  { value: '1y', label: '一年内' },
]

const sortBy = ref('comprehensive')
const timeRange = ref('all')
const filterExpanded = ref(false)

/** 时间显示：去掉 ISO 里的 T，改为空格 */
function formatDateTime(iso: string) {
  if (!iso) return ''
  return iso.replace('T', ' ')
}

/** 将文本按关键词拆成片段用于淡红色高亮（不区分大小写） */
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

function setSearchType(type: SearchType) {
  searchType.value = type
  applyQuery()
}

function applyQuery() {
  const q = route.query.q
  const query: Record<string, string> = typeof q === 'string' ? { q } : {}
  query.type = searchType.value
  if (searchType.value === 'all') {
    query.sort = sortBy.value
    query.time = timeRange.value
  }
  router.replace({ path: '/search', query })
}

function syncFromQuery() {
  const t = route.query.type
  if (t === 'column' || t === 'user') searchType.value = t
  else searchType.value = 'all'
  const s = Array.isArray(route.query.sort) ? route.query.sort[0] : route.query.sort
  if (s && sortOptions.some((o) => o.value === s)) sortBy.value = s
  const time = Array.isArray(route.query.time) ? route.query.time[0] : route.query.time
  if (time && timeOptions.some((o) => o.value === time)) timeRange.value = time
}

async function doSearch() {
  const q = keyword.value.trim()
  const type = searchType.value
  if (!q) {
    columnResults.value = []
    userResults.value = []
    articleResults.value = []
    return
  }
  searchLoading.value = true
  columnResults.value = []
  userResults.value = []
  articleResults.value = []
  try {
    if (type === 'column') {
      columnResults.value = await searchColumns(q)
    } else if (type === 'user') {
      userResults.value = await searchUsers(q)
    } else {
      articleResults.value = await searchContents({
        q,
        page: 1,
        pageSize: 20,
        sort: sortBy.value,
        time: timeRange.value,
      })
    }
  } finally {
    searchLoading.value = false
  }
}

watch(
  () => route.query.q,
  (q) => {
    keyword.value = typeof q === 'string' ? q : ''
  },
  { immediate: true }
)

watch(
  () => [route.query.type, route.query.sort, route.query.time],
  () => syncFromQuery(),
  { immediate: true }
)

watch(
  () => [keyword.value, searchType.value, sortBy.value, timeRange.value],
  () => {
    if (searchType.value === 'all' || searchType.value === 'column' || searchType.value === 'user') doSearch()
    else {
      columnResults.value = []
      userResults.value = []
      articleResults.value = []
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
  display: flex;
  gap: 24px;
}

.search-main {
  flex: 1;
  min-width: 0;
}

.search-nav {
  background: #fff;
  border-radius: 12px 12px 0 0;
  padding: 0 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  min-height: 48px;
}

.search-nav-item {
  padding: 14px 0;
  margin: 0 8px 0 0;
  font-size: 15px;
  color: #666;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}

.search-nav-item:hover {
  color: #1a1a1a;
}

.search-nav-item.active {
  font-weight: 600;
  color: #BB1919;
  border-bottom-color: #BB1919;
}

.search-nav-filter-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
  padding: 8px 12px;
  font-size: 14px;
  color: #666;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.search-nav-filter-trigger:hover {
  color: #1a1a1a;
  background: #f5f5f5;
}

.search-nav-filter-icon {
  font-size: 16px;
}

.search-nav-filter-arrow {
  font-size: 14px;
  transition: transform 0.2s;
}

.search-nav-filter-arrow.expanded {
  transform: rotate(180deg);
}

.search-nav-filters {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  padding: 12px 0 16px;
}

.search-filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.search-filter-group-label {
  font-size: 13px;
  color: #999;
  flex-shrink: 0;
  width: 56px;
}

.search-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-filter-chip {
  padding: 6px 12px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.search-filter-chip:hover {
  background: #eee;
  color: #1a1a1a;
}

.search-filter-chip.active {
  background: #fff0f0;
  color: #BB1919;
}

.search-card {
  background: #fff;
  border-radius: 0 0 12px 12px;
  padding: 24px 24px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.search-keyword-hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;
}

/* 搜索词高亮：淡淡红色 */
.search-highlight {
  background: rgba(187, 25, 25, 0.14);
  padding: 0 2px;
  border-radius: 2px;
}

.search-placeholder-icon-wrap {
  margin-bottom: 16px;
  text-align: center;
}

.search-placeholder-icon {
  font-size: 48px;
  color: #c9c9c9;
}

.search-loading,
.search-empty {
  padding: 32px 0;
  text-align: center;
  font-size: 14px;
  color: #999;
}

.search-result-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.search-result-item {
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-link {
  display: block;
  padding: 14px 0;
  color: #1a1a1a;
  text-decoration: none;
}

.search-result-link:hover {
  color: #BB1919;
}

.search-result-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.search-result-desc {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-meta {
  font-size: 12px;
  color: #999;
}

.search-result-columns .search-result-item {
  padding: 0;
}

.search-result-column-link {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 0;
}

.search-result-column-cover {
  flex-shrink: 0;
  width: 100px;
  height: 70px;
  border-radius: 8px;
  overflow: hidden;
  background: #e8e8e8;
}

.search-result-column-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.search-result-cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 20px;
  font-weight: 600;
  color: #999;
}

.search-result-column-body {
  flex: 1;
  min-width: 0;
}

.search-result-column-body .search-result-desc {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.search-result-articles .search-result-item {
  padding: 0;
}

.search-result-article-link {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 0;
}

.search-result-article-cover {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #e8e8e8;
}

.search-result-article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.search-result-article-body {
  flex: 1;
  min-width: 0;
}

.search-result-article-body .search-result-desc {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.search-result-users .search-result-item {
  padding: 0;
}

.search-result-user-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.search-result-avatar {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: #e8e8e8;
}

.search-result-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.search-result-avatar-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 18px;
  font-weight: 600;
  color: #666;
}

.search-result-user-info {
  flex: 1;
  min-width: 0;
}

.search-result-user-info .search-result-desc {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.search-placeholder-input-wrap {
  display: flex;
  max-width: 480px;
  margin: 0 auto 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.search-placeholder-input {
  flex: 1;
  padding: 12px 16px;
  font-size: 15px;
  border: none;
  outline: none;
}

.search-placeholder-input::placeholder {
  color: #999;
}

.search-placeholder-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: #1a1a1a;
  color: #fff;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.search-placeholder-btn:hover {
  background: #333;
}

.search-placeholder-hint {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.search-sidebar-wrap {
  width: 280px;
  flex-shrink: 0;
}

.search-sidebar-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 900px) {
  .search-sidebar-wrap {
    display: none;
  }
}
</style>
