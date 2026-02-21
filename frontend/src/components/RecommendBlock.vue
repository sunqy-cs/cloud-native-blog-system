<template>
  <section class="rec-block" :class="`rec-block--${variant}`">
    <div class="rec-block-head">
      <component
        :is="titleLink ? 'router-link' : 'span'"
        :to="titleLink"
        class="rec-block-title"
      >
        {{ title }}
      </component>
      <div v-if="variant === 'strip'" class="rec-block-nav">
        <button type="button" class="rec-block-arrow" title="向左滚动" @click="scrollLeft">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <button type="button" class="rec-block-arrow" title="向右滚动" @click="scrollRight">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </div>

    <!-- variant: two - 2 张大卡 -->
    <div v-if="variant === 'two'" class="rec-block-two">
      <router-link
        v-for="item in items"
        :key="item.id"
        :to="item.link || `/article/${item.id}`"
        class="rec-two-card"
      >
        <div class="rec-two-card-img">
          <img v-if="item.cover" :src="item.cover" :alt="item.title" />
          <span v-else class="rec-img-placeholder">{{ item.title.charAt(0) }}</span>
        </div>
        <h3 class="rec-two-card-headline">{{ item.title }}</h3>
        <p class="rec-two-card-desc">{{ item.subtitle || item.summary }}</p>
      </router-link>
    </div>

    <!-- variant: four - 4 张中卡 -->
    <div v-if="variant === 'four'" class="rec-block-four">
      <router-link
        v-for="item in items"
        :key="item.id"
        :to="item.link || `/article/${item.id}`"
        class="rec-four-card"
      >
        <div class="rec-four-card-img">
          <img v-if="item.cover" :src="item.cover" :alt="item.title" />
          <span v-else class="rec-img-placeholder">{{ item.title.charAt(0) }}</span>
        </div>
        <h3 class="rec-four-card-headline">{{ item.title }}</h3>
        <p class="rec-four-card-desc">{{ item.subtitle || item.summary }}</p>
      </router-link>
    </div>

    <!-- variant: strip - 多条横向滚动，按钮控制 -->
    <div v-if="variant === 'strip'" class="rec-block-strip-wrap">
      <div ref="stripRef" class="rec-block-strip">
        <router-link
          v-for="item in items"
          :key="item.id"
          :to="item.link || `/article/${item.id}`"
          class="rec-strip-card"
        >
          <div class="rec-strip-card-img">
            <img v-if="item.cover" :src="item.cover" :alt="item.title" />
            <span v-else class="rec-img-placeholder">{{ item.title.charAt(0) }}</span>
          </div>
          <span class="rec-strip-card-label">{{ item.label || '推荐' }}</span>
          <h4 class="rec-strip-card-title">{{ item.title }}</h4>
          <p class="rec-strip-card-subtitle">{{ item.subtitle || item.summary }}</p>
          <div class="rec-strip-card-meta">
            <el-icon class="rec-strip-card-icon"><Collection /></el-icon>
            {{ item.meta || '' }}
          </div>
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeft, ArrowRight, Collection } from '@element-plus/icons-vue'

export interface RecommendBlockItem {
  id: string
  title: string
  subtitle?: string
  summary?: string
  meta?: string
  cover?: string
  link?: string
  label?: string
}

const props = withDefaults(
  defineProps<{
    title: string
    titleLink?: string
    variant: 'two' | 'four' | 'strip'
    items: RecommendBlockItem[]
  }>(),
  { titleLink: '' }
)

const stripRef = ref<HTMLElement | null>(null)
const STRIP_SCROLL = 440

function scrollLeft() {
  const el = stripRef.value
  if (!el) return
  el.scrollBy({ left: -STRIP_SCROLL, behavior: 'smooth' })
}

function scrollRight() {
  const el = stripRef.value
  if (!el) return
  el.scrollBy({ left: STRIP_SCROLL, behavior: 'smooth' })
}
</script>

<style scoped>
.rec-block {
  margin-bottom: 32px;
}

.rec-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.rec-block-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.04em;
  text-decoration: none;
  text-transform: uppercase;
}

a.rec-block-title:hover {
  color: #1474b8;
  text-decoration: underline;
}

.rec-block-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.rec-block-arrow {
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: #eee;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s, background 0.2s;
}

.rec-block-arrow:hover {
  background: #ddd;
  color: #111;
}

.rec-block-arrow .el-icon {
  font-size: 18px;
}

/* two: 2 张大卡并排 */
.rec-block-two {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.rec-two-card {
  display: block;
  text-decoration: none;
  color: inherit;
}

.rec-two-card:hover .rec-two-card-headline {
  color: #111;
  text-decoration: underline;
  text-decoration-color: #111;
}

.rec-two-card-img {
  width: 100%;
  aspect-ratio: 16 / 10;
  background: #e0e0e0;
  overflow: hidden;
  margin-bottom: 12px;
}

.rec-two-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rec-two-card-headline {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 8px;
  color: #1a1a1a;
}

.rec-two-card-desc {
  font-size: 15px;
  line-height: 1.5;
  color: #444;
  margin: 0;
}

/* four: 4 张中卡并排 */
.rec-block-four {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.rec-four-card {
  display: block;
  text-decoration: none;
  color: inherit;
}

.rec-four-card:hover .rec-four-card-headline {
  color: #111;
  text-decoration: underline;
  text-decoration-color: #111;
}

.rec-four-card-img {
  width: 100%;
  aspect-ratio: 16 / 10;
  background: #e0e0e0;
  overflow: hidden;
  margin-bottom: 10px;
}

.rec-four-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rec-four-card-headline {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 6px;
  color: #1a1a1a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rec-four-card-desc {
  font-size: 14px;
  line-height: 1.45;
  color: #444;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* strip: 横向滚动条，按钮滚动 */
.rec-block-strip-wrap {
  overflow: hidden;
}

.rec-block-strip {
  display: flex;
  flex-direction: row;
  gap: 20px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
  scrollbar-width: none;
}

.rec-block-strip::-webkit-scrollbar {
  height: 6px;
}

.rec-block-strip::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.rec-strip-card {
  flex: 0 0 200px;
  width: 200px;
  text-decoration: none;
  color: inherit;
  display: block;
}

.rec-strip-card:hover .rec-strip-card-title {
  color: #111;
  text-decoration: underline;
  text-decoration-color: #111;
}

.rec-strip-card-img {
  width: 200px;
  height: 200px;
  background: #e0e0e0;
  overflow: hidden;
  margin-bottom: 10px;
}

.rec-strip-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rec-strip-card-label {
  display: block;
  font-size: 11px;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.rec-strip-card-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 4px;
  color: #1a1a1a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rec-strip-card-subtitle {
  font-size: 13px;
  line-height: 1.4;
  color: #555;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rec-strip-card-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.rec-strip-card-icon {
  font-size: 14px;
}

.rec-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 700;
  color: #aaa;
  user-select: none;
}

@media (max-width: 1024px) {
  .rec-block-four {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .rec-block-two {
    grid-template-columns: 1fr;
  }

  .rec-block-four {
    grid-template-columns: 1fr;
  }

  .rec-strip-card {
    flex: 0 0 160px;
    width: 160px;
  }

  .rec-strip-card-img {
    width: 160px;
    height: 160px;
  }
}
</style>
