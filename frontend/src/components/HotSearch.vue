<template>
  <aside class="hot-search">
    <div class="hot-search-header">
      <div class="hot-search-title-wrap">
        <el-icon class="hot-search-icon"><TrendCharts /></el-icon>
        <span class="hot-search-title">猜你想看</span>
      </div>
      <button type="button" class="hot-search-refresh" @click="refresh">
        <el-icon class="refresh-icon"><Refresh /></el-icon>
        <span>换一换</span>
      </button>
    </div>
    <div v-if="loading" class="hot-search-loading">加载中…</div>
    <ul v-else class="hot-search-list">
      <li
        v-for="(item, index) in displayList"
        :key="item.id"
        class="hot-search-item"
      >
        <span
          class="hot-search-rank"
          :class="{ 'rank-top': index < 3 }"
        >{{ index + 1 }}</span>
        <router-link :to="item.link" class="hot-search-link">
          {{ item.title }}
        </router-link>
        <span v-if="item.showNew" class="hot-search-tag tag-new">新</span>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TrendCharts, Refresh } from '@element-plus/icons-vue'
import { getHotList } from '@/api/content'
import { getMainTags } from '@/api/tag'

export interface HotSearchItem {
  id: string
  title: string
  showNew: boolean
  link: string
}

const displayList = ref<HotSearchItem[]>([])
const loading = ref(false)

/** 去掉标签名中括号及括号内内容 */
function stripParentheses(name: string): string {
  if (!name || typeof name !== 'string') return name
  return name
    .replace(/\s*\([^)]*\)/g, '')
    .replace(/\s*（[^）]*）/g, '')
    .trim() || name
}

/** 文章是否在 7 天内发布（对应热榜「新」） */
function isNewArticle(createdAt: string | undefined): boolean {
  if (!createdAt) return false
  const d = new Date(createdAt)
  const days = (Date.now() - d.getTime()) / (24 * 60 * 60 * 1000)
  return days <= 7
}

async function loadList() {
  loading.value = true
  try {
    const [mainTagsRes, hotRes] = await Promise.all([
      getMainTags(),
      getHotList({ page: 1, pageSize: 50 }),
    ])
    const mainNames = new Set((mainTagsRes || []).map((t) => t.name))
    const list = hotRes.list || []

    const tagMeta = new Map<string, { showNew: boolean; count: number }>()
    for (const c of list) {
      const tagNames = (c.tagNames || []).filter((n) => n && !mainNames.has(n))
      const articleNew = isNewArticle(c.publishedAt ?? c.createdAt)
      for (const name of tagNames) {
        const key = name.trim()
        if (!key) continue
        const cur = tagMeta.get(key)
        tagMeta.set(key, {
          showNew: (cur?.showNew ?? false) || articleNew,
          count: (cur?.count ?? 0) + 1,
        })
      }
    }

    const ordered = Array.from(tagMeta.entries())
      .map(([name, meta]) => ({ name, showNew: meta.showNew, count: meta.count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10)

    displayList.value = ordered.map((entry, i) => {
      const title = stripParentheses(entry.name)
      return {
        id: `tag-${i}-${entry.name}`,
        title,
        showNew: entry.showNew,
        link: `/search?q=${encodeURIComponent(title)}&type=all`,
      }
    })
  } catch {
    displayList.value = []
  } finally {
    loading.value = false
  }
}

function refresh() {
  const next = [...displayList.value]
  const first = next.shift()
  if (first) next.push(first)
  displayList.value = next
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
/* BBC 风格：白底、红色顶条、统一 #BB1919、清晰层级 */
.hot-search {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-top: 3px solid #BB1919;
  border-radius: 0;
  padding: 16px 0 12px;
  min-width: 280px;
  margin-top: 20px;
}

.hot-search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px 14px;
  border-bottom: 1px solid #e8e8e8;
}

.hot-search-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hot-search-icon {
  font-size: 18px;
  color: #BB1919;
}

.hot-search-title {
  font-size: 16px;
  font-weight: 700;
  color: #111;
  letter-spacing: 0.02em;
}

.hot-search-refresh {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  background: none;
  border: none;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.hot-search-refresh:hover {
  color: #000;
}

.refresh-icon {
  font-size: 14px;
}

.hot-search-list {
  list-style: none;
  margin: 0;
  padding: 0 20px;
}

.hot-search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  min-height: 44px;
}

.hot-search-item:last-child {
  border-bottom: none;
}

.hot-search-rank {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  background: #f0f0f0;
  border-radius: 2px;
}

.hot-search-rank.rank-top {
  background: #BB1919;
  color: #fff;
  border-radius: 2px;
}

.hot-search-link {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #111;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.hot-search-link:hover {
  color: #111;
  text-decoration: underline;
}

.hot-search-count {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
}

.hot-search-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 2px;
  line-height: 1.2;
}

.hot-search-loading {
  padding: 24px 20px;
  text-align: center;
  font-size: 14px;
  color: #888;
}

.hot-search-tag.tag-new {
  background: #BB1919;
  color: #fff;
}
</style>
