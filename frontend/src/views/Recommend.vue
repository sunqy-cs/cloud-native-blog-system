<template>
  <div class="recommend-page">
    <!-- 顶部：推荐标签栏，tag 居中，最右为换一批 icon -->
    <nav class="rec-tag-bar">
      <div class="rec-tag-bar-inner">
        <div class="rec-tag-bar-left"></div>
        <div class="rec-tag-bar-center" ref="recTagBarCenterRef">
          <router-link
            v-for="tag in recTags"
            :key="tag.id"
            :to="{ path: '/recommend', query: { tag: tag.id } }"
            class="rec-tag-item"
            :class="{ active: currentTagId === tag.id }"
          >
            {{ tag.name }}
          </router-link>
          <div class="rec-tag-indicator" :style="recTagIndicatorStyle"></div>
        </div>
        <button type="button" class="rec-tag-refresh" title="换一批" @click="refreshTags">
          <el-icon><Refresh /></el-icon>
        </button>
      </div>
    </nav>

    <div class="page-layout">
      <main class="rec-main">
        <!-- BBC 三栏：左 2 条带图 | 中 1 条大头条+相关链接 | 右 4 条纯文 -->
        <div class="bbc-grid">
          <div class="bbc-col-left">
            <router-link
              v-for="item in (leftArticles || [])"
              :key="item.id"
              :to="`/article/${item.id}`"
              class="bbc-card bbc-card-left"
            >
              <div class="bbc-card-img">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                <span v-else class="bbc-card-img-ph">{{ item.title.charAt(0) }}</span>
              </div>
              <h3 class="bbc-card-headline">{{ item.title }}</h3>
              <p class="bbc-card-desc">{{ item.summary }}</p>
              <div class="bbc-card-meta">{{ item.meta }}</div>
            </router-link>
          </div>
          <div class="bbc-col-center" v-if="featureArticle">
            <router-link :to="`/article/${featureArticle.id}`" class="bbc-feature">
              <div class="bbc-feature-img">
                <img v-if="featureArticle.cover" :src="featureArticle.cover" :alt="featureArticle.title" />
                <span v-else class="bbc-card-img-ph">{{ featureArticle.title.charAt(0) }}</span>
              </div>
              <h2 class="bbc-feature-headline">{{ featureArticle.title }}</h2>
              <p class="bbc-feature-desc">{{ featureArticle.summary }}</p>
              <div class="bbc-feature-meta">{{ featureArticle.meta }}</div>
            </router-link>
            <ul class="bbc-related">
              <li v-for="(link, i) in (relatedLinks || [])" :key="i">
                <router-link :to="link.to">
                  <el-icon class="bbc-related-icon"><VideoPlay /></el-icon>
                  {{ link.title }}
                </router-link>
              </li>
            </ul>
          </div>
          <div class="bbc-col-right">
            <router-link
              v-for="item in (rightArticles || [])"
              :key="item.id"
              :to="`/article/${item.id}`"
              class="bbc-card bbc-card-right"
            >
              <h3 class="bbc-card-headline">{{ item.title }}</h3>
              <p class="bbc-card-desc">{{ item.summary }}</p>
              <div class="bbc-card-meta">{{ item.meta }}</div>
            </router-link>
          </div>
        </div>

        <div class="rec-section-divider"></div>

        <!-- 第一次：2 条，WEEKEND READS 风格 -->
        <RecommendBlock
          title="WEEKEND READS"
          variant="two"
          :items="weekendReads"
        />

        <div class="rec-section-divider"></div>

        <!-- 第二次：4 条，WINTER OLYMPICS 风格 -->
        <RecommendBlock
          title="WINTER OLYMPICS >"
          title-link="/recommend?tab=olympics"
          variant="four"
          :items="winterOlympics"
        />

        <div class="rec-section-divider"></div>

        <!-- 第三次：多条 + 按钮滚动，BEST AUDIO OF THE WEEK 风格 -->
        <RecommendBlock
          title="BEST AUDIO OF THE WEEK >"
          title-link="/recommend?tab=audio"
          variant="strip"
          :items="bestAudioItems"
        />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, VideoPlay } from '@element-plus/icons-vue'
import RecommendBlock from '@/components/RecommendBlock.vue'

interface RecTag {
  id: string
  name: string
}

interface BbcItem {
  id: string
  title: string
  subtitle?: string
  summary?: string
  meta?: string
  cover?: string
  link?: string
  label?: string
}

const route = useRoute()
const recTags = ref<RecTag[]>([])
const currentTagId = computed(() => (route.query.tag as string) || '')

const allTagPool: RecTag[] = [
  { id: '1', name: 'Home' },
  { id: '2', name: '技术' },
  { id: '3', name: '云原生' },
  { id: '4', name: 'Kubernetes' },
  { id: '5', name: '写作' },
  { id: '6', name: '产品' },
  { id: '7', name: 'AI' },
  { id: '8', name: '开源' },
  { id: '9', name: '商业' },
  { id: '10', name: '健康' },
  { id: '11', name: '文化' },
  { id: '12', name: '旅行' },
]

function pickTags(count: number) {
  const shuffled = [...allTagPool].sort(() => Math.random() - 0.5)
  return shuffled.slice(0, count)
}

function refreshTags() {
  recTags.value = pickTags(10)
}

interface RecArticle {
  id: string
  title: string
  summary: string
  meta: string
  cover?: string
}

const leftArticles = ref<RecArticle[]>([
  { id: '1', title: 'Trump lashes out at US Supreme Court justices over tariffs ruling', summary: 'The six justices who voted against the tariffs, dealing a major blow to his signature economic policy, should be \'absolutely ashamed\', Trump said.', meta: '51 mins ago | US & Canada' },
  { id: '2', title: 'UK government considers removing Andrew from royal line of succession', summary: 'The former Duke of York is eighth in line to the throne, meaning he remains eligible to be King.', meta: '6 hrs ago | Politics' },
])

const featureArticle = ref<RecArticle>({
  id: '3',
  title: 'Trump brings in new 10% tariff as Supreme Court rejects his global import taxes',
  summary: 'The US Supreme Court\'s decision, striking down some of Trump\'s most sweeping tariffs, injects new uncertainty into global trade.',
  meta: '1 hr ago | Business',
})

const relatedLinks = ref<{ title: string; to: string }[]>([
  { title: 'Canada looks to trade talks after US Supreme Court tosses Trump\'s tariffs', to: '/article/4' },
  { title: 'BBC inside Trump press briefing slamming Supreme Court tariffs ruling', to: '/article/5' },
])

const rightArticles = ref<RecArticle[]>([
  { id: '6', title: 'Andrew and King Charles: A personal battle of royal brothers', summary: 'Short context here.', meta: '9 hrs ago | UK' },
  { id: '7', title: 'In the army now: Pictures that show how ordinary Ukrainians have been shaped by war', summary: 'Short context here.', meta: '5 hrs ago | Europe' },
  { id: '8', title: 'Trump says he is considering limited military strike on Iran', summary: 'Short context here.', meta: '5 hrs ago | World' },
  { id: '9', title: 'Canada and USA to meet in charged Olympic finale', summary: 'Short context here.', meta: '4 hrs ago | Winter Olympics' },
])

// 第一次：2 条
const weekendReads = ref<BbcItem[]>([
  { id: '1', title: 'The most anticipated museum openings of 2026', subtitle: 'From Paris to Tokyo, a look at the bold new institutions set to redefine cultural landscapes in the coming year.' },
  { id: '2', title: '10 early photographic \'fakes\' that trick the eye', subtitle: 'Long before Photoshop, pioneers of photography were already manipulating images to surprise and deceive.' },
])

// 第二次：4 条
const winterOlympics = ref<BbcItem[]>([
  { id: '3', title: '11 of the Winter Olympics\' most striking images - as classical artworks', subtitle: 'We reimagine the finest moments from the Games in the style of old masters.' },
  { id: '4', title: 'Four men, one aim - to end 102-year wait for curling gold', subtitle: 'The British curling team has its eyes on history in the final.' },
  { id: '5', title: '\'I\'m on right side\': Kenworthy on death threats after ICE post', subtitle: 'The US skier speaks out after receiving abuse for supporting immigration enforcement.' },
  { id: '6', title: 'Eileen Gu: The \'snow princess\' who divides opinion', subtitle: 'The freestyle skier has become one of the most talked-about figures at the Winter Games.' },
])

// 第三次：多条，可按钮滚动
const bestAudioItems = ref<BbcItem[]>([
  { id: '7', title: 'The Global Story', subtitle: 'What \'looksmaxxing\' tells us about modern masculinity', meta: '27 mins', label: 'Podcast' },
  { id: '8', title: 'The Interview', subtitle: 'Gisèle Pelicot: Shame must be carried by the accused, not the victims', meta: '23 mins', label: 'Podcast' },
  { id: '9', title: 'The Documentary Podcast', subtitle: 'Bridgerton: Behind the scenes', meta: '58 mins', label: 'Podcast' },
  { id: '10', title: 'You\'re Dead to Me', subtitle: 'Philippe, Duc d\'Orléans: in the shadow of the Sun King', meta: '18 mins', label: 'Podcast' },
  { id: '11', title: 'Business Daily', subtitle: 'Is AI about to transform food production?', meta: '28 mins', label: 'Podcast' },
  { id: '12', title: 'Lady Killers with Lucy Worsley', subtitle: '59. Kitty Newton - Killer Daughter', meta: '34 mins', label: 'Podcast' },
  { id: '13', title: 'The Global News Podcast', subtitle: 'Former Prince Andrew released as investigations continue', meta: '29 mins', label: 'Podcast' },
  { id: '14', title: 'World of Secrets', subtitle: '1. Meeting a monster', meta: '45 mins', label: 'Podcast' },
])

const recTagBarCenterRef = ref<HTMLElement | null>(null)
const recTagIndicatorStyle = ref({ left: '0px', width: '0px' })

function updateRecTagIndicator() {
  nextTick(() => {
    const wrapper = recTagBarCenterRef.value
    const activeEl = wrapper?.querySelector('.rec-tag-item.active')
    if (!wrapper || !activeEl) {
      recTagIndicatorStyle.value = { left: '0px', width: '0px' }
      return
    }
    const rect = activeEl.getBoundingClientRect()
    const wrapperRect = wrapper.getBoundingClientRect()
    recTagIndicatorStyle.value = {
      left: `${rect.left - wrapperRect.left}px`,
      width: `${rect.width}px`,
    }
  })
}

watch(currentTagId, updateRecTagIndicator)
watch(recTags, updateRecTagIndicator, { deep: true })
onMounted(() => {
  recTags.value = pickTags(10)
  updateRecTagIndicator()
  window.addEventListener('resize', updateRecTagIndicator)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateRecTagIndicator)
})
</script>

<style scoped>
.recommend-page {
  min-height: calc(100vh - 64px);
  background: #f5f5f5;
}

/* 推荐标签栏 */
.rec-tag-bar {
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.rec-tag-bar-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  min-height: 48px;
}

.rec-tag-bar-left {
  width: 40px;
  flex-shrink: 0;
}

.rec-tag-bar-center {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-wrap: wrap;
}

.rec-tag-indicator {
  position: absolute;
  bottom: 0;
  height: 4px;
  background: #BB1919;
  transition: left 0.3s ease, width 0.3s ease;
  pointer-events: none;
}

.rec-tag-item {
  padding: 10px 14px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s, background 0.2s;
}

.rec-tag-item:hover {
  color: #000;
  background: rgba(0, 0, 0, 0.04);
}

.rec-tag-item.active {
  color: #000;
  font-weight: 700;
  text-decoration: none;
}

.rec-tag-refresh {
  flex-shrink: 0;
  width: 40px;
  padding: 8px;
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s, background 0.2s;
}

.rec-tag-refresh:hover {
  color: #000;
  background: rgba(0, 0, 0, 0.06);
}

.rec-tag-refresh .el-icon {
  font-size: 18px;
}

/* 主布局 */
.page-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.rec-main {
  flex: 1;
  min-width: 0;
}

/* 分割线 */
.rec-section-divider {
  height: 0;
  border: none;
  border-top: 1px solid #e0e0e0;
  margin: 32px 0 28px;
}

/* BBC 三栏网格 */
.bbc-grid {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
  gap: 24px;
  align-items: start;
}

.bbc-col-left,
.bbc-col-center,
.bbc-col-right {
  min-width: 0;
}

.bbc-col-left {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.bbc-card-left .bbc-card-img {
  width: 100%;
  aspect-ratio: 16 / 10;
  background: #e8e8e8;
  overflow: hidden;
  margin-bottom: 10px;
}

.bbc-card-left .bbc-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bbc-card-img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: #bbb;
}

.bbc-card {
  text-decoration: none;
  color: inherit;
  display: block;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.bbc-col-left .bbc-card:last-child,
.bbc-col-right .bbc-card:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.bbc-card:hover .bbc-card-headline {
  text-decoration: underline;
  text-decoration-color: #111;
}

.bbc-card-headline {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
  margin: 0 0 8px;
  color: #111;
}

.bbc-card-desc {
  font-size: 14px;
  line-height: 1.5;
  color: #444;
  margin: 0 0 8px;
}

.bbc-card-meta {
  font-size: 12px;
  color: #999;
}

.bbc-feature {
  display: block;
  text-decoration: none;
  color: inherit;
  margin-bottom: 16px;
}

.bbc-feature-img {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #e8e8e8;
  overflow: hidden;
  margin-bottom: 14px;
}

.bbc-feature-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bbc-feature:hover .bbc-feature-headline {
  text-decoration: underline;
  text-decoration-color: #111;
}

.bbc-feature-headline {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  margin: 0 0 12px;
  color: #111;
}

.bbc-feature-desc {
  font-size: 16px;
  line-height: 1.55;
  color: #333;
  margin: 0 0 10px;
}

.bbc-feature-meta {
  font-size: 13px;
  color: #999;
}

.bbc-related {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid #eee;
  padding-top: 12px;
}

.bbc-related li {
  margin-bottom: 8px;
}

.bbc-related a {
  font-size: 15px;
  color: #111;
  text-decoration: none;
  line-height: 1.4;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.bbc-related a:hover {
  color: #111;
  text-decoration: underline;
}

.bbc-related-icon {
  font-size: 14px;
  color: inherit;
}

.bbc-col-right .bbc-card {
  padding: 16px 0;
  border-bottom: 1px solid #eee;
}

.bbc-col-right .bbc-card-headline {
  font-size: 16px;
}

.bbc-col-right .bbc-card-desc {
  font-size: 13px;
}

@media (max-width: 1200px) {
  .bbc-grid {
    grid-template-columns: 1fr 1fr;
  }

  .bbc-col-right {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .bbc-grid {
    grid-template-columns: 1fr;
  }
}
</style>
