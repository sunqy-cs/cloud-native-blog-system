<template>
  <div class="column-detail-page">
    <div class="column-detail-layout">
      <main class="column-detail-main">
        <div v-if="loading" class="column-detail-loading">加载中…</div>
        <template v-else-if="column">
          <header class="column-detail-header">
            <div class="column-detail-cover" :class="{ 'has-img': !!column.cover }">
              <img v-if="column.cover" :src="column.cover" :alt="column.name" />
              <span v-else class="column-detail-cover-ph">{{ column.name.charAt(0) }}</span>
            </div>
            <div class="column-detail-info">
              <h1 class="column-detail-title">{{ column.name }}</h1>
              <p v-if="column.description" class="column-detail-desc">{{ column.description }}</p>
              <p class="column-detail-meta">
                {{ column.articleCount ?? 0 }} 篇内容
                <template v-if="isOwnColumn">
                  · <button type="button" class="column-detail-add-btn" @click="addDialogVisible = true">添加博客</button>
                  <template v-if="articleList.length > 0">
                    <template v-if="!batchRemoveMode">
                      · <button type="button" class="column-detail-batch-remove-btn" @click="batchRemoveMode = true">批量移除</button>
                    </template>
                    <template v-else>
                      · <button
                        type="button"
                        class="column-detail-batch-remove-btn"
                        :disabled="selectedIds.length === 0"
                        @click="batchRemoveFromColumn"
                      >
                        移除选中{{ selectedIds.length > 0 ? ` (${selectedIds.length})` : '' }}
                      </button>
                      · <button type="button" class="column-detail-cancel-batch-btn" @click="exitBatchRemoveMode">取消</button>
                    </template>
                  </template>
                </template>
                <template v-else-if="column.userId">
                  · <router-link :to="{ path: '/blog', query: { userId: column.userId } }" class="column-detail-link">进入 TA 的博客</router-link>
                </template>
              </p>
            </div>
          </header>
          <section class="column-detail-list-section">
            <div v-if="listLoading" class="column-detail-loading">加载中…</div>
            <div v-else-if="articleList.length === 0" class="column-detail-empty">该专栏暂无文章</div>
            <div v-else class="column-article-list">
              <div v-for="item in articleList" :key="item.id" class="column-article-item-wrap">
                <el-checkbox
                  v-if="isOwnColumn && batchRemoveMode"
                  :model-value="selectedIds.includes(item.id)"
                  class="column-article-checkbox"
                  @update:model-value="(v: boolean) => toggleSelect(item.id, v)"
                  @click.stop
                />
                <router-link :to="`/article/${item.id}`" class="column-article-item">
                  <div class="column-article-body">
                    <h4 class="column-article-title">{{ item.title }}</h4>
                    <p v-if="item.summary" class="column-article-summary">{{ item.summary }}</p>
                    <div class="column-article-stats">
                      <span class="stat">阅读 {{ formatCount(item.viewCount) }}</span>
                      <span class="stat">赞 {{ formatCount(item.likeCount) }}</span>
                      <span class="stat">收藏 {{ formatCount(item.collectionCount) }}</span>
                      <span class="stat column-article-time">{{ formatDate(item.publishedAt ?? item.createdAt) }}</span>
                    </div>
                  </div>
                  <div class="column-article-cover">
                    <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                    <span v-else class="column-article-cover-ph">{{ item.title.charAt(0) }}</span>
                  </div>
                </router-link>
              </div>
            </div>
            <div v-if="total > 0" class="column-detail-pagination-wrap">
              <span class="column-detail-total">共 {{ total }} 篇</span>
              <el-pagination
                v-model:current-page="page"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                class="column-detail-pagination"
              />
            </div>
          </section>
        </template>
        <div v-else class="column-detail-empty">专栏不存在</div>
      </main>
      <aside class="column-detail-sidebar">
        <CreationCenter />
      </aside>
    </div>

    <el-dialog
      v-model="addDialogVisible"
      title="添加博客"
      width="540px"
      class="add-blog-dialog"
      @opened="onAddDialogOpened"
    >
      <div class="add-blog-actions">
        <router-link :to="{ path: '/creator/write', query: { columnId: column?.id } }" class="add-blog-write-link">写新文章</router-link>
      </div>
      <div class="add-blog-search-row">
        <el-input
          v-model="addSearchKeyword"
          placeholder="搜索标题或摘要"
          clearable
          class="add-blog-search"
          @input="onAddSearchInput"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div v-if="addDialogLoading" class="add-blog-loading">加载中…</div>
      <ul v-else-if="addMyArticles.length === 0" class="add-blog-empty">暂无已发布文章，或没有匹配结果</ul>
      <ul v-else class="add-blog-list">
        <li v-for="art in addMyArticles" :key="art.id" class="add-blog-item">
          <span class="add-blog-title">{{ art.title }}</span>
          <template v-if="idsInColumn.has(art.id)">
            <span class="add-blog-tag">已添加</span>
          </template>
          <el-button
            v-else
            type="primary"
            size="small"
            class="add-blog-btn"
            :loading="addingId === art.id"
            @click="addToColumn(art.id)"
          >
            添加
          </el-button>
        </li>
      </ul>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getColumnById, addContentToColumn, removeContentFromColumn, type ColumnItem } from '@/api/column'
import { getContentsList, getContentsMe, type ContentListItem } from '@/api/content'
import CreationCenter from '@/components/CreationCenter.vue'

const route = useRoute()
const userStore = useUserStore()
const column = ref<ColumnItem | null>(null)
const isOwnColumn = computed(() => {
  const uid = userStore.userInfo?.id
  const c = column.value
  return uid != null && c?.userId != null && Number(uid) === Number(c.userId)
})
const loading = ref(true)
const articleList = ref<ContentListItem[]>([])
const listLoading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const batchRemoveMode = ref(false)
const selectedIds = ref<number[]>([])
const addDialogVisible = ref(false)
const addSearchKeyword = ref('')
const addSearchTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const addDialogLoading = ref(false)
const addMyArticles = ref<ContentListItem[]>([])
const addingId = ref<number | null>(null)
const idsInColumn = computed(() => new Set(articleList.value.map((a) => a.id)))

function onAddDialogOpened() {
  addSearchKeyword.value = ''
  loadAddMyArticles()
}
function onAddSearchInput() {
  if (addSearchTimer.value) clearTimeout(addSearchTimer.value)
  addSearchTimer.value = setTimeout(() => loadAddMyArticles(), 300)
}
function loadAddMyArticles() {
  if (!addDialogVisible.value) return
  addDialogLoading.value = true
  const q = addSearchKeyword.value.trim() || undefined
  getContentsMe({ status: 'PUBLISHED', pageSize: 50, page: 1, q })
    .then((res) => {
      addMyArticles.value = res.list ?? []
    })
    .finally(() => {
      addDialogLoading.value = false
    })
}
function addToColumn(contentId: number) {
  const columnId = Number(route.params.id)
  if (!columnId || !column.value) return
  addingId.value = contentId
  addContentToColumn(columnId, contentId)
    .then(() => {
      ElMessage.success('已加入专栏')
      loadColumn()
      loadList()
    })
    .catch((e: { message?: string }) => {
      ElMessage.warning(e?.message ?? '添加失败')
    })
    .finally(() => {
      addingId.value = null
    })
}

function exitBatchRemoveMode() {
  batchRemoveMode.value = false
  selectedIds.value = []
}

function toggleSelect(id: number, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value = [...selectedIds.value, id]
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  }
}

function batchRemoveFromColumn() {
  const columnId = Number(route.params.id)
  if (!columnId || !column.value || selectedIds.value.length === 0) return
  const ids = [...selectedIds.value]
  Promise.all(ids.map((contentId) => removeContentFromColumn(columnId, contentId)))
    .then(() => {
      ElMessage.success(`已从专栏移除 ${ids.length} 篇`)
      selectedIds.value = []
      batchRemoveMode.value = false
      loadColumn()
      loadList()
    })
    .catch((e: { message?: string }) => {
      ElMessage.warning(e?.message ?? '移除失败')
    })
}

function formatCount(n: number | undefined) {
  if (n == null) return '0'
  return n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n)
}
function formatDate(iso: string) {
  if (!iso) return '—'
  return iso.replace('T', ' ').slice(0, 16)
}

function loadColumn() {
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  loading.value = true
  getColumnById(id)
    .then((data) => {
      column.value = data
    })
    .catch(() => {
      column.value = null
    })
    .finally(() => {
      loading.value = false
    })
}

function loadList() {
  const id = Number(route.params.id)
  if (!id || !column.value) return
  listLoading.value = true
  getContentsList({
    columnId: id,
    page: page.value,
    pageSize: pageSize.value,
    sortBy: 'time',
    order: 'desc',
  })
    .then((res) => {
      articleList.value = res.list ?? []
      total.value = res.total ?? 0
    })
    .finally(() => {
      listLoading.value = false
    })
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      column.value = null
      loadColumn()
    }
  }
)
watch([column, page], () => {
  if (column.value) loadList()
})

onMounted(() => {
  loadColumn()
})
</script>

<style scoped>
.column-detail-page {
  min-height: calc(100vh - 64px);
  background: #f5f5f5;
}
.column-detail-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
}
.column-detail-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.column-detail-loading,
.column-detail-empty {
  padding: 32px;
  text-align: center;
  color: #666;
}
.column-detail-header {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.column-detail-cover {
  width: 120px;
  height: 84px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #eee;
}
.column-detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.column-detail-cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 28px;
  color: #999;
  font-weight: 600;
}
.column-detail-info {
  flex: 1;
  min-width: 0;
}
.column-detail-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}
.column-detail-desc {
  margin: 0 0 8px;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}
.column-detail-meta {
  margin: 0;
  font-size: 13px;
  color: #999;
}
.column-detail-link {
  color: #BB1919;
  text-decoration: none;
}
.column-detail-link:hover {
  text-decoration: underline;
}
.column-detail-add-btn {
  margin-left: 4px;
  padding: 0 10px;
  font-size: 13px;
  color: #BB1919;
  background: none;
  border: 1px solid #BB1919;
  border-radius: 6px;
  cursor: pointer;
}
.column-detail-add-btn:hover {
  background: rgba(187, 25, 25, 0.08);
}
.column-detail-batch-remove-btn {
  margin-left: 8px;
  padding: 0 10px;
  font-size: 13px;
  color: #666;
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}
.column-detail-batch-remove-btn:hover:not(:disabled) {
  color: #BB1919;
  border-color: #BB1919;
  background: rgba(187, 25, 25, 0.06);
}
.column-detail-batch-remove-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.column-detail-selected-hint {
  margin-left: 6px;
  font-size: 13px;
  color: #999;
}
.column-detail-cancel-batch-btn {
  margin-left: 4px;
  padding: 0 10px;
  font-size: 13px;
  color: #666;
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}
.column-detail-cancel-batch-btn:hover {
  color: #333;
  border-color: #999;
}

/* 添加博客弹窗：与收藏夹添加弹窗风格一致 */
.add-blog-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #eee;
  padding: 16px 20px;
}
.add-blog-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 20px;
}
.add-blog-actions {
  margin-bottom: 12px;
}
.add-blog-write-link {
  display: inline-block;
  padding: 6px 14px;
  font-size: 13px;
  color: #BB1919;
  background: #fff;
  border: 1px solid #BB1919;
  border-radius: 6px;
  text-decoration: none;
}
.add-blog-write-link:hover {
  background: #BB1919;
  color: #fff;
}
.add-blog-search-row {
  margin-bottom: 12px;
}
.add-blog-search {
  width: 100%;
}
.add-blog-dialog :deep(.add-blog-search .el-input__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}
.add-blog-dialog :deep(.add-blog-search .el-input__wrapper:hover),
.add-blog-dialog :deep(.add-blog-search .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.add-blog-loading,
.add-blog-empty {
  padding: 24px;
  text-align: center;
  color: #666;
  font-size: 14px;
}
.add-blog-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 320px;
  overflow-y: auto;
}
.add-blog-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.add-blog-item:last-child {
  border-bottom: none;
}
.add-blog-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.add-blog-tag {
  font-size: 12px;
  color: #999;
}
.add-blog-btn {
  background: #BB1919 !important;
  border-color: #BB1919 !important;
}
.add-blog-btn:hover {
  background: #9e1515 !important;
  border-color: #9e1515 !important;
}

.column-detail-list-section {
  margin-top: 16px;
}
.column-article-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.column-article-item-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.column-article-checkbox {
  flex-shrink: 0;
  margin-right: 0;
}
.column-article-checkbox :deep(.el-checkbox__inner) {
  border-radius: 4px;
}
.column-article-item-wrap:last-child {
  border-bottom: none;
}
.column-article-item-wrap:hover {
  background: #fafafa;
  margin: 0 -24px;
  padding: 0 24px;
}
.column-article-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
  flex: 1;
  min-width: 0;
  text-decoration: none;
  color: inherit;
  border: none;
}
.column-article-item-wrap:hover .column-article-item {
  margin: 0;
  padding-left: 0;
  padding-right: 8px;
}
.column-article-body {
  flex: 1;
  min-width: 0;
}
.column-article-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}
.column-article-summary {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.column-article-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #999;
}
.column-article-time {
  margin-left: auto;
}
.column-article-cover {
  width: 100px;
  height: 70px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #eee;
}
.column-article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.column-article-cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 18px;
  color: #999;
  font-weight: 600;
}
.column-detail-pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.column-detail-total {
  font-size: 13px;
  color: #666;
}
.column-detail-sidebar {
  width: 280px;
  flex-shrink: 0;
}
@media (max-width: 900px) {
  .column-detail-sidebar {
    display: none;
  }
}
</style>
