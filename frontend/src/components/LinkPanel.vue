<template>
  <aside class="link-panel" :class="{ 'link-panel--collapsed': collapsed }">
    <div class="link-panel-header">
      <div class="link-panel-tabs">
        <button
          type="button"
          class="link-panel-tab"
          :class="{ active: activeTab === 'in' }"
          title="入链：链接到当前文件"
          @click="activeTab = 'in'"
        >
          <el-icon><Back /></el-icon>
          <span v-show="!collapsed">入链</span>
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
          <span v-show="!collapsed">出链</span>
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
import { ref, watch } from 'vue'
import { Back, Right, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'
import { getContentBacklinks, getContentOutlinks, type ContentListItem } from '@/api/content'

const props = withDefaults(
  defineProps<{
    contentId: number | null
    /** 是否显示出链（笔记为 true，博客仅入链为 false） */
    showOutlinks?: boolean
  }>(),
  { showOutlinks: true }
)

defineEmits<{ (e: 'open', id: number): void }>()

const collapsed = ref(false)
const activeTab = ref<'in' | 'out'>('in')
const backlinks = ref<ContentListItem[]>([])
const outlinks = ref<ContentListItem[]>([])
const backlinksLoading = ref(false)
const outlinksLoading = ref(false)

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
.link-panel {
  flex-shrink: 0;
  width: 260px;
  border-left: 1px solid #e8e8e8;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
}
.link-panel--collapsed {
  width: 48px;
  min-width: 48px;
}

.link-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 8px 8px 12px;
  border-bottom: 1px solid #eee;
  min-height: 40px;
}
.link-panel-tabs {
  display: flex;
  gap: 4px;
}
.link-panel-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  font-size: 13px;
  color: #666;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.link-panel-tab:hover {
  color: #111;
  background: #eee;
}
.link-panel-tab.active {
  color: #111;
  background: #e8e8e8;
}
.link-panel-tab .el-icon {
  font-size: 16px;
}
.link-panel-fold {
  padding: 4px;
  color: #888;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.link-panel-fold:hover {
  color: #111;
  background: #eee;
}

.link-panel-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}
.link-panel-section-title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
}
.link-panel-loading,
.link-panel-empty {
  margin: 0;
  font-size: 13px;
  color: #999;
}
.link-panel-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.link-panel-item {
  margin-bottom: 4px;
}
.link-panel-link {
  display: block;
  width: 100%;
  padding: 6px 8px;
  font-size: 13px;
  color: #333;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.link-panel-link:hover {
  color: #0d6efd;
  background: #eee;
}
</style>
