<template>
  <section class="editor-picks">
    <div class="editor-picks-line"></div>
    <div class="editor-picks-head">
      <h2 class="editor-picks-title">{{ title }}</h2>
      <div class="editor-picks-nav">
        <button type="button" class="editor-picks-arrow" title="向左滚动" @click="scrollLeft">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <button type="button" class="editor-picks-arrow" title="向右滚动" @click="scrollRight">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </div>
    <div ref="stripRef" class="editor-picks-strip">
      <router-link
        v-for="item in items"
        :key="item.id"
        :to="item.link || `/article/${item.id}`"
        class="editor-picks-card"
      >
        <div class="editor-picks-card-img">
          <img v-if="item.cover" :src="item.cover" :alt="item.title" />
          <span v-else class="editor-picks-img-ph">{{ item.title.charAt(0) }}</span>
          <span class="editor-picks-play">
            <el-icon><VideoPlay /></el-icon>
          </span>
        </div>
        <h3 class="editor-picks-card-title">{{ item.title }}</h3>
        <p class="editor-picks-card-desc">{{ item.subtitle || item.summary }}</p>
        <span class="editor-picks-card-label">{{ item.meta || item.label || '' }}</span>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeft, ArrowRight, VideoPlay } from '@element-plus/icons-vue'

export interface EditorPicksItem {
  id: string
  title: string
  subtitle?: string
  summary?: string
  cover?: string
  link?: string
  label?: string
  /** 博客时间（如 2026年2月28日），优先显示 */
  meta?: string
}

const props = withDefaults(
  defineProps<{
    title?: string
    items: EditorPicksItem[]
  }>(),
  { title: "EDITOR'S PICKS" }
)

const stripRef = ref<HTMLElement | null>(null)
const STRIP_SCROLL = 320

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
.editor-picks {
  padding-bottom: 8px;
}

.editor-picks-line {
  height: 1px;
  background: rgba(255, 255, 255, 0.25);
  margin-bottom: 14px;
}

.editor-picks-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.editor-picks-title {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin: 0;
}

.editor-picks-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.editor-picks-arrow {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: transparent;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, border-color 0.2s;
}

.editor-picks-arrow:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.6);
}

.editor-picks-arrow .el-icon {
  font-size: 16px;
}

.editor-picks-strip {
  display: flex;
  flex-direction: row;
  gap: 24px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
  scrollbar-width: none;
}

.editor-picks-strip::-webkit-scrollbar {
  height: 6px;
}

.editor-picks-strip::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.editor-picks-card {
  flex: 0 0 280px;
  width: 280px;
  text-decoration: none;
  color: inherit;
  display: block;
}

.editor-picks-card:hover .editor-picks-card-title {
  text-decoration: underline;
  text-decoration-color: #fff;
}

.editor-picks-card-img {
  width: 280px;
  height: 160px;
  background: #333;
  overflow: hidden;
  margin-bottom: 12px;
  position: relative;
}

.editor-picks-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.editor-picks-img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  color: #666;
}

.editor-picks-play {
  position: absolute;
  left: 10px;
  top: 10px;
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 2px;
}

.editor-picks-play .el-icon {
  font-size: 18px;
}

.editor-picks-card-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
  margin: 0 0 6px;
  color: #fff;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.editor-picks-card-desc {
  font-size: 14px;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.editor-picks-card-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

@media (max-width: 768px) {
  .editor-picks-card {
    flex: 0 0 240px;
    width: 240px;
  }

  .editor-picks-card-img {
    width: 240px;
    height: 136px;
  }
}
</style>
