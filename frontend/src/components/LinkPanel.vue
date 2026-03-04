<template>
  <!-- 悬浮可拖动模式 -->
  <div
    v-if="floating"
    ref="floatRef"
    class="link-panel-float"
    :style="floatStyle"
  >
    <aside class="link-panel link-panel--float" :class="{ 'link-panel--collapsed': collapsed }">
      <div class="link-panel-header">
        <div class="link-panel-drag-handle" title="拖动移动" @mousedown.prevent="onDragStart">
          <span class="link-panel-grip">⋮⋮</span>
        </div>
        <div v-show="!collapsed" class="link-panel-tabs">
          <button
            type="button"
            class="link-panel-tab"
            :class="{ active: activeTab === 'in' }"
            title="入链：链接到当前文件"
            @click="activeTab = 'in'"
          >
            <el-icon><Back /></el-icon>
            <span>入链</span>
          </button>
          <button
            v-if="showOutlinks"
            type="button"
            class="link-panel-tab"
            :class="{ active: activeTab === 'out' }"
            title="出链：当前文件链接到"
            @click="activeTab = 'out'"
          >
            <el-icon><Right /></el-icon>
            <span>出链</span>
          </button>
        </div>
        <button
          type="button"
          class="link-panel-fold"
          :title="collapsed ? '展开' : '收起'"
          @click="collapsed = !collapsed"
        >
          <el-icon><DArrowRight v-if="collapsed" /><DArrowLeft v-else /></el-icon>
        </button>
      </div>
      <div v-show="!collapsed" class="link-panel-body">
        <template v-if="activeTab === 'in'">
          <h4 class="link-panel-section-title">链接当前文件</h4>
          <div v-if="backlinksLoading" class="link-panel-loading">加载中…</div>
          <p v-else-if="!backlinks.length" class="link-panel-empty">没有笔记链接</p>
          <ul v-else class="link-panel-list">
            <li v-for="item in backlinks" :key="item.id" class="link-panel-item">
              <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
                {{ item.title || '[无标题]' }}
              </button>
            </li>
          </ul>
        </template>
        <template v-else-if="showOutlinks && activeTab === 'out'">
          <h4 class="link-panel-section-title">当前文件链接到</h4>
          <div v-if="outlinksLoading" class="link-panel-loading">加载中…</div>
          <p v-else-if="!outlinks.length" class="link-panel-empty">没有引出链接</p>
          <ul v-else class="link-panel-list">
            <li v-for="item in outlinks" :key="item.id" class="link-panel-item">
              <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
                {{ item.title || '[无标题]' }}
              </button>
            </li>
          </ul>
        </template>
      </div>
    </aside>
  </div>
  <!-- 边栏模式 -->
  <aside v-else class="link-panel" :class="{ 'link-panel--collapsed': collapsed }">
    <div class="link-panel-header">
      <div v-show="!collapsed" class="link-panel-tabs">
        <button
          type="button"
          class="link-panel-tab"
          :class="{ active: activeTab === 'in' }"
          title="入链：链接到当前文件"
          @click="activeTab = 'in'"
        >
          <el-icon><Back /></el-icon>
          <span>入链</span>
        </button>
        <button
          v-if="showOutlinks"
          type="button"
          class="link-panel-tab"
          :class="{ active: activeTab === 'out' }"
          title="出链：当前文件链接到"
          @click="activeTab = 'out'"
        >
          <el-icon><Right /></el-icon>
          <span>出链</span>
        </button>
      </div>
      <button
        type="button"
        class="link-panel-fold"
        :title="collapsed ? '展开' : '收起'"
        @click="collapsed = !collapsed"
      >
        <el-icon><DArrowRight v-if="collapsed" /><DArrowLeft v-else /></el-icon>
      </button>
    </div>
    <div v-show="!collapsed" class="link-panel-body">
      <template v-if="activeTab === 'in'">
        <h4 class="link-panel-section-title">链接当前文件</h4>
        <div v-if="backlinksLoading" class="link-panel-loading">加载中…</div>
        <p v-else-if="!backlinks.length" class="link-panel-empty">没有笔记链接</p>
        <ul v-else class="link-panel-list">
          <li v-for="item in backlinks" :key="item.id" class="link-panel-item">
            <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
              {{ item.title || '[无标题]' }}
            </button>
          </li>
        </ul>
      </template>
      <template v-else-if="showOutlinks && activeTab === 'out'">
        <h4 class="link-panel-section-title">当前文件链接到</h4>
        <div v-if="outlinksLoading" class="link-panel-loading">加载中…</div>
        <p v-else-if="!outlinks.length" class="link-panel-empty">没有引出链接</p>
        <ul v-else class="link-panel-list">
          <li v-for="item in outlinks" :key="item.id" class="link-panel-item">
            <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
              {{ item.title || '[无标题]' }}
            </button>
          </li>
        </ul>
      </template>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Back, Right, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'
import { getContentBacklinks, getContentOutlinks, type ContentListItem } from '@/api/content'

const props = withDefaults(
  defineProps<{
    contentId: number | null
    /** 是否显示出链（笔记为 true，博客仅入链为 false） */
    showOutlinks?: boolean
    /** 是否为悬浮卡片（可拖动） */
    floating?: boolean
  }>(),
  { showOutlinks: true, floating: false }
)

defineEmits<{ (e: 'open', id: number): void }>()

/** 悬浮卡片默认收起，边栏模式默认展开 */
const collapsed = ref(props.floating)
const activeTab = ref<'in' | 'out'>('in')
const backlinks = ref<ContentListItem[]>([])
const outlinks = ref<ContentListItem[]>([])
const backlinksLoading = ref(false)
const outlinksLoading = ref(false)

/** 悬浮位置：null 表示使用默认 right/top，有值则用 left/top */
const dragPosition = ref<{ left: number; top: number } | null>(null)
const floatRef = ref<HTMLDivElement | null>(null)

const DEFAULT_RIGHT = 20
const DEFAULT_TOP = 88
const PANEL_WIDTH = 260

const floatStyle = computed(() => {
  if (!props.floating) return undefined
  const pos = dragPosition.value
  return {
    position: 'fixed' as const,
    zIndex: 100,
    ...(pos
      ? { left: `${pos.left}px`, top: `${pos.top}px` }
      : { right: `${DEFAULT_RIGHT}px`, top: `${DEFAULT_TOP}px` }),
    borderRadius: '12px',
    boxShadow: '0 4px 24px rgba(0,0,0,0.1), 0 0 1px rgba(0,0,0,0.06)',
    overflow: 'hidden',
    background: '#fff',
  }
})

function onDragStart(e: MouseEvent) {
  if (!floatRef.value) return
  const rect = floatRef.value.getBoundingClientRect()
  let startX = e.clientX
  let startY = e.clientY
  let left = dragPosition.value?.left ?? rect.left
  let top = dragPosition.value?.top ?? rect.top

  function onMove(moveE: MouseEvent) {
    const dx = moveE.clientX - startX
    const dy = moveE.clientY - startY
    startX = moveE.clientX
    startY = moveE.clientY
    left += dx
    top += dy
    const w = floatRef.value?.getBoundingClientRect().width ?? PANEL_WIDTH
    left = Math.max(0, Math.min(left, document.documentElement.clientWidth - w))
    top = Math.max(0, Math.min(top, document.documentElement.clientHeight - 60))
    dragPosition.value = { left, top }
  }

  function onUp() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.body.style.removeProperty('user-select')
    document.body.style.removeProperty('cursor')
  }

  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'grabbing'
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  dragPosition.value = { left, top }
}

function load() {
  if (props.contentId == null) {
    backlinks.value = []
    outlinks.value = []
    return
  }
  backlinksLoading.value = true
  getContentBacklinks(props.contentId)
    .then((list) => { backlinks.value = list ?? [] })
    .finally(() => { backlinksLoading.value = false })
  if (props.showOutlinks) {
    outlinksLoading.value = true
    getContentOutlinks(props.contentId)
      .then((list) => { outlinks.value = list ?? [] })
      .finally(() => { outlinksLoading.value = false })
  } else {
    outlinks.value = []
  }
}

watch(() => props.contentId, load, { immediate: true })
</script>

<style scoped>
.link-panel-float {
  cursor: default;
}
.link-panel-drag-handle {
  cursor: grab;
  padding: 4px 6px;
  margin: -4px 4px -4px -6px;
  border-radius: 4px;
  color: #86868b;
  display: flex;
  align-items: center;
  user-select: none;
}
.link-panel-drag-handle:active {
  cursor: grabbing;
}
.link-panel-drag-handle:hover {
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.05);
}
.link-panel-grip {
  font-size: 14px;
  letter-spacing: -2px;
  line-height: 1;
}
.link-panel--float {
  border-left: none;
}
.link-panel {
  flex-shrink: 0;
  width: 260px;
  border-left: 1px solid rgba(0, 0, 0, 0.06);
  background: #fafafa;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
}
.link-panel--collapsed {
  width: 56px;
  min-width: 56px;
}

.link-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 10px 10px 14px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  min-height: 44px;
  flex-shrink: 0;
}
.link-panel--collapsed .link-panel-header {
  justify-content: center;
  padding: 10px;
}
.link-panel-tabs {
  display: flex;
  gap: 4px;
}
.link-panel-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  font-size: 13px;
  color: #424245;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}
.link-panel-tab:hover {
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.05);
}
.link-panel-tab.active {
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.08);
}
.link-panel-tab .el-icon {
  font-size: 16px;
}
.link-panel-fold {
  padding: 6px;
  color: #86868b;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
  flex-shrink: 0;
}
.link-panel--collapsed .link-panel-fold {
  padding: 10px;
  min-width: 36px;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.link-panel-fold:hover {
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.05);
}

.link-panel-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px;
}
.link-panel-section-title {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 600;
  color: #424245;
  letter-spacing: -0.01em;
}
.link-panel-loading,
.link-panel-empty {
  margin: 0;
  font-size: 13px;
  color: #86868b;
}
.link-panel-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.link-panel-item {
  margin-bottom: 2px;
}
.link-panel-link {
  display: block;
  width: 100%;
  padding: 8px 10px;
  font-size: 13px;
  color: #1d1d1f;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease, background 0.2s ease;
}
.link-panel-link:hover {
  color: #007aff;
  background: rgba(0, 122, 255, 0.08);
}
</style>
