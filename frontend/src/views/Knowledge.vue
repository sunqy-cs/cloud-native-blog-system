<template>
  <div class="knowledge-page">
    <!-- 左侧：知识库导航栏（顶栏下方固定，可展开/收起） -->
    <aside class="knowledge-sidebar" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-sidebar-inner">
        <!-- 折叠标：放在导航栏最上面 -->
        <button
          type="button"
          class="knowledge-collapse-btn"
          :title="sidebarExpanded ? '收起' : '展开'"
          @click="sidebarExpanded = !sidebarExpanded"
        >
          <span class="knowledge-collapse-icon" :class="{ expanded: sidebarExpanded }">
            <span class="collapse-panel collapse-panel-left">
              <span class="collapse-line"></span>
              <span class="collapse-line"></span>
            </span>
            <span class="collapse-panel collapse-panel-right"></span>
          </span>
        </button>

        <!-- 1. 知识库（当前选中） -->
        <router-link to="/knowledge" class="knowledge-nav-item active" title="知识库">
          <el-icon><FolderOpened /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">知识库</span>
        </router-link>
        <!-- 2. 搜索 -->
        <router-link to="/knowledge" class="knowledge-nav-item" title="搜索">
          <el-icon><Search /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">搜索</span>
        </router-link>
        <!-- 3. 收藏 -->
        <a href="#" class="knowledge-nav-item" title="收藏">
          <el-icon><Star /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">收藏</span>
        </a>

        <div class="knowledge-nav-spacer"></div>
      </div>
    </aside>

    <!-- 中间偏左：知识库内容边栏（标题、热门、搜索、我的知识库、我的订阅） -->
    <aside class="knowledge-library-sidebar" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-library-inner">
        <h1 class="knowledge-page-title">知识库</h1>
        <router-link to="/knowledge/square" class="knowledge-hot-tab">
          <el-icon class="knowledge-hot-tab-icon"><FolderOpened /></el-icon>
          <span>热门知识库</span>
        </router-link>
        <div class="knowledge-main-divider" />
        <div class="knowledge-search-row">
          <div class="knowledge-search-inner">
            <el-icon class="knowledge-search-icon"><Search /></el-icon>
            <input
              v-model="searchKeyword"
              type="text"
              class="knowledge-search-input"
              placeholder="搜索知识库/文件"
              autocomplete="off"
            />
          </div>
          <el-dropdown trigger="click" placement="bottom-end" popper-class="knowledge-add-dropdown-bbc" @command="onAddDropdownCommand">
            <button type="button" class="knowledge-add-btn" title="添加">
              <el-icon><Plus /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="newKb">新建知识库</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <section class="knowledge-section">
          <h2 class="knowledge-section-title">我的知识库</h2>
          <ul class="knowledge-my-list">
            <li
              v-for="kb in myKnowledgeBases"
              :key="kb.id"
              class="knowledge-my-item"
              :class="{ active: selectedKb?.id === kb.id && selectedKbSource === 'mine' }"
              @click="selectedKb = { ...kb }; selectedKbSource = 'mine'"
            >
              <el-icon class="knowledge-my-icon"><Reading /></el-icon>
              <span class="knowledge-my-name">{{ kb.name || '未命名' }}</span>
            </li>
          </ul>
        </section>
        <section class="knowledge-section">
          <h2 class="knowledge-section-title">我的订阅</h2>
          <ul class="knowledge-my-list">
            <li
              v-for="sub in mySubscriptions"
              :key="sub.id"
              class="knowledge-my-item"
              :class="{ active: selectedKb?.id === sub.id && selectedKbSource === 'sub' }"
              @click="selectedKb = { ...sub }; selectedKbSource = 'sub'"
            >
              <el-icon class="knowledge-my-icon"><Reading /></el-icon>
              <span class="knowledge-my-name">{{ sub.name || '未命名' }}</span>
            </li>
          </ul>
        </section>
      </div>
    </aside>

    <!-- 右侧：知识库详情边栏（点击某个知识库时显示） -->
    <aside
      v-show="selectedKb"
      class="knowledge-detail-sidebar"
      :class="{ expanded: sidebarExpanded }"
    >
      <div class="knowledge-detail-inner">
        <div class="knowledge-detail-header">
          <div class="knowledge-detail-cover-row">
            <div v-if="selectedKb?.cover" class="knowledge-detail-cover-wrap">
              <img :src="selectedKb.cover" :alt="selectedKb.name" class="knowledge-detail-cover" />
            </div>
            <div v-else class="knowledge-detail-cover-placeholder">
              <el-icon><FolderOpened /></el-icon>
              <span>暂无封面</span>
            </div>
            <div class="knowledge-detail-cover-side">
              <h2 class="knowledge-detail-title">{{ selectedKb?.name || '未命名' }}</h2>
              <div class="knowledge-detail-author">
                <img
                  v-if="selectedKb?.ownerAvatar"
                  :src="selectedKb.ownerAvatar"
                  :alt="selectedKb.ownerName"
                  class="knowledge-detail-author-avatar"
                />
                <span v-else class="knowledge-detail-author-avatar-ph">{{ (selectedKb?.ownerName || '我').charAt(0) }}</span>
                <span class="knowledge-detail-author-name">{{ selectedKb?.ownerName || '我' }}</span>
              </div>
              <div class="knowledge-detail-stats">
                {{ selectedKb?.subCount ?? 0 }} 订阅 · {{ selectedKb?.contentCount ?? detailContents.length }} 内容
              </div>
              <span class="knowledge-detail-visibility">{{ selectedKb?.visibility === 'PRIVATE' ? '私有' : '公开' }}</span>
            </div>
          </div>
          <div class="knowledge-detail-header-actions">
            <template v-if="isOwnDetail && !isDefaultKb">
              <button type="button" class="knowledge-detail-btn small" title="编辑" @click="openEditKbDialog">编辑</button>
              <button type="button" class="knowledge-detail-btn small danger" title="删除该知识库" @click="deleteKb">删除</button>
            </template>
            <button type="button" class="knowledge-detail-close" title="关闭" @click="selectedKb = null; selectedKbSource = null">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>
        <dl v-if="selectedKb?.description" class="knowledge-detail-meta">
          <dt>简介</dt>
          <dd>{{ selectedKb.description }}</dd>
        </dl>

        <!-- 操作：自己的显示添加/批量删除，别人的显示订阅 -->
        <div class="knowledge-detail-actions">
          <template v-if="isOwnDetail">
            <button type="button" class="knowledge-detail-btn primary" @click="openAddContentDialog">添加</button>
            <template v-if="detailContents.length > 0">
              <template v-if="!detailBatchMode">
                <button type="button" class="knowledge-detail-btn" @click="detailBatchMode = true">批量删除</button>
              </template>
              <template v-else>
                <button
                  type="button"
                  class="knowledge-detail-btn"
                  :disabled="detailSelectedIds.length === 0"
                  @click="batchRemoveFromKb"
                >
                  删除选中{{ detailSelectedIds.length > 0 ? ` (${detailSelectedIds.length})` : '' }}
                </button>
                <button type="button" class="knowledge-detail-btn" @click="exitDetailBatchMode">取消</button>
              </template>
            </template>
          </template>
          <template v-else>
            <button
              type="button"
              class="knowledge-detail-btn primary"
              :class="{ subscribed: isDetailSubscribed }"
              @click="toggleDetailSubscribe"
            >
              {{ isDetailSubscribed ? '已订阅' : '订阅' }}
            </button>
          </template>
        </div>

        <!-- 收录的文章 -->
        <h3 class="knowledge-detail-list-title">收录的文章</h3>
        <div v-if="detailContents.length === 0" class="knowledge-detail-empty">暂无收录</div>
        <ul v-else class="knowledge-detail-article-list">
          <li v-for="art in detailContents" :key="art.id" class="knowledge-detail-article-item">
            <el-checkbox
              v-if="isOwnDetail && detailBatchMode"
              :model-value="detailSelectedIds.includes(art.id)"
              class="knowledge-detail-article-checkbox"
              @update:model-value="(v: boolean) => toggleDetailSelect(art.id, v)"
              @click.stop
            />
            <router-link :to="`/article/${art.id}`" class="knowledge-detail-article-link">{{ art.title }}</router-link>
            <el-dropdown
              v-if="isOwnDetail && !detailBatchMode"
              trigger="click"
              placement="bottom-end"
              popper-class="knowledge-article-dropdown-bbc"
              @command="(cmd: string) => removeContentFromKb(Number(cmd))"
            >
              <button type="button" class="knowledge-detail-article-more" title="更多" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="String(art.id)" class="knowledge-article-dropdown-danger">删除这篇博客</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </li>
        </ul>
      </div>
    </aside>

    <!-- 添加文章到知识库 弹窗 -->
    <el-dialog
      v-model="addContentDialogVisible"
      title="添加文章到知识库"
      width="480px"
      class="knowledge-add-dialog"
    >
      <div class="knowledge-add-search-row">
        <el-input
          v-model="addContentKeyword"
          placeholder="搜索标题或摘要"
          clearable
          size="small"
          @input="onAddContentSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <ul v-if="addContentCandidates.length === 0" class="knowledge-add-empty">暂无已发布文章或没有匹配结果</ul>
      <ul v-else class="knowledge-add-list">
        <li v-for="art in addContentCandidates" :key="art.id" class="knowledge-add-item">
          <span class="knowledge-add-title">{{ art.title }}</span>
          <span v-if="detailContentIds.has(art.id)" class="knowledge-add-tag">已收录</span>
          <el-button
            v-else
            type="primary"
            size="small"
            :loading="addingContentId === art.id"
            @click="addContentToKb(art.id)"
          >
            添加
          </el-button>
        </li>
      </ul>
    </el-dialog>

    <!-- 编辑知识库 弹窗 -->
    <el-dialog
      v-model="editKbDialogVisible"
      title="编辑知识库"
      width="440px"
      class="knowledge-create-dialog knowledge-create-dialog--bbc"
      @closed="editKbCoverInputRef = null"
    >
      <div class="knowledge-create-form">
        <div class="knowledge-create-field">
          <label>封面（选填）</label>
          <div
            class="knowledge-create-cover"
            :class="{ 'has-cover': !!editKbCover, uploading: editKbCoverUploading }"
            @click="!editKbCoverUploading && triggerEditKbCoverSelect()"
          >
            <template v-if="editKbCoverUploading">
              <el-icon class="knowledge-create-cover-loading"><Loading /></el-icon>
              <span>上传中…</span>
            </template>
            <template v-else-if="editKbCover">
              <img :src="editKbCover" alt="封面" class="knowledge-create-cover-img" />
              <button type="button" class="knowledge-create-cover-remove" @click.stop="editKbCover = ''">移除封面</button>
            </template>
            <template v-else>
              <el-icon class="knowledge-create-cover-plus"><Plus /></el-icon>
              <span>上传封面图片</span>
            </template>
          </div>
          <input
            ref="editKbCoverInputRef"
            type="file"
            accept="image/*"
            class="knowledge-create-cover-input"
            @change="onEditKbCoverFileChange"
          />
        </div>
        <div class="knowledge-create-field">
          <label>名称</label>
          <el-input v-model="editKbName" placeholder="请输入知识库名称" maxlength="128" show-word-limit clearable />
        </div>
        <div class="knowledge-create-field">
          <label>简介</label>
          <el-input
            v-model="editKbDescription"
            type="textarea"
            placeholder="选填"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </div>
        <div class="knowledge-create-field">
          <label>可见性</label>
          <el-radio-group v-model="editKbVisibility">
            <el-radio value="PRIVATE">私有</el-radio>
            <el-radio value="PUBLIC">公开</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <span class="knowledge-create-footer">
          <el-button @click="editKbDialogVisible = false">取消</el-button>
          <el-button type="primary" class="knowledge-create-submit" @click="submitEditKb">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 新建知识库 弹窗（BBC 风格） -->
    <el-dialog
      v-model="createKbDialogVisible"
      title="新建知识库"
      width="440px"
      class="knowledge-create-dialog knowledge-create-dialog--bbc"
      @closed="createKbName = ''; createKbDescription = ''; createKbVisibility = 'PRIVATE'; createKbCover = ''"
    >
      <div class="knowledge-create-form">
        <div class="knowledge-create-field">
          <label>封面（选填）</label>
          <div
            class="knowledge-create-cover"
            :class="{ 'has-cover': !!createKbCover, uploading: createKbCoverUploading }"
            @click="!createKbCoverUploading && triggerCreateKbCoverSelect()"
          >
            <template v-if="createKbCoverUploading">
              <el-icon class="knowledge-create-cover-loading"><Loading /></el-icon>
              <span>上传中…</span>
            </template>
            <template v-else-if="createKbCover">
              <img :src="createKbCover" alt="封面" class="knowledge-create-cover-img" />
              <button type="button" class="knowledge-create-cover-remove" @click.stop="createKbCover = ''">移除封面</button>
            </template>
            <template v-else>
              <el-icon class="knowledge-create-cover-plus"><Plus /></el-icon>
              <span>上传封面图片</span>
            </template>
          </div>
          <input
            ref="createKbCoverInputRef"
            type="file"
            accept="image/*"
            class="knowledge-create-cover-input"
            @change="onCreateKbCoverFileChange"
          />
        </div>
        <div class="knowledge-create-field">
          <label>名称</label>
          <el-input v-model="createKbName" placeholder="请输入知识库名称" maxlength="128" show-word-limit clearable />
        </div>
        <div class="knowledge-create-field">
          <label>简介</label>
          <el-input
            v-model="createKbDescription"
            type="textarea"
            placeholder="选填"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </div>
        <div class="knowledge-create-field">
          <label>可见性</label>
          <el-radio-group v-model="createKbVisibility">
            <el-radio value="PRIVATE">私有</el-radio>
            <el-radio value="PUBLIC">公开</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <span class="knowledge-create-footer">
          <el-button @click="createKbDialogVisible = false">取消</el-button>
          <el-button type="primary" class="knowledge-create-submit" @click="submitCreateKb">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 中间：大片留白主内容区 -->
    <main class="knowledge-main" :class="{ expanded: sidebarExpanded }" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Search, FolderOpened, Star, Plus, Reading, Close, Delete, Loading, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'

interface KnowledgeBaseItem {
  id: string
  name: string
  cover?: string
  description?: string
  visibility?: 'PRIVATE' | 'PUBLIC'
  ownerAvatar?: string
  ownerName?: string
  subCount?: number
  contentCount?: number
}

interface DetailContentItem {
  id: number
  title: string
  summary?: string
}

const sidebarExpanded = ref(false)
const searchKeyword = ref('')
const myKnowledgeBases = ref<KnowledgeBaseItem[]>([
  { id: 'default', name: '默认知识库', description: '默认创建的知识库，可在此收录文章与文件。', visibility: 'PRIVATE' },
])
const mySubscriptions = ref<KnowledgeBaseItem[]>([])
const selectedKb = ref<KnowledgeBaseItem | null>(null)
const selectedKbSource = ref<'mine' | 'sub' | null>(null)

const isOwnDetail = computed(() => selectedKbSource.value === 'mine')
const isDefaultKb = computed(() => selectedKb.value?.id === 'default')
const detailContents = ref<DetailContentItem[]>([])
const detailBatchMode = ref(false)
const detailSelectedIds = ref<number[]>([])
const subscribedDetailKbId = ref<string | null>(null)
const isDetailSubscribed = computed(() => selectedKb.value != null && subscribedDetailKbId.value === selectedKb.value.id)

const addContentDialogVisible = ref(false)
const addContentKeyword = ref('')
const addContentCandidates = ref<DetailContentItem[]>([])
const addingContentId = ref<number | null>(null)
const detailContentIds = computed(() => new Set(detailContents.value.map((c) => c.id)))

watch(selectedKb, (kb) => {
  if (!kb) {
    detailContents.value = []
    detailBatchMode.value = false
    detailSelectedIds.value = []
    return
  }
  detailBatchMode.value = false
  detailSelectedIds.value = []
  loadDetailContents(kb.id)
}, { immediate: true })

function loadDetailContents(kbId: string) {
  if (kbId === 'default') {
    detailContents.value = []
    return
  }
  detailContents.value = []
}

function openAddContentDialog() {
  addContentDialogVisible.value = true
  addContentKeyword.value = ''
  addContentCandidates.value = []
}

function onAddContentSearch() {
  addContentCandidates.value = []
}

function toggleDetailSelect(id: number, checked: boolean) {
  if (checked) detailSelectedIds.value = [...detailSelectedIds.value, id]
  else detailSelectedIds.value = detailSelectedIds.value.filter((x) => x !== id)
}

function exitDetailBatchMode() {
  detailBatchMode.value = false
  detailSelectedIds.value = []
}

function batchRemoveFromKb() {
  const ids = detailSelectedIds.value
  detailContents.value = detailContents.value.filter((c) => !ids.includes(c.id))
  detailSelectedIds.value = []
  detailBatchMode.value = false
  ElMessage.success('已移除')
}

function removeContentFromKb(id: number) {
  detailContents.value = detailContents.value.filter((c) => c.id !== id)
  ElMessage.success('已移除')
}

function deleteKb() {
  if (!selectedKb.value || selectedKb.value.id === 'default') return
  myKnowledgeBases.value = myKnowledgeBases.value.filter((kb) => kb.id !== selectedKb.value!.id)
  selectedKb.value = null
  selectedKbSource.value = null
  ElMessage.success('已删除该知识库')
}

const editKbDialogVisible = ref(false)
const editKbName = ref('')
const editKbDescription = ref('')
const editKbVisibility = ref<'PRIVATE' | 'PUBLIC'>('PRIVATE')
const editKbCover = ref('')
const editKbCoverUploading = ref(false)
const editKbCoverInputRef = ref<HTMLInputElement | null>(null)

function openEditKbDialog() {
  if (!selectedKb.value) return
  editKbName.value = selectedKb.value.name
  editKbDescription.value = selectedKb.value.description ?? ''
  editKbVisibility.value = selectedKb.value.visibility ?? 'PRIVATE'
  editKbCover.value = selectedKb.value.cover ?? ''
  editKbDialogVisible.value = true
}

function triggerEditKbCoverSelect() {
  editKbCoverInputRef.value?.click()
}

async function onEditKbCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  editKbCoverUploading.value = true
  try {
    const meta = await uploadImage(file, 'covers')
    if (meta?.url) {
      editKbCover.value = meta.url
      ElMessage.success('封面上传成功')
    }
  } finally {
    editKbCoverUploading.value = false
  }
}

function submitEditKb() {
  const name = editKbName.value?.trim()
  if (!name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  if (!selectedKb.value) return
  const id = selectedKb.value.id
  const idx = myKnowledgeBases.value.findIndex((kb) => kb.id === id)
  if (idx === -1) return
  const updated = {
    ...myKnowledgeBases.value[idx],
    name,
    description: editKbDescription.value?.trim() || undefined,
    visibility: editKbVisibility.value,
    cover: editKbCover.value || undefined,
  }
  myKnowledgeBases.value = myKnowledgeBases.value.slice()
  myKnowledgeBases.value[idx] = updated
  selectedKb.value = updated
  editKbDialogVisible.value = false
  ElMessage.success('已保存')
}

function toggleDetailSubscribe() {
  if (!selectedKb.value) return
  if (subscribedDetailKbId.value === selectedKb.value.id) {
    subscribedDetailKbId.value = null
    ElMessage.success('已取消订阅')
  } else {
    subscribedDetailKbId.value = selectedKb.value.id
    ElMessage.success('已订阅')
  }
}

function addContentToKb(id: number) {
  addingContentId.value = id
  setTimeout(() => {
    const art = addContentCandidates.value.find((a) => a.id === id)
    if (art) {
      detailContents.value = [...detailContents.value, art]
      ElMessage.success('已添加')
    }
    addingContentId.value = null
  }, 300)
}

function onAddDropdownCommand(command: string) {
  if (command === 'newKb') {
    createKbDialogVisible.value = true
    createKbName.value = ''
    createKbDescription.value = ''
    createKbVisibility.value = 'PRIVATE'
  }
}

const createKbDialogVisible = ref(false)
const createKbName = ref('')
const createKbDescription = ref('')
const createKbVisibility = ref<'PRIVATE' | 'PUBLIC'>('PRIVATE')
const createKbCover = ref('')
const createKbCoverUploading = ref(false)
const createKbCoverInputRef = ref<HTMLInputElement | null>(null)

function triggerCreateKbCoverSelect() {
  createKbCoverInputRef.value?.click()
}

async function onCreateKbCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  createKbCoverUploading.value = true
  try {
    const meta = await uploadImage(file, 'covers')
    if (meta?.url) {
      createKbCover.value = meta.url
      ElMessage.success('封面上传成功')
    }
  } finally {
    createKbCoverUploading.value = false
  }
}

function submitCreateKb() {
  const name = createKbName.value?.trim()
  if (!name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  const id = 'kb-' + Date.now()
  myKnowledgeBases.value = [
    ...myKnowledgeBases.value,
    {
      id,
      name,
      cover: createKbCover.value || undefined,
      description: createKbDescription.value?.trim() || undefined,
      visibility: createKbVisibility.value,
      ownerName: '我',
      subCount: 0,
    },
  ]
  createKbDialogVisible.value = false
  ElMessage.success('已创建')
}
</script>

<style scoped>
.knowledge-page {
  display: flex;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  overflow: hidden;
  background: #fff;
}

/* 左侧导航栏：顶栏下方固定，可展开/收起 */
.knowledge-sidebar {
  position: fixed;
  top: 64px;
  left: 0;
  bottom: 0;
  width: 72px;
  background: #fff;
  border-right: 1px solid #eee;
  z-index: 100;
  transition: width 0.2s ease;
}

.knowledge-sidebar.expanded {
  width: 200px;
}

.knowledge-sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 12px 14px 12px 14px;
  min-width: 72px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-sidebar-inner {
  padding-left: 14px;
  padding-right: 14px;
  align-items: center;
}

.knowledge-sidebar.expanded .knowledge-sidebar-inner {
  padding-left: 14px;
  padding-right: 14px;
}

/* 统一：所有导航项同高、同内边距、同图标尺寸，左对齐 */
.knowledge-nav-item {
  height: 44px;
  min-width: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 12px;
  color: #555;
  text-decoration: none;
  transition: color 0.2s, background 0.2s;
  margin-bottom: 2px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-nav-item {
  justify-content: center;
  padding: 0;
}

.knowledge-nav-item:hover {
  color: #111;
  background: #f0f0f0;
}

.knowledge-nav-item.active {
  color: #111;
  background: #f0f0f0;
  font-weight: 500;
}

.knowledge-nav-item .el-icon {
  font-size: 20px;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
}

.knowledge-nav-text {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
}

.knowledge-nav-spacer {
  flex: 1;
  min-height: 12px;
}

/* 折叠标：与导航项同宽同高，对齐 */
.knowledge-collapse-btn {
  width: 44px;
  height: 44px;
  margin-bottom: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
}

.knowledge-sidebar:not(.expanded) .knowledge-collapse-btn {
  width: 44px;
}

.knowledge-sidebar.expanded .knowledge-collapse-btn {
  width: 100%;
  min-width: 0;
  justify-content: flex-start;
  padding-left: 12px;
}

.knowledge-collapse-btn:hover {
  background: #f5f5f5;
}

/* 展开/折叠 icon：两栏并排，左侧带两条横线（打开的书/分栏） */
.knowledge-collapse-icon {
  position: relative;
  width: 20px;
  height: 14px;
  display: flex;
  border: 1.5px solid #555;
  border-radius: 3px;
  box-sizing: border-box;
  overflow: hidden;
}

.knowledge-collapse-btn:hover .knowledge-collapse-icon {
  border-color: #333;
}

.collapse-panel {
  box-sizing: border-box;
}

.collapse-panel-left {
  width: 7px;
  flex-shrink: 0;
  border-right: 1.5px solid #555;
  position: relative;
  background: #fff;
}

.knowledge-collapse-btn:hover .collapse-panel-left {
  border-color: #333;
}

.collapse-panel-left .collapse-line {
  position: absolute;
  left: 2px;
  width: 2px;
  height: 1px;
  background: #555;
  border-radius: 0.5px;
}

.knowledge-collapse-btn:hover .collapse-line {
  background: #333;
}

.collapse-panel-left .collapse-line:first-child {
  top: 4px;
}

.collapse-panel-left .collapse-line:last-child {
  top: 8px;
}

.collapse-panel-right {
  flex: 1;
  min-width: 0;
  background: #fff;
}

/* 展开时图标左右对调，表示“可收起” */
.knowledge-collapse-icon.expanded {
  flex-direction: row-reverse;
}

.knowledge-collapse-icon.expanded .collapse-panel-left {
  border-right: none;
  border-left: 1.5px solid #555;
}

.knowledge-collapse-btn:hover .knowledge-collapse-icon.expanded .collapse-panel-left {
  border-left-color: #333;
}

/* 知识库内容边栏（仅此栏内容多时可内部滚动） */
.knowledge-library-sidebar {
  width: 260px;
  flex-shrink: 0;
  margin-left: 72px;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-right: 1px solid #eee;
  overflow-y: auto;
  overflow-x: hidden;
  transition: margin-left 0.2s ease;
}

.knowledge-library-sidebar.expanded {
  margin-left: 200px;
}

.knowledge-library-inner {
  padding: 20px 18px 32px;
  box-sizing: border-box;
  min-height: min-content;
}

/* 右侧知识库详情边栏（点击某项时显示） */
.knowledge-detail-sidebar {
  width: 280px;
  flex-shrink: 0;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-right: 1px solid #eee;
  overflow-y: auto;
  overflow-x: hidden;
}

.knowledge-detail-inner {
  padding: 20px 18px 32px;
  box-sizing: border-box;
}

.knowledge-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.knowledge-detail-cover-row {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.knowledge-detail-cover-side {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
}

.knowledge-detail-cover-side .knowledge-detail-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #111;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.knowledge-detail-visibility {
  display: inline-block;
  font-size: 11px;
  color: #BB1919;
  background: rgba(187, 25, 25, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}

.knowledge-detail-author {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.knowledge-detail-author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}

.knowledge-detail-author-avatar-ph {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #666;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.knowledge-detail-author-name {
  font-size: 12px;
  color: #333;
}

.knowledge-detail-stats {
  font-size: 11px;
  color: #888;
  white-space: nowrap;
  flex-shrink: 0;
}

.knowledge-detail-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.knowledge-detail-btn.small {
  padding: 4px 10px;
  font-size: 12px;
}

.knowledge-detail-btn.danger {
  color: #BB1919;
  border-color: #BB1919;
  background: #fff;
}

.knowledge-detail-btn.danger:hover {
  background: #fff8f8;
  border-color: #9e1515;
  color: #9e1515;
}

.knowledge-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}

.knowledge-detail-close {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.knowledge-detail-close:hover {
  background: #f0f0f0;
  color: #111;
}

.knowledge-detail-cover-wrap {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.knowledge-detail-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-detail-cover-placeholder {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  background: #f0eef5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #888;
  font-size: 11px;
  flex-shrink: 0;
}

.knowledge-detail-cover-placeholder .el-icon {
  font-size: 24px;
}

.knowledge-detail-meta {
  margin: 0 0 16px;
}

.knowledge-detail-meta:last-child {
  margin-bottom: 0;
}

.knowledge-detail-meta dt {
  font-size: 12px;
  color: #888;
  margin: 0 0 6px;
  font-weight: 500;
}

.knowledge-detail-meta dd {
  font-size: 14px;
  color: #333;
  margin: 0;
  line-height: 1.5;
}

.knowledge-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  margin-bottom: 16px;
}

.knowledge-detail-btn {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.knowledge-detail-btn:hover:not(:disabled) {
  background: #f5f5f5;
  border-color: #ccc;
}

.knowledge-detail-btn.primary {
  background: #BB1919;
  border-color: #BB1919;
  color: #fff;
}

.knowledge-detail-btn.primary:hover:not(:disabled) {
  background: #9e1515;
  border-color: #9e1515;
}

.knowledge-detail-btn.primary.subscribed {
  background: #f0f0f0;
  border-color: #ddd;
  color: #666;
}

.knowledge-detail-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.knowledge-detail-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  margin: 0 0 12px;
}

.knowledge-detail-empty {
  font-size: 13px;
  color: #888;
  padding: 16px 0;
}

.knowledge-detail-article-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.knowledge-detail-article-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background 0.2s;
}

.knowledge-detail-article-item:hover {
  background: #f5f5f5;
}

.knowledge-detail-article-checkbox {
  flex-shrink: 0;
}

.knowledge-detail-article-link {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #333;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-detail-article-link:hover {
  color: #BB1919;
}

.knowledge-detail-article-remove {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.knowledge-detail-article-remove:hover {
  color: #BB1919;
  background: #fafafa;
}

.knowledge-detail-article-more {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.knowledge-detail-article-more:hover {
  color: #333;
  background: #f0f0f0;
}

.knowledge-add-dialog .knowledge-add-search-row {
  margin-bottom: 12px;
}

.knowledge-add-empty {
  font-size: 13px;
  color: #888;
  padding: 24px 0;
  margin: 0;
}

.knowledge-add-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 320px;
  overflow-y: auto;
}

.knowledge-add-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.knowledge-add-item:last-child {
  border-bottom: none;
}

.knowledge-add-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-add-tag {
  font-size: 12px;
  color: #888;
}

/* 新建知识库弹窗：BBC 风格（主色 #BB1919、输入框焦点、按钮） */
.knowledge-create-dialog--bbc:deep(.el-dialog__header) {
  color: #111;
  border-bottom: 1px solid #eee;
}
.knowledge-create-dialog--bbc:deep(.el-dialog__body) {
  --el-color-primary: #BB1919;
}
.knowledge-create-dialog--bbc:deep(.el-input__wrapper),
.knowledge-create-dialog--bbc:deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}
.knowledge-create-dialog--bbc:deep(.el-input__wrapper:hover),
.knowledge-create-dialog--bbc:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.knowledge-create-dialog--bbc:deep(.el-input__wrapper:focus-within),
.knowledge-create-dialog--bbc:deep(.el-textarea__inner:focus-within) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.knowledge-create-dialog--bbc:deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #BB1919 !important;
  background: #BB1919 !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__input.is-checked .el-radio__inner::after) {
  background-color: #fff !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__inner:hover) {
  border-color: #BB1919 !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #BB1919 !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__label) {
  color: #333;
}

.knowledge-create-form .knowledge-create-field {
  margin-bottom: 16px;
}

.knowledge-create-form .knowledge-create-field:last-child {
  margin-bottom: 0;
}

.knowledge-create-form .knowledge-create-field label {
  display: block;
  font-size: 13px;
  color: #333;
  margin-bottom: 6px;
}

.knowledge-create-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  max-height: 160px;
  border-radius: 8px;
  border: 1px dashed #ddd;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #888;
  font-size: 13px;
  transition: border-color 0.2s, background 0.2s;
  overflow: hidden;
  position: relative;
}

.knowledge-create-cover:hover {
  border-color: #BB1919;
  background: #fff8f8;
  color: #BB1919;
}

.knowledge-create-cover.uploading {
  cursor: not-allowed;
  border-color: #BB1919;
}

.knowledge-create-cover-plus {
  font-size: 28px;
}

.knowledge-create-cover-loading {
  font-size: 24px;
  color: #BB1919;
}

.knowledge-create-cover.has-cover {
  padding: 0;
  border-style: solid;
}

.knowledge-create-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-create-cover-remove {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 4px 10px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}

.knowledge-create-cover-remove:hover {
  background: rgba(0, 0, 0, 0.7);
}

.knowledge-create-cover-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.knowledge-create-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.knowledge-create-submit {
  background: #BB1919 !important;
  border-color: #BB1919 !important;
}
.knowledge-create-submit:hover {
  background: #9e1515 !important;
  border-color: #9e1515 !important;
}

/* 中间大片留白主内容区 */
.knowledge-main {
  flex: 1;
  min-height: 0;
  height: calc(100vh - 64px);
  background: #fff;
  min-width: 0;
  overflow: hidden;
}

.knowledge-page-title {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin: 0 0 24px;
  line-height: 1.3;
}

.knowledge-hot-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 0;
  border-radius: 10px;
  background: #f0eef5;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s, color 0.2s;
}

.knowledge-hot-tab:hover {
  background: #e5e0f0;
  color: #111;
}

.knowledge-hot-tab-icon {
  font-size: 18px;
}

.knowledge-main-divider {
  height: 1px;
  background: #ddd;
  margin: 24px 0;
  border: none;
  flex-shrink: 0;
}

.knowledge-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 0;
  margin-bottom: 28px;
}

.knowledge-search-inner {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  transition: border-color 0.2s;
}

.knowledge-search-inner:focus-within {
  border-color: #999;
}

.knowledge-search-icon {
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
}

.knowledge-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  font-size: 12px;
  color: #111;
  background: transparent;
}

.knowledge-search-input::placeholder {
  color: #999;
}

.knowledge-add-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.knowledge-add-btn:hover {
  background: #f5f5f5;
  color: #111;
  border-color: #ddd;
}

.knowledge-add-btn .el-icon {
  font-size: 16px;
}

.knowledge-section {
  margin-top: 0;
  margin-bottom: 28px;
}

.knowledge-section:last-child {
  margin-bottom: 0;
}

.knowledge-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  margin: 0 0 12px;
}

.knowledge-my-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.knowledge-my-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background 0.2s;
  cursor: pointer;
}

.knowledge-my-item:hover {
  background: #f5f5f5;
}

.knowledge-my-item.active {
  background: #f0eef5;
  color: #333;
}

.knowledge-my-icon {
  font-size: 16px;
  color: #409eff;
  flex-shrink: 0;
}

.knowledge-my-name {
  font-size: 13px;
  color: #333;
}

.knowledge-input-wrap {
  margin-bottom: 56px;
}

.knowledge-input-box {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px 20px 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.knowledge-input-box:focus-within {
  border-color: #BB1919;
  box-shadow: 0 0 0 2px rgba(187, 25, 25, 0.12);
}

.knowledge-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 16px;
  line-height: 1.5;
  color: #111;
  background: transparent;
  display: block;
  margin-bottom: 12px;
}

.knowledge-input::placeholder {
  color: #999;
}

.knowledge-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-input-opt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #888;
  cursor: pointer;
}

.knowledge-input-opt:hover {
  color: #111;
}

.knowledge-input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.knowledge-input-action {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
}

.knowledge-input-action:hover {
  color: #BB1919;
  background: #fafafa;
}

.knowledge-input-send {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #BB1919;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.knowledge-input-send:hover {
  background: #9e1515;
}

/* 两列 */
.knowledge-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
}

.knowledge-col-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0 0 20px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.knowledge-list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  border: 1px solid transparent;
  transition: border-color 0.2s, background 0.2s;
}

.knowledge-list-item:hover {
  background: #fafafa;
  border-color: #eee;
}

.knowledge-list-thumb {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #eee;
}

.knowledge-list-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-list-thumb-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #999;
}

.knowledge-list-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.knowledge-list-name {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.knowledge-list-meta {
  font-size: 13px;
  color: #888;
}

.knowledge-col-link {
  font-size: 14px;
  color: #BB1919;
  text-decoration: none;
}

.knowledge-col-link:hover {
  text-decoration: underline;
}

.knowledge-questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.knowledge-question-btn {
  width: 100%;
  padding: 14px 18px;
  text-align: left;
  font-size: 15px;
  color: #333;
  background: #f7f7f7;
  border: 1px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.knowledge-question-btn:hover {
  background: #f0f0f0;
  border-color: #e0e0e0;
  color: #111;
}

@media (max-width: 900px) {
  .knowledge-cols {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* 加号下拉菜单：BBC 红，无 scoped 以便作用于 teleport 出的 popper */
.knowledge-add-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item {
  color: #333;
}
.knowledge-add-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:hover,
.knowledge-add-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:focus {
  background: #fff8f8;
  color: #BB1919;
}

/* 新建知识库弹窗内可见性单选：强制 BBC 红（弹窗 teleport 到 body，需全局样式） */
.knowledge-create-dialog--bbc .el-dialog__body {
  --el-color-primary: #BB1919;
}
.knowledge-create-dialog--bbc .el-radio__input.is-checked .el-radio__inner {
  border-color: #BB1919 !important;
  background: #BB1919 !important;
}
.knowledge-create-dialog--bbc .el-radio__input.is-checked .el-radio__inner::after {
  background-color: #fff !important;
}
.knowledge-create-dialog--bbc .el-radio__inner:hover {
  border-color: #BB1919 !important;
}
.knowledge-create-dialog--bbc .el-radio__input.is-checked + .el-radio__label {
  color: #BB1919 !important;
}
.knowledge-create-dialog--bbc .el-radio__label {
  color: #606266;
}

/* 收录文章右侧「...」下拉：删除这篇博客 用 BBC 红 */
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item {
  color: #333;
}
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:hover,
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:focus {
  background: #fff8f8;
  color: #BB1919;
}
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item.knowledge-article-dropdown-danger {
  color: #BB1919;
}
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item.knowledge-article-dropdown-danger:hover {
  background: #fff8f8;
  color: #9e1515;
}
</style>
