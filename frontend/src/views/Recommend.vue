<template>
  <div class="recommend-page">
    <!-- 顶部：全部（默认选中）+ 3 个主标签 + 换一批，括号内容不显示 -->
    <nav class="rec-tag-bar">
      <div class="rec-tag-bar-inner" ref="recTagBarWrapperRef">
        <router-link :to="{ path: '/recommend' }" class="rec-tag-item rec-tag-all" :class="{ active: isAllActive }">全部</router-link>
        <router-link
          v-for="tag in recDisplayedTags"
          :key="tag.id"
          :to="{ path: '/recommend', query: { tag: tag.id } }"
          class="rec-tag-item"
          :class="{ active: currentTagId === tag.id }"
        >
          {{ tag.name }}
        </router-link>
        <button type="button" class="rec-tag-refresh" title="换一批" @click="refreshDisplayedTags">
          <el-icon><Refresh /></el-icon>
        </button>
        <div class="rec-tag-indicator" :style="recTagIndicatorStyle"></div>
      </div>
    </nav>

    <div id="rec-section-all" class="page-layout">
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

        <!-- 第 1～3 栏（白），带 id 供导航滚动 -->
        <template v-for="(tag, idx) in mainTagsForSections.slice(0, 3)" :key="'w-' + tag.id">
          <div class="rec-section-divider"></div>
          <div :id="'rec-section-tag-' + tag.id">
            <RecommendBlock
              :title="tag.name"
              :title-link="`/recommend?tag=${tag.id}`"
              :variant="(SECTION_VARIANTS[idx] && SECTION_VARIANTS[idx].variant) || 'two'"
              :items="(tagSectionItems[idx] || [])"
              :image-side="SECTION_VARIANTS[idx]?.imageSide || 'left'"
            />
          </div>
        </template>

        <div class="rec-section-divider"></div>
      </main>
    </div>

    <!-- 第 1 条黑色栏：第四个主标签「多模态与生成模型」内容 -->
    <div id="rec-section-black-1" class="rec-dark-strip-wrap">
      <div class="rec-dark-strip-inner">
        <EditorPicksStrip
          :title="mainTagsForSections[3]?.name ?? '编辑精选'"
          :items="editorPicksFirstStripItems"
        />
      </div>
    </div>

    <div class="page-layout">
      <main class="rec-main">
        <div class="rec-section-divider"></div>
        <!-- 第 4～9 栏（白），带 id 供导航滚动 -->
        <template v-for="idx in 6" :key="'w-' + (idx + 3)">
          <div v-if="mainTagsForSections[idx + 3]" :id="'rec-section-tag-' + mainTagsForSections[idx + 3].id">
            <RecommendBlock
              :title="mainTagsForSections[idx + 3].name"
              :title-link="`/recommend?tag=${mainTagsForSections[idx + 3].id}`"
              :variant="(SECTION_VARIANTS[idx + 3] && SECTION_VARIANTS[idx + 3].variant) || 'two'"
              :items="(tagSectionItems[idx + 3] || [])"
              :image-side="SECTION_VARIANTS[idx + 3]?.imageSide || 'left'"
            />
          </div>
          <div v-if="mainTagsForSections[idx + 3]" class="rec-section-divider"></div>
        </template>
      </main>
    </div>

    <!-- 第 2 条黑色栏：最后一个主标签「评测基准与实验方法」内容 -->
    <div id="rec-section-black-2" class="rec-dark-strip-wrap">
      <div class="rec-dark-strip-inner">
        <EditorPicksStrip
          :title="lastMainTag?.name ?? '编辑精选'"
          :items="editorPicksSecondStripItems"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { VideoPlay, Refresh } from '@element-plus/icons-vue'
import { getMainTags } from '@/api/tag'
import { getContentsList, type ContentListItem } from '@/api/content'
import RecommendBlock from '@/components/RecommendBlock.vue'
import EditorPicksStrip from '@/components/EditorPicksStrip.vue'
import type { RecommendBlockItem } from '@/components/RecommendBlock.vue'
import type { EditorPicksItem } from '@/components/EditorPicksStrip.vue'

interface RecTag {
  id: string
  name: string
}

/** 将接口内容项转为推荐块/编辑精选的展示项 */
function toRecItem(c: ContentListItem): RecommendBlockItem {
  return {
    id: String(c.id),
    title: c.title || '无标题',
    summary: c.summary ?? '',
    meta: formatRecMeta(c.createdAt),
    cover: c.cover ?? undefined,
  }
}

function toEditorPickItem(c: ContentListItem): EditorPicksItem {
  return {
    id: String(c.id),
    title: c.title || '无标题',
    subtitle: c.summary ?? '',
    cover: c.cover ?? undefined,
    meta: formatRecMeta(c.createdAt),
  }
}

function formatRecMeta(createdAt: string): string {
  if (!createdAt) return ''
  const d = new Date(createdAt)
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const day = d.getDate()
  return `${y}年${m}月${day}日`
}

/** 11 个主标签栏的版式配置（保持原有格式：two/four/strip/feature 等） */
const SECTION_VARIANTS: { variant: 'two' | 'three' | 'four' | 'strip' | 'feature'; pageSize: number; imageSide?: 'left' | 'right' }[] = [
  { variant: 'two', pageSize: 2 },
  { variant: 'four', pageSize: 4 },
  { variant: 'strip', pageSize: 6 },
  { variant: 'four', pageSize: 4 },
  { variant: 'two', pageSize: 2 },
  { variant: 'three', pageSize: 3 },
  { variant: 'feature', pageSize: 1, imageSide: 'left' },
  { variant: 'three', pageSize: 3 },
  { variant: 'feature', pageSize: 1, imageSide: 'right' },
  { variant: 'two', pageSize: 2 },
  { variant: 'four', pageSize: 4 },
]

const route = useRoute()
/** 去掉标签名中括号及括号内内容（支持半角 () 与全角 （）），仅显示主名称 */
function stripParentheses(name: string): string {
  if (!name || typeof name !== 'string') return name
  return name
    .replace(/\s*\([^)]*\)/g, '')           // 半角括号
    .replace(/\s*（[^）]*）/g, '')           // 全角括号
    .trim() || name
}

/** 除「其他」外的主标签池（展示名已去括号） */
const recNavTags = ref<RecTag[]>([])
/** 当前展示的 3 个标签 */
const recDisplayedTags = ref<RecTag[]>([])
const DISPLAYED_TAG_COUNT = 3
/** 选「全部」时：顺序切换的起始下标，换一批 +3 */
const recDisplayStartIndex = ref(0)
/** 选中某个标签时：另外两个的起始下标（在「除当前外」的池子里），换一批 +2 */
const recOthersStartIndex = ref(0)

const currentTagId = computed(() => {
  const t = route.query.tag as string | undefined
  return t ?? ''
})
const isAllActive = computed(() => !currentTagId.value)

/** 选「全部」时：按起始下标取 3 个 */
function applyDisplayedFromStart() {
  const pool = recNavTags.value
  const n = pool.length
  if (n <= DISPLAYED_TAG_COUNT) {
    recDisplayedTags.value = [...pool]
    return
  }
  const start = recDisplayStartIndex.value % n
  recDisplayedTags.value = [
    pool[start],
    pool[(start + 1) % n],
    pool[(start + 2) % n],
  ]
}

/** 选中某标签时：保留当前选中，另外两个从「除当前外」的池子里按顺序取 */
function applyDisplayedWithCurrent() {
  const pool = recNavTags.value
  const curId = currentTagId.value
  const current = pool.find((t) => t.id === curId)
  if (!current) {
    applyDisplayedFromStart()
    return
  }
  const others = pool.filter((t) => t.id !== curId)
  const L = others.length
  if (L < 2) {
    recDisplayedTags.value = L === 1 ? [current, others[0]] : [current]
    return
  }
  const start = recOthersStartIndex.value % L
  recDisplayedTags.value = [
    current,
    others[start],
    others[(start + 1) % L],
  ]
}

function applyDisplayed() {
  if (isAllActive.value) applyDisplayedFromStart()
  else applyDisplayedWithCurrent()
}

function refreshDisplayedTags() {
  const pool = recNavTags.value
  const n = pool.length
  if (n <= DISPLAYED_TAG_COUNT) return
  if (isAllActive.value) {
    recDisplayStartIndex.value = (recDisplayStartIndex.value + DISPLAYED_TAG_COUNT) % n
  } else {
    const curId = currentTagId.value
    const othersCount = pool.some((t) => t.id === curId) ? pool.length - 1 : pool.length
    if (othersCount >= 2) {
      recOthersStartIndex.value = (recOthersStartIndex.value + 2) % othersCount
    }
  }
  applyDisplayed()
  updateRecTagIndicator()
}

interface RecArticle {
  id: string
  title: string
  summary: string
  meta: string
  cover?: string
}

/** 顶区：除下面 11 栏之外的最新文章（左2 + 中1 + 右4 + 相关2） */
const leftArticles = ref<RecommendBlockItem[]>([])
const featureArticle = ref<RecommendBlockItem | null>(null)
const relatedLinks = ref<{ title: string; to: string }[]>([])
const rightArticles = ref<RecommendBlockItem[]>([])

/** 11 个主标签栏的数据，与 mainTagsForSections 一一对应 */
const tagSectionItems = ref<RecommendBlockItem[][]>([])
const mainTagsForSections = ref<RecTag[]>([])
const recTopLoading = ref(false)
const recSectionsLoading = ref(false)

/** 第一条黑色栏：第四个主标签「多模态与生成模型」 */
const editorPicksFirstStripItems = ref<EditorPicksItem[]>([])
const FIRST_EDITOR_STRIP_TAG_INDEX = 3
/** 第二条黑色栏：最后一个主标签「评测基准与实验方法」 */
const editorPicksSecondStripItems = ref<EditorPicksItem[]>([])
const lastMainTag = computed(() => {
  const tags = mainTagsForSections.value
  return tags.length > 0 ? tags[tags.length - 1] : null
})

/** 加载推荐页数据：11 栏按主标签拉取最新文章，顶区为除此以外的全局最新（需先已设置 mainTagsForSections） */
async function loadRecData() {
  const tags = mainTagsForSections.value
  if (tags.length === 0) return

  recSectionsLoading.value = true
  try {
    const sectionPromises = tags.map((tag, i) => {
      const conf = SECTION_VARIANTS[i] ?? { variant: 'two', pageSize: 2 }
      return getContentsList({
        mainTagId: Number(tag.id),
        page: 1,
        pageSize: conf.pageSize,
        sortBy: 'time',
        order: 'desc',
      }).then((res) => res.list.map(toRecItem))
    })
    const sectionLists = await Promise.all(sectionPromises)
    tagSectionItems.value = sectionLists

    if (tags.length > FIRST_EDITOR_STRIP_TAG_INDEX) {
      const fourthTag = tags[FIRST_EDITOR_STRIP_TAG_INDEX]
      const res = await getContentsList({
        mainTagId: Number(fourthTag.id),
        page: 1,
        pageSize: 8,
        sortBy: 'time',
        order: 'desc',
      })
      editorPicksFirstStripItems.value = res.list.map(toEditorPickItem)
    } else {
      editorPicksFirstStripItems.value = []
    }
    const lastTag = tags[tags.length - 1]
    if (lastTag) {
      const resLast = await getContentsList({
        mainTagId: Number(lastTag.id),
        page: 1,
        pageSize: 8,
        sortBy: 'time',
        order: 'desc',
      })
      editorPicksSecondStripItems.value = resLast.list.map(toEditorPickItem)
    } else {
      editorPicksSecondStripItems.value = []
    }
  } finally {
    recSectionsLoading.value = false
  }

  const idsBelow = new Set(tagSectionItems.value.flat().map((x) => x.id))
  recTopLoading.value = true
  try {
    const topRes = await getContentsList({ page: 1, pageSize: 50, sortBy: 'time', order: 'desc' })
    const topFiltered = topRes.list.filter((c) => !idsBelow.has(String(c.id))).map(toRecItem)
    leftArticles.value = topFiltered.slice(0, 2)
    featureArticle.value = topFiltered[2] ?? null
    rightArticles.value = topFiltered.slice(3, 7)
    relatedLinks.value = topFiltered.slice(7, 9).map((c) => ({ title: c.title, to: `/article/${c.id}` }))
  } finally {
    recTopLoading.value = false
  }
}

const recTagBarWrapperRef = ref<HTMLElement | null>(null)
const recTagIndicatorStyle = ref({ left: '0px', width: '0px' })

function updateRecTagIndicator() {
  nextTick(() => {
    const wrapper = recTagBarWrapperRef.value
    const activeEl = wrapper?.querySelector('.rec-tag-item.active') as HTMLElement | null
    if (!wrapper || !activeEl) {
      recTagIndicatorStyle.value = { left: '0px', width: '0px' }
      return
    }
    const left = activeEl.offsetLeft
    const width = activeEl.offsetWidth
    recTagIndicatorStyle.value = { left: `${left}px`, width: `${width}px` }
  })
}

/** 根据当前选中的 tagId 得到对应栏的 DOM id（全部=顶区，第4主标签=黑栏1，最后主标签=黑栏2，其余=白栏） */
function getRecSectionId(tagId: string): string {
  if (!tagId) return 'rec-section-all'
  const tags = mainTagsForSections.value
  if (tags.length > FIRST_EDITOR_STRIP_TAG_INDEX && tags[FIRST_EDITOR_STRIP_TAG_INDEX]?.id === tagId) return 'rec-section-black-1'
  if (lastMainTag.value?.id === tagId) return 'rec-section-black-2'
  return 'rec-section-tag-' + tagId
}

function scrollToRecSection() {
  const id = getRecSectionId(currentTagId.value)
  nextTick(() => {
    const el = document.getElementById(id)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

watch([currentTagId, recDisplayedTags], updateRecTagIndicator, { flush: 'post' })
watch([currentTagId, recNavTags], () => {
  recOthersStartIndex.value = 0
  applyDisplayed()
  updateRecTagIndicator()
}, { flush: 'post' })
watch(currentTagId, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) scrollToRecSection()
})

onMounted(() => {
  getMainTags().then((list) => {
    if (!Array.isArray(list)) return
    const filtered = list.filter((t) => t.name !== '其他').map((t) => ({ id: String(t.id), name: stripParentheses(t.name) }))
    recNavTags.value = filtered
    mainTagsForSections.value = filtered.slice(0, 11)
    recDisplayStartIndex.value = 0
    recOthersStartIndex.value = 0
    applyDisplayed()
    updateRecTagIndicator()
    loadRecData()
  })
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

/* 导航栏点击滚动到对应栏时，留出顶栏+导航高度，避免被遮挡 */
#rec-section-all,
#rec-section-black-1,
#rec-section-black-2,
[id^="rec-section-tag-"] {
  scroll-margin-top: 116px;
}

/* 推荐标签栏：不随滚动条滚动 */
.rec-tag-bar {
  position: sticky;
  top: 64px;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.rec-tag-bar-inner {
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

.rec-tag-all {
  margin-right: 8px;
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
  padding: 10px 18px;
  color: #444;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
}

.rec-tag-item:hover {
  color: #000;
  background: rgba(0, 0, 0, 0.05);
}

.rec-tag-item.active {
  color: #000;
  font-weight: 700;
  text-decoration: none;
}

.rec-tag-refresh {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  padding: 8px;
  margin-left: 8px;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  border-radius: 8px;
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

/* 全宽深色栏：占满从左到右 */
.rec-dark-strip-wrap {
  width: 100%;
  background: #1a1a1a;
  padding: 32px 0 40px;
}

.rec-dark-strip-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
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
