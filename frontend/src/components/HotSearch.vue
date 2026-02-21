<template>
  <aside class="hot-search">
    <div class="hot-search-header">
      <div class="hot-search-title-wrap">
        <el-icon class="hot-search-icon"><TrendCharts /></el-icon>
        <span class="hot-search-title">大家都在搜</span>
      </div>
      <button type="button" class="hot-search-refresh" @click="refresh">
        <el-icon class="refresh-icon"><Refresh /></el-icon>
        <span>换一换</span>
      </button>
    </div>
    <ul class="hot-search-list">
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
        <span class="hot-search-count">{{ item.count }}</span>
        <span
          v-if="item.tag"
          class="hot-search-tag"
          :class="item.tag === '热' ? 'tag-hot' : 'tag-new'"
        >
          {{ item.tag }}
        </span>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TrendCharts, Refresh } from '@element-plus/icons-vue'

export interface HotSearchItem {
  id: string
  title: string
  count: string
  tag?: '热' | '新'
  link?: string
}

const displayList = ref<HotSearchItem[]>([])

const mockList: HotSearchItem[] = [
  { id: '1', title: '接财神', count: '312万', tag: '热', link: '/hot' },
  { id: '2', title: '飞驰人生3', count: '305万', tag: '热', link: '/hot' },
  { id: '3', title: '中国队短道速滑男子接力', count: '298万', tag: '热', link: '/hot' },
  { id: '4', title: '春节档票房', count: '256万', tag: '新', link: '/hot' },
  { id: '5', title: '云原生技术实践', count: '188万', tag: '热', link: '/blog' },
  { id: '6', title: 'Kubernetes 入门', count: '165万', tag: '新', link: '/blog' },
  { id: '7', title: '写作与表达', count: '142万', tag: '热', link: '/blog' },
  { id: '8', title: 'AI 与创作', count: '128万', tag: '新', link: '/blog' },
  { id: '9', title: '开源项目推荐', count: '98万', link: '/recommend' },
  { id: '10', title: '开发者工具', count: '86万', tag: '热', link: '/knowledge' },
]

function refresh() {
  // 简单轮换：把列表循环移位
  const next = [...displayList.value]
  const first = next.shift()
  if (first) next.push(first)
  displayList.value = next
}

onMounted(() => {
  displayList.value = [...mockList]
})
</script>

<style scoped>
/* BBC 风格：白底、黑顶条、克制用色、清晰层级 */
.hot-search {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-top: 3px solid #000;
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
  color: #c00;
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
  background: #c00;
  color: #fff;
  border-radius: 2px;
}

.hot-search-link {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #333;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.hot-search-link:hover {
  color: #000;
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

.hot-search-tag.tag-hot {
  background: #c00;
  color: #fff;
}

.hot-search-tag.tag-new {
  background: #1474b8;
  color: #fff;
}
</style>
