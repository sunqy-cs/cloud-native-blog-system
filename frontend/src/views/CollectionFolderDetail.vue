<template>
  <div class="collection-detail-page">
    <div class="collection-detail-layout">
      <main class="collection-detail-main">
        <div v-if="loading" class="collection-detail-loading">加载中…</div>
        <template v-else-if="folder">
          <header class="collection-detail-header">
            <h1 class="collection-detail-title">{{ folder.name }}</h1>
            <p v-if="folder.description" class="collection-detail-desc">{{ folder.description }}</p>
            <p class="collection-detail-meta">
              {{ folder.count ?? 0 }} 条内容
              <template v-if="folder.createdAt"> · 创建于 {{ folder.createdAt }}</template>
              · <router-link to="/profile" class="collection-detail-link">返回我的收藏</router-link>
              · <button type="button" class="collection-detail-add-btn" @click="openAddDialog">添加博客</button>
              <template v-if="articleList.length > 0">
                <template v-if="!batchRemoveMode">
                  · <button type="button" class="collection-detail-batch-remove-btn" @click="batchRemoveMode = true">批量移除</button>
                </template>
                <template v-else>
                  · <button
                    type="button"
                    class="collection-detail-batch-remove-btn"
                    :disabled="selectedIds.length === 0"
                    @click="batchRemoveFromFolder"
                  >
                    移除选中{{ selectedIds.length > 0 ? ` (${selectedIds.length})` : '' }}
                  </button>
                  · <button type="button" class="collection-detail-cancel-batch-btn" @click="exitBatchRemoveMode">取消</button>
                </template>
              </template>
            </p>
          </header>
          <section class="collection-detail-list-section">
            <div v-if="listLoading" class="collection-detail-loading">加载中…</div>
            <div v-else-if="articleList.length === 0" class="collection-detail-empty">该收藏夹暂无内容</div>
            <div v-else class="collection-article-list">
              <div v-for="item in articleList" :key="item.id" class="collection-article-item-wrap">
                <el-checkbox
                  v-if="batchRemoveMode"
                  :model-value="selectedIds.includes(item.id)"
                  class="collection-article-checkbox"
                  @update:model-value="(v: boolean) => toggleSelect(item.id, v)"
                  @click.stop
                />
                <router-link :to="`/article/${item.id}`" class="collection-article-item">
                  <div class="collection-article-body">
                    <h4 class="collection-article-title">{{ item.title }}</h4>
                    <p v-if="item.summary" class="collection-article-summary">{{ item.summary }}</p>
                    <div class="collection-article-stats">
                      <span class="stat">阅读 {{ formatCount(item.viewCount) }}</span>
                      <span class="stat">赞 {{ formatCount(item.likeCount) }}</span>
                      <span class="stat">收藏 {{ formatCount(item.collectionCount) }}</span>
                      <span class="stat collection-article-time">{{ formatDate(item.publishedAt ?? item.createdAt) }}</span>
                    </div>
                  </div>
                  <div class="collection-article-cover">
                    <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                    <span v-else class="collection-article-cover-ph">{{ item.title.charAt(0) }}</span>
                  </div>
                </router-link>
              </div>
            </div>
            <div v-if="total > 0" class="collection-detail-pagination-wrap">
              <span class="collection-detail-total">共 {{ total }} 条</span>
              <el-pagination
                v-model:current-page="page"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                class="collection-detail-pagination"
              />
            </div>
          </section>
        </template>
        <div v-else class="collection-detail-empty">收藏夹不存在</div>
      </main>
      <aside class="collection-detail-sidebar">
        <CreationCenter />
      </aside>
    </div>

    <el-dialog
      v-model="addDialogVisible"
      title="添加博客到收藏夹"
      width="540px"
      class="add-blog-dialog"
      @opened="onAddDialogOpened"
    >
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
      <ul v-else-if="myArticles.length === 0" class="add-blog-empty">暂无已发布文章，或没有匹配结果</ul>
      <ul v-else class="add-blog-list">
        <li v-for="art in myArticles" :key="art.id" class="add-blog-item">
          <span class="add-blog-title">{{ art.title }}</span>
          <template v-if="idsInFolder.has(art.id)">
            <span class="add-blog-tag">已添加</span>
          </template>
          <el-button
            v-else
            type="primary"
            size="small"
            class="add-blog-btn"
            :loading="addingId === art.id"
            @click="addToFolder(art.id)"
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
import { getCollectionFolderById, getCollectionFolderContents, addContentToCollectionFolder, removeContentFromCollectionFolder, type CollectionFolderItem } from '@/api/collectionFolder'
import { getContentsMe, type ContentListItem } from '@/api/content'
import CreationCenter from '@/components/CreationCenter.vue'

interface ListItem {
  id: number
  title: string
  summary?: string
  cover?: string | null
  viewCount: number
  likeCount: number
  collectionCount: number
  createdAt: string
  publishedAt?: string
}

const route = useRoute()
const folder = ref<CollectionFolderItem | null>(null)
const loading = ref(true)
const articleList = ref<ListItem[]>([])
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
const myArticles = ref<ContentListItem[]>([])
const addingId = ref<number | null>(null)
const idsInFolder = computed(() => new Set(articleList.value.map((a) => a.id)))

function openAddDialog() {
  addDialogVisible.value = true
}
function onAddDialogOpened() {
  addSearchKeyword.value = ''
  loadMyArticles()
}
function onAddSearchInput() {
  if (addSearchTimer.value) clearTimeout(addSearchTimer.value)
  addSearchTimer.value = setTimeout(() => loadMyArticles(), 300)
}
function loadMyArticles() {
  if (!addDialogVisible.value) return
  addDialogLoading.value = true
  const q = addSearchKeyword.value.trim() || undefined
  getContentsMe({ status: 'PUBLISHED', pageSize: 50, page: 1, q })
    .then((res) => {
      myArticles.value = res.list ?? []
    })
    .finally(() => {
      addDialogLoading.value = false
    })
}
function addToFolder(contentId: number) {
  const folderId = Number(route.params.id)
  if (!folderId || !folder.value) return
  addingId.value = contentId
  addContentToCollectionFolder(folderId, contentId)
    .then(() => {
      ElMessage.success('已添加到收藏夹')
      loadFolder()
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

function batchRemoveFromFolder() {
  const folderId = Number(route.params.id)
  if (!folderId || !folder.value || selectedIds.value.length === 0) return
  const ids = [...selectedIds.value]
  Promise.all(ids.map((contentId) => removeContentFromCollectionFolder(folderId, contentId)))
    .then(() => {
      ElMessage.success(`已从收藏夹移除 ${ids.length} 条`)
      selectedIds.value = []
      batchRemoveMode.value = false
      loadFolder()
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

function loadFolder() {
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  loading.value = true
  getCollectionFolderById(id)
    .then((data) => {
      folder.value = data
    })
    .catch(() => {
      folder.value = null
    })
    .finally(() => {
      loading.value = false
    })
}

function loadList() {
  const id = Number(route.params.id)
  if (!id || !folder.value) return
  listLoading.value = true
  getCollectionFolderContents(id, { page: page.value, pageSize: pageSize.value })
    .then((res) => {
      articleList.value = (res.list ?? []) as ListItem[]
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
      folder.value = null
      loadFolder()
    }
  }
)
watch([folder, page], () => {
  if (folder.value) loadList()
})

onMounted(() => {
  loadFolder()
})
</script>

<style scoped>
.collection-detail-page {
  min-height: calc(100vh - 64px);
  background: #f5f5f5;
}
.collection-detail-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
}
.collection-detail-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.collection-detail-loading,
.collection-detail-empty {
  padding: 32px;
  text-align: center;
  color: #666;
}
.collection-detail-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}
.collection-detail-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}
.collection-detail-desc {
  margin: 0 0 8px;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}
.collection-detail-meta {
  margin: 0;
  font-size: 13px;
  color: #999;
}
.collection-detail-link {
  color: #BB1919;
  text-decoration: none;
}
.collection-detail-link:hover {
  text-decoration: underline;
}
.collection-detail-add-btn {
  margin-left: 8px;
  padding: 0 10px;
  font-size: 13px;
  color: #BB1919;
  background: none;
  border: 1px solid #BB1919;
  border-radius: 6px;
  cursor: pointer;
}
.collection-detail-add-btn:hover {
  background: rgba(187, 25, 25, 0.08);
}
.collection-detail-batch-remove-btn {
  margin-left: 8px;
  padding: 0 10px;
  font-size: 13px;
  color: #666;
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}
.collection-detail-batch-remove-btn:hover:not(:disabled) {
  color: #BB1919;
  border-color: #BB1919;
  background: rgba(187, 25, 25, 0.06);
}
.collection-detail-batch-remove-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.collection-detail-selected-hint {
  margin-left: 6px;
  font-size: 13px;
  color: #999;
}
.collection-detail-cancel-batch-btn {
  margin-left: 4px;
  padding: 0 10px;
  font-size: 13px;
  color: #666;
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}
.collection-detail-cancel-batch-btn:hover {
  color: #333;
  border-color: #999;
}

/* 添加博客弹窗：与专栏添加弹窗风格一致 */
.add-blog-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #eee;
  padding: 16px 20px;
}
.add-blog-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 20px;
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

.collection-detail-list-section {
  margin-top: 16px;
}
.collection-article-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.collection-article-item-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.collection-article-item-wrap:last-child {
  border-bottom: none;
}
.collection-article-item-wrap:hover {
  background: #fafafa;
  margin: 0 -24px;
  padding: 0 24px;
}
.collection-article-checkbox {
  flex-shrink: 0;
  margin-right: 0;
}
.collection-article-checkbox :deep(.el-checkbox__inner) {
  border-radius: 4px;
}
.collection-article-item {
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
.collection-article-item-wrap:hover .collection-article-item {
  margin: 0;
  padding-left: 0;
  padding-right: 8px;
}
.collection-article-body {
  flex: 1;
  min-width: 0;
}
.collection-article-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}
.collection-article-summary {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.collection-article-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #999;
}
.collection-article-time {
  margin-left: auto;
}
.collection-article-cover {
  width: 100px;
  height: 70px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #eee;
}
.collection-article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.collection-article-cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 18px;
  color: #999;
  font-weight: 600;
}
.collection-detail-pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.collection-detail-total {
  font-size: 13px;
  color: #666;
}
.collection-detail-sidebar {
  width: 280px;
  flex-shrink: 0;
}
@media (max-width: 900px) {
  .collection-detail-sidebar {
    display: none;
  }
}
</style>
