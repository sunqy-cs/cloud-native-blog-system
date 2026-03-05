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
          <div class="link-panel-section-head">
            <h4 class="link-panel-section-title">链接当前文件</h4>
            <button type="button" class="link-panel-btn-add" title="添加入链" @click="openAddBacklinkPicker">添加</button>
          </div>
          <div v-if="backlinksLoading" class="link-panel-loading">加载中…</div>
          <p v-else-if="!backlinks.length" class="link-panel-empty">没有笔记链接</p>
          <ul v-else class="link-panel-list">
            <li v-for="item in backlinks" :key="item.id" class="link-panel-item">
              <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
                {{ item.title || '[无标题]' }}
              </button>
              <button type="button" class="link-panel-btn-del" title="删除入链" @click.stop="onDeleteBacklink(item.id)">删除</button>
            </li>
          </ul>
        </template>
        <template v-else-if="showOutlinks && activeTab === 'out'">
          <div class="link-panel-section-head">
            <h4 class="link-panel-section-title">当前文件链接到</h4>
            <button type="button" class="link-panel-btn-add" title="添加出链" @click="openAddOutlinkPicker">添加</button>
          </div>
          <div v-if="outlinksLoading" class="link-panel-loading">加载中…</div>
          <p v-else-if="!outlinks.length" class="link-panel-empty">没有引出链接</p>
          <ul v-else class="link-panel-list">
            <li v-for="item in outlinks" :key="item.id" class="link-panel-item">
              <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
                {{ item.title || '[无标题]' }}
              </button>
              <button type="button" class="link-panel-btn-del" title="删除出链" @click.stop="onDeleteOutlink(item.id)">删除</button>
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
        <div class="link-panel-section-head">
          <h4 class="link-panel-section-title">链接当前文件</h4>
          <button type="button" class="link-panel-btn-add" title="添加入链" @click="openAddBacklinkPicker">添加</button>
        </div>
        <div v-if="backlinksLoading" class="link-panel-loading">加载中…</div>
        <p v-else-if="!backlinks.length" class="link-panel-empty">没有笔记链接</p>
        <ul v-else class="link-panel-list">
          <li v-for="item in backlinks" :key="item.id" class="link-panel-item">
            <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
              {{ item.title || '[无标题]' }}
            </button>
            <button type="button" class="link-panel-btn-del" title="删除入链" @click.stop="onDeleteBacklink(item.id)">删除</button>
          </li>
        </ul>
      </template>
      <template v-else-if="showOutlinks && activeTab === 'out'">
        <div class="link-panel-section-head">
          <h4 class="link-panel-section-title">当前文件链接到</h4>
          <button type="button" class="link-panel-btn-add" title="添加出链" @click="openAddOutlinkPicker">添加</button>
        </div>
        <div v-if="outlinksLoading" class="link-panel-loading">加载中…</div>
        <p v-else-if="!outlinks.length" class="link-panel-empty">没有引出链接</p>
        <ul v-else class="link-panel-list">
          <li v-for="item in outlinks" :key="item.id" class="link-panel-item">
            <button type="button" class="link-panel-link" @click="$emit('open', item.id)">
              {{ item.title || '[无标题]' }}
            </button>
            <button type="button" class="link-panel-btn-del" title="删除出链" @click.stop="onDeleteOutlink(item.id)">删除</button>
          </li>
        </ul>
      </template>
    </div>
  </aside>

  <!-- 选择内容作为入链/出链 -->
  <el-dialog
    v-model="pickerVisible"
    :title="pickerMode === 'backlink' ? '选择要链接到当前文件的笔记' : '选择当前文件要链接到的内容'"
    width="400px"
    class="link-panel-picker-dialog"
    @closed="pickerList = []"
  >
    <div v-if="pickerLoading" class="link-panel-loading">加载中…</div>
    <ul v-else-if="pickerList.length === 0" class="link-panel-empty">暂无可选内容</ul>
    <ul v-else class="link-panel-picker-list">
      <li
        v-for="item in pickerList"
        :key="item.id"
        class="link-panel-picker-item"
        @click="onPickContent(item)"
      >
        {{ item.title || '[无标题]' }}
      </li>
    </ul>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Back, Right, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getContentBacklinks,
  getContentOutlinks,
  addContentOutlink,
  deleteContentOutlink,
  addContentBacklink,
  deleteContentBacklink,
  getContentsMe,
  type ContentListItem,
} from '@/api/content'

const props = withDefaults(
  defineProps<{
    contentId: number | null
    /** 是否显示出链（笔记为 true，博客仅入链为 false） */
    showOutlinks?: boolean
    /** 是否为悬浮卡片（可拖动） */
    floating?: boolean
    /** 是否为知识库且可编辑正文：true 时添加/删除出链通过事件由父组件插入/移除 [[id]]；false（博客等）时通过 API 增删 */
    canEditBody?: boolean
    /** 可选链接范围（如传入则只从该列表中选，用于同一知识库内；不传则拉取全部我的内容） */
    candidateContents?: { id: number; title?: string }[]
    /** 父组件保存正文后递增此值，用于重新拉取入链/出链列表 */
    refreshTrigger?: number
  }>(),
  { showOutlinks: true, floating: false, canEditBody: false, candidateContents: () => [] }
)

const emit = defineEmits<{
  (e: 'open', id: number): void
  (e: 'insert-outlink', payload: { id: number; title: string }): void
  (e: 'remove-outlink', payload: { id: number }): void
}>()

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

const pickerVisible = ref(false)
const pickerMode = ref<'backlink' | 'outlink'>('outlink')
const pickerList = ref<ContentListItem[]>([])
const pickerLoading = ref(false)

function openAddBacklinkPicker() {
  if (props.contentId == null) return
  pickerMode.value = 'backlink'
  pickerVisible.value = true
  const candidates = props.candidateContents?.length ? props.candidateContents : null
  if (candidates) {
    pickerLoading.value = false
    pickerList.value = candidates.filter((c) => c.id !== props.contentId && !backlinks.value.some((b) => b.id === c.id)) as ContentListItem[]
  } else {
    pickerLoading.value = true
    getContentsMe({ page: 1, pageSize: 200 })
      .then((res) => {
        pickerList.value = (res.list ?? []).filter((c) => c.id !== props.contentId && !backlinks.value.some((b) => b.id === c.id))
      })
      .catch(() => { pickerList.value = [] })
      .finally(() => { pickerLoading.value = false })
  }
}

function openAddOutlinkPicker() {
  if (props.contentId == null) return
  pickerMode.value = 'outlink'
  pickerVisible.value = true
  const candidates = props.candidateContents?.length ? props.candidateContents : null
  if (candidates) {
    pickerLoading.value = false
    pickerList.value = candidates.filter((c) => c.id !== props.contentId && !outlinks.value.some((o) => o.id === c.id)) as ContentListItem[]
  } else {
    pickerLoading.value = true
    getContentsMe({ page: 1, pageSize: 200 })
      .then((res) => {
        pickerList.value = (res.list ?? []).filter((c) => c.id !== props.contentId && !outlinks.value.some((o) => o.id === c.id))
      })
      .catch(() => { pickerList.value = [] })
      .finally(() => { pickerLoading.value = false })
  }
}

function onPickContent(item: ContentListItem) {
  if (props.contentId == null) return
  const id = item.id
  const title = item.title || '[无标题]'
  if (pickerMode.value === 'backlink') {
    addContentBacklink(props.contentId, id)
      .then(() => {
        pickerVisible.value = false
        load()
        ElMessage.success('已添加入链')
      })
      .catch((e: { message?: string }) => ElMessage.warning(e?.message || '添加失败'))
  } else {
    if (props.canEditBody) {
      emit('insert-outlink', { id, title })
      pickerVisible.value = false
      load()
      ElMessage.success('已添加出链（请保存后生效）')
    } else {
      addContentOutlink(props.contentId, id)
        .then(() => {
          pickerVisible.value = false
          load()
          ElMessage.success('已添加出链')
        })
        .catch((e: { message?: string }) => ElMessage.warning(e?.message || '添加失败'))
    }
  }
}

function onDeleteBacklink(sourceId: number) {
  if (props.contentId == null) return
  deleteContentBacklink(props.contentId, sourceId)
    .then(() => { load(); ElMessage.success('已删除入链') })
    .catch((e: { message?: string }) => ElMessage.warning(e?.message || '删除失败'))
}

function onDeleteOutlink(targetId: number) {
  if (props.contentId == null) return
  if (props.canEditBody) {
    emit('remove-outlink', { id: targetId })
    load()
    ElMessage.success('已移除出链（请保存后生效）')
  } else {
    deleteContentOutlink(props.contentId, targetId)
      .then(() => { load(); ElMessage.success('已删除出链') })
      .catch((e: { message?: string }) => ElMessage.warning(e?.message || '删除失败'))
  }
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
watch(() => props.refreshTrigger, () => { if (props.contentId != null) load() })
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
.link-panel-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.link-panel-section-title {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: #424245;
  letter-spacing: -0.01em;
}
.link-panel-btn-add {
  flex-shrink: 0;
  padding: 4px 10px;
  font-size: 12px;
  color: #007aff;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.link-panel-btn-add:hover {
  background: rgba(0, 122, 255, 0.1);
}
.link-panel-item {
  margin-bottom: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.link-panel-item .link-panel-link {
  flex: 1;
  min-width: 0;
}
.link-panel-btn-del {
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: 11px;
  color: #86868b;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}
.link-panel-btn-del:hover {
  color: #ff3b30;
  background: rgba(255, 59, 48, 0.08);
}
.link-panel-picker-list {
  margin: 0;
  padding: 0;
  list-style: none;
  max-height: 320px;
  overflow-y: auto;
}
.link-panel-picker-item {
  padding: 10px 12px;
  font-size: 14px;
  color: #1d1d1f;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.link-panel-picker-item:hover {
  background: rgba(0, 0, 0, 0.05);
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
