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
                  {{ item.heat }}万热度
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
        </section>
        <p class="hot-no-more">没有更多内容</p>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { Share } from '@element-plus/icons-vue'
import CreationCenter from '@/components/CreationCenter.vue'
import HotSearch from '@/components/HotSearch.vue'
import RecommendedFollows from '@/components/RecommendedFollows.vue'

interface HotListItem {
  id: string
  title: string
  summary?: string
  heat: number
  cover?: string
  isNew?: boolean
}

/** 顶部切换条用的数据：从 hotList 取前几条并加 tag */
const topBarList = ref<{ id: string; tag: string; title: string; cover?: string }[]>([
  { id: '1', tag: '热点', title: '如何评价国产刑侦剧《重案六组》?', cover: '' },
  { id: '2', tag: '思维', title: '像战略家一样思考（五维思考）', cover: '' },
  { id: '3', tag: '写作', title: 'This prompt will change how you write', cover: '' },
  { id: '4', tag: '技术', title: '云原生入门：从零到部署', cover: '' },
  { id: '5', tag: '文化', title: '美国最高法院裁定特朗普政府大规模关税政策违法', cover: '' },
])

const topBarIndex = ref(0)
const topBarItem = ref(topBarList.value[0] ?? null)

const hotList = ref<HotListItem[]>([
  { id: '1', title: '如何评价国产刑侦剧《重案六组》?', summary: '从剧本、表演到时代背景，多角度解读这部经典刑侦剧。', heat: 1155, isNew: false },
  { id: '2', title: '像战略家一样思考（五维思考）', summary: '当所有人把思考外包给机器时，思考本身就是你的竞争优势。', heat: 892, isNew: false },
  { id: '3', title: 'This prompt will change how you write', summary: 'A practical guide to communication and logic.', heat: 756, isNew: false },
  { id: '4', title: '云原生入门：从零到部署', summary: '容器、编排与可观测性。', heat: 634, isNew: true },
  { id: '5', title: 'Purpose & Profit – 发现你一生的事业', summary: '全书免费阅读。', heat: 521, isNew: false },
  { id: '6', title: '一天内理顺生活的办法', summary: '极简行动清单。', heat: 448, isNew: true },
  { id: '7', title: '多兴趣者如何不浪费你的天赋', summary: '跨领域成长的地图。', heat: 387, isNew: false },
  { id: '8', title: '如何清晰而有深度地表达自己', summary: '沟通与逻辑的实践指南。', heat: 312, isNew: false },
  { id: '9', title: 'Kubernetes 生产实践中的常见坑', summary: '从部署到可观测性，避坑指南。', heat: 286, isNew: false },
  { id: '10', title: '写作与表达：从想法到成稿', summary: '结构化写作与清晰表达的方法。', heat: 254, isNew: true },
  { id: '11', title: 'AI 辅助创作的真实体验', summary: '工具、边界与人的角色。', heat: 228, isNew: false },
  { id: '12', title: '开源项目参与入门', summary: '从提 issue 到第一次 PR。', heat: 205, isNew: false },
  { id: '13', title: '开发者效率工具推荐', summary: '终端、编辑器与协作工具。', heat: 188, isNew: false },
  { id: '14', title: '春节档票房与观影指南', summary: '今年春节档有哪些值得看。', heat: 172, isNew: true },
  { id: '15', title: '健康作息与高效工作', summary: '睡眠、运动与专注力。', heat: 156, isNew: false },
  { id: '16', title: '产品思维：从需求到方案', summary: '如何把问题拆解成可执行方案。', heat: 142, isNew: false },
  { id: '17', title: '远程协作的沟通技巧', summary: '异步沟通与会议效率。', heat: 128, isNew: false },
  { id: '18', title: '读书笔记的系统化方法', summary: '从划线到输出。', heat: 115, isNew: true },
  { id: '19', title: '个人知识管理实践', summary: '笔记、标签与检索。', heat: 103, isNew: false },
  { id: '20', title: '从零开始学 TypeScript', summary: '类型系统与工程实践。', heat: 92, isNew: false },
  { id: '21', title: '前端性能优化清单', summary: '首屏、交互与监控。', heat: 82, isNew: false },
  { id: '22', title: '后端 API 设计原则', summary: 'RESTful、版本与错误码。', heat: 73, isNew: true },
  { id: '23', title: '数据库索引与查询优化', summary: '何时建索引、如何看执行计划。', heat: 65, isNew: false },
  { id: '24', title: '微服务拆分的边界', summary: '领域驱动与团队边界。', heat: 58, isNew: false },
  { id: '25', title: '日志与链路追踪实践', summary: '可观测性入门。', heat: 52, isNew: false },
  { id: '26', title: '安全开发：常见漏洞与防护', summary: '注入、越权与敏感信息。', heat: 46, isNew: false },
  { id: '27', title: '技术选型的权衡', summary: '成熟度、生态与团队能力。', heat: 41, isNew: true },
  { id: '28', title: '技术博客的写作与传播', summary: '选题、结构与平台。', heat: 36, isNew: false },
  { id: '29', title: '面试准备与复盘', summary: '简历、项目与算法。', heat: 32, isNew: false },
  { id: '30', title: '职业发展中的几个关键选择', summary: '赛道、团队与成长。', heat: 28, isNew: false },
])

function onShare(item: HotListItem) {
  // 后续对接分享
  console.log('share', item.id)
}

const TOP_BAR_INTERVAL = 4000
let topBarTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  topBarItem.value = topBarList.value[0]
  topBarTimer = setInterval(() => {
    topBarIndex.value = (topBarIndex.value + 1) % topBarList.value.length
    topBarItem.value = topBarList.value[topBarIndex.value]
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
  font-size: 14px;
  color: #BB1919;
  font-weight: 600;
  flex-shrink: 0;
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
