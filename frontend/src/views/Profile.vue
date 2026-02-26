<template>
  <div class="profile-page">
    <!-- 封面 + 头像与资料区 -->
    <section class="profile-header">
      <div class="profile-cover" :style="coverStyle">
        <input
          ref="coverFileInputRef"
          type="file"
          accept="image/*"
          class="cover-file-input"
          @change="onCoverFileChange"
        />
        <button type="button" class="cover-upload-btn" @click="triggerCoverUpload">
          <el-icon><Camera /></el-icon>
          上传封面图片
        </button>
      </div>
      <div class="profile-info-row">
        <div class="profile-avatar-wrap">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" class="profile-avatar" />
          <span v-else class="profile-avatar profile-avatar-ph">{{ displayName.charAt(0).toUpperCase() }}</span>
        </div>
        <div class="profile-meta">
          <h1 class="profile-name">{{ displayName }}</h1>
          <template v-if="detailCollapsed">
            <p v-if="profile.intro" class="profile-intro-plain">{{ profile.intro }}</p>
          </template>
          <template v-else>
            <p v-if="profile.residence" class="profile-line">
              <el-icon class="profile-line-icon"><Location /></el-icon>
              <span class="profile-label">居住地</span>{{ profile.residence }}
            </p>
            <p v-if="profile.industry" class="profile-line">
              <el-icon class="profile-line-icon"><Briefcase /></el-icon>
              <span class="profile-label">所在行业</span>{{ profile.industry }}</p>
            <p v-if="profile.gender" class="profile-line">
              <el-icon class="profile-line-icon"><User /></el-icon>
              <span class="profile-label">性别</span>{{ profile.gender }}
            </p>
            <p v-if="profile.bio" class="profile-line profile-bio">
              <el-icon class="profile-line-icon"><Document /></el-icon>
              <span class="profile-label">简介</span>{{ profile.bio }}
            </p>
            <p v-if="profile.wechatId" class="profile-line">
              <el-icon class="profile-line-icon"><ChatDotRound /></el-icon>
              <span class="profile-label">微信</span>{{ profile.wechatId }}
            </p>
          </template>
          <button type="button" class="profile-toggle-detail" @click="detailCollapsed = !detailCollapsed">
            <el-icon><component :is="detailCollapsed ? ArrowDown : ArrowUp" /></el-icon>
            {{ detailCollapsed ? '查看' : '收起' }}详细资料
          </button>
        </div>
        <div class="profile-actions">
          <button type="button" class="btn-edit-profile" @click="openEditProfile">编辑个人资料</button>
        </div>
      </div>
    </section>

    <!-- Tab 导航 -->
    <nav class="profile-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="profile-tab"
        :class="{ active: currentTab === tab.key }"
        @click="currentTab = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
      </button>
      <button type="button" class="profile-tab profile-tab-search" title="搜索">
        <el-icon><Search /></el-icon>
      </button>
    </nav>

    <div class="profile-body">
      <main class="profile-main">
        <template v-if="currentTab === 'collection'">
          <div class="blog-header-row collection-header-row">
            <h2 class="section-title">我的收藏</h2>
            <button type="button" class="btn-new-folder" @click="openCreateFolder">
              <el-icon><Plus /></el-icon>
              新建收藏夹
            </button>
          </div>
        </template>
        <template v-if="currentTab === 'column'">
          <div class="blog-header-row collection-header-row">
            <h2 class="section-title">我的专栏</h2>
            <button type="button" class="btn-new-folder" @click="openCreateColumn">
              <el-icon><Plus /></el-icon>
              新建专栏
            </button>
          </div>
        </template>
        <template v-if="currentTab === 'blog'">
          <div class="blog-header-row">
            <h2 class="section-title">我的博客</h2>
            <div class="blog-filters">
              <el-dropdown trigger="click" popper-class="blog-visibility-dropdown" @command="setVisibility">
                <span class="blog-sort blog-sort--dropdown">
                  {{ blogVisibilityLabel }}
                  <el-icon class="blog-sort-arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="ALL">全部可见</el-dropdown-item>
                    <el-dropdown-item command="SELF">仅我可见</el-dropdown-item>
                    <el-dropdown-item command="FANS">粉丝可见</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <span class="blog-sort" :class="{ active: blogSortBy === 'time' }" @click="setSort('time')">
                按时间排序 {{ blogSortBy === 'time' ? (blogOrder === 'desc' ? '↓' : '↑') : '↑↓' }}
              </span>
              <span class="blog-sort" :class="{ active: blogSortBy === 'likes' }" @click="setSort('likes')">
                按点赞量排序 {{ blogSortBy === 'likes' ? (blogOrder === 'desc' ? '↓' : '↑') : '↑↓' }}
              </span>
              <span class="blog-sort" :class="{ active: blogSortBy === 'views' }" @click="setSort('views')">
                按浏览量排序 {{ blogSortBy === 'views' ? (blogOrder === 'desc' ? '↓' : '↑') : '↑↓' }}
              </span>
            </div>
          </div>
        </template>
        <h2 v-else-if="currentTab !== 'collection' && currentTab !== 'column'" class="section-title">{{ sectionTitle }}</h2>
        <!-- 我的动态：赞同了文章 + 发表了博客 混合时间线，滚动加载更多 -->
        <div v-if="currentTab === 'dynamic'" ref="dynamicTabWrapRef" class="dynamic-tab-wrap">
          <div v-if="dynamicLoading" class="blog-loading">加载中…</div>
          <template v-else>
            <div class="activity-list">
              <article v-for="item in dynamicFeedList" :key="item.id" class="activity-item">
                <p class="activity-action">{{ item.actionText }}</p>
                <p class="activity-time">{{ item.time }}</p>
                <router-link :to="`/article/${item.contentId}`" class="activity-title">{{ item.title }}</router-link>
                <div v-if="item.authorName" class="activity-author">
                  <span class="activity-author-avatar">{{ item.authorName.charAt(0) }}</span>
                  <span class="activity-author-name">{{ item.authorName }}</span>
                  <span v-if="item.authorDesc" class="activity-author-desc">{{ item.authorDesc }}</span>
                </div>
              </article>
            </div>
            <div v-if="dynamicFeedList.length === 0" class="blog-empty">
              <p class="blog-empty-text">暂无动态</p>
            </div>
            <div
              v-show="dynamicFeedList.length > 0 && hasMoreDynamic"
              ref="dynamicSentinelRef"
              class="dynamic-sentinel"
            />
            <div v-if="dynamicLoadingMore" class="blog-loading dynamic-load-more">加载更多…</div>
            <p v-if="dynamicFeedList.length > 0 && !hasMoreDynamic && !dynamicLoading" class="dynamic-no-more">没有更多了</p>
          </template>
        </div>
        <!-- 我的博客 -->
        <div v-else-if="currentTab === 'blog'" class="blog-tab-wrap">
          <div v-if="blogLoading" class="blog-loading">加载中…</div>
          <div v-else-if="blogList.length === 0" class="blog-empty">
            <el-icon class="blog-empty-icon"><Document /></el-icon>
            <p class="blog-empty-text">还没有博客</p>
          </div>
          <div v-else class="profile-card-list">
            <article v-for="item in blogList" :key="item.id" class="profile-card-item profile-card-item--blog">
              <div class="profile-card-body">
                <router-link :to="`/article/${item.id}`" class="profile-card-title">{{ item.title }}</router-link>
                <p class="profile-card-meta">{{ item.summary }}</p>
                <div class="profile-card-stats">
                  <span class="stat"><el-icon><View /></el-icon> 阅读 {{ formatCount(item.viewCount) }}</span>
                  <span class="stat"><el-icon><Star /></el-icon> 赞 {{ formatCount(item.likeCount) }}</span>
                  <span class="stat"><el-icon><Collection /></el-icon> 收藏 {{ formatCount(item.collectionCount) }}</span>
                  <span class="stat profile-card-time">发布时间 {{ formatCreatedAt(item.createdAt) }}</span>
                </div>
              </div>
              <router-link :to="`/article/${item.id}`" class="profile-card-thumb">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" class="profile-card-thumb-img" />
                <span v-else class="profile-card-thumb-ph"></span>
              </router-link>
            </article>
          </div>
          <div v-if="blogTotal > 0" class="blog-pagination-wrap">
            <span class="blog-total-text">共 <span class="blog-total-num">{{ blogTotal }}</span> 条</span>
            <el-pagination
              v-model:current-page="blogPage"
              :page-size="blogPageSize"
              :total="blogTotal"
              layout="prev, pager, next"
              class="blog-pagination"
            />
          </div>
        </div>
        <!-- 我的收藏：收藏夹列表 -->
        <div v-else-if="currentTab === 'collection'" class="collection-tab-wrap">
          <div v-if="folderLoading" class="blog-loading">加载中…</div>
          <div v-else-if="folderList.length === 0" class="blog-empty collection-empty">
            <el-icon class="blog-empty-icon"><FolderOpened /></el-icon>
            <p class="blog-empty-text">还没有收藏夹</p>
            <button type="button" class="btn-new-folder-inline" @click="openCreateFolder">新建收藏夹</button>
          </div>
          <div v-else class="profile-card-list folder-list">
            <article v-for="folder in folderList" :key="folder.id" class="profile-card-item folder-item">
              <div class="folder-main">
                <span class="folder-name">{{ folder.name }}</span>
                <el-tag v-if="folder.isDefault" type="info" size="small" class="folder-tag-default">默认</el-tag>
                <div v-if="!folder.isDefault" class="folder-actions">
                  <button type="button" class="folder-action-btn" @click="openEditFolder(folder)">编辑</button>
                  <button type="button" class="folder-action-btn folder-action-btn--danger" @click="confirmDeleteFolder(folder)">删除</button>
                </div>
              </div>
              <p v-if="folder.description" class="profile-card-meta folder-desc">{{ folder.description }}</p>
              <p class="profile-card-meta folder-meta">
                {{ folder.count }} 条内容 · 创建于 {{ folder.createdAt }}
              </p>
            </article>
          </div>
        </div>
        <!-- 我的专栏：参考收藏布局，右侧新建专栏 + 每条右侧显示专栏封面 -->
        <div v-else-if="currentTab === 'column'" class="column-tab-wrap">
          <div v-if="columnLoading" class="blog-loading">加载中…</div>
          <div v-else-if="columnList.length === 0" class="blog-empty">
            <el-icon class="blog-empty-icon"><FolderOpened /></el-icon>
            <p class="blog-empty-text">还没有专栏</p>
            <button type="button" class="btn-new-folder-inline" @click="openCreateColumn">新建专栏</button>
          </div>
          <div v-else class="profile-card-list column-list">
            <article v-for="item in columnList" :key="item.id" class="profile-card-item profile-card-item--column">
              <div class="profile-card-body">
                <router-link :to="`/column/${item.id}`" class="profile-card-title">{{ item.name }}</router-link>
                <p class="profile-card-meta">{{ item.description }}</p>
                <p class="profile-card-time">{{ item.articleCount }} 篇内容 · 更新于 {{ item.updatedAt ? item.updatedAt.slice(0, 10) : '—' }}</p>
              </div>
              <router-link :to="`/column/${item.id}`" class="profile-card-thumb column-cover">
                <img v-if="item.cover" :src="item.cover" :alt="item.name" class="profile-card-thumb-img" />
                <span v-else class="profile-card-thumb-ph"></span>
              </router-link>
            </article>
          </div>
        </div>
      </main>
      <aside class="profile-sidebar">
        <CreationCenter />
        <div class="sidebar-stats">
          <div class="stat-item">
            <span class="stat-value">{{ followingCount }}</span>
            <span class="stat-label">关注了</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ followerCount }}</span>
            <span class="stat-label">关注者</span>
          </div>
        </div>
      </aside>
    </div>

    <!-- 背景图裁剪弹窗：裁剪区域与封面同比例 1200:280 -->
    <el-dialog
      v-model="cropDialogVisible"
      title="选择封面区域"
      width="640px"
      :close-on-click-modal="false"
      class="cover-crop-dialog"
      @closed="resetCropState"
    >
      <div class="crop-viewport" ref="cropViewportRef">
        <img
          v-if="cropImageUrl"
          ref="cropImageRef"
          :src="cropImageUrl"
          class="crop-image"
          :style="cropImageStyle"
          draggable="false"
          @mousedown.prevent="onCropMouseDown"
          @load="onCropImageLoad"
        />
      </div>
      <p class="crop-tip">拖动图片调整位置，将截取与封面相同比例的区域</p>
      <template #footer>
        <el-button @click="cropDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="coverUploading" @click="confirmCrop">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑个人资料弹窗 -->
    <el-dialog
      v-model="editProfileVisible"
      title="编辑个人资料"
      width="520px"
      top="8vh"
      class="edit-profile-dialog"
      @closed="resetEditForm"
    >
      <el-form :model="editForm" label-width="90px" label-position="left">
        <el-form-item class="edit-avatar-row">
          <div class="edit-avatar-wrap" @click="triggerAvatarUpload">
            <input
              ref="avatarFileInputRef"
              type="file"
              accept="image/*"
              class="edit-avatar-input"
              @change="onAvatarFileChange"
            />
            <img v-if="editForm.avatar" :src="editForm.avatar" alt="" class="edit-avatar-img" />
            <span v-else class="edit-avatar-ph">{{ (editForm.nickname || userStore.userInfo?.username || '用').charAt(0).toUpperCase() }}</span>
            <span class="edit-avatar-overlay" />
            <span v-if="avatarUploading" class="edit-avatar-loading"><el-icon class="is-loading"><Loading /></el-icon></span>
            <span v-else class="edit-avatar-plus"><el-icon :size="28"><Plus /></el-icon></span>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="昵称" maxlength="64" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="一句话介绍">
          <el-input v-model="editForm.intro" placeholder="一句话介绍" maxlength="256" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="editForm.gender" placeholder="请选择" clearable style="width: 100%" popper-class="edit-profile-select-popper">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="居住地">
          <el-cascader
            v-model="residenceCascaderValue"
            :options="residenceCascaderOptions"
            :props="{ value: 'name', label: 'name', checkStrictly: true }"
            placeholder="请选择省/市"
            clearable
            filterable
            style="width: 100%"
            popper-class="edit-profile-cascader-popper"
            @change="onResidenceChange"
          />
        </el-form-item>
        <el-form-item label="所在行业">
          <el-select
            v-model="editForm.industry"
            placeholder="请选择或输入行业"
            clearable
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            popper-class="edit-profile-select-popper"
          >
            <el-option v-for="item in industryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="4" placeholder="个人简介" maxlength="500" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editProfileVisible = false">取消</el-button>
        <el-button class="btn-save-profile" type="primary" :loading="editProfileSubmitting" @click="submitEditProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建新收藏夹弹窗：仅标题 + 简介，无可见性 -->
    <el-dialog
      v-model="createFolderVisible"
      title="创建新收藏夹"
      width="480px"
      top="12vh"
      class="create-folder-dialog"
      @closed="resetCreateFolderForm"
    >
      <el-form :model="createFolderForm" label-position="top">
        <el-form-item label="收藏标题">
          <el-input v-model="createFolderForm.name" placeholder="收藏标题" maxlength="64" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="收藏描述（可选）">
          <el-input v-model="createFolderForm.description" type="textarea" :rows="3" placeholder="收藏描述 (可选)" maxlength="256" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFolderVisible = false">取消</el-button>
        <el-button class="btn-confirm-folder" type="primary" :loading="createFolderSubmitting" @click="submitCreateFolder">确认</el-button>
      </template>
    </el-dialog>

    <!-- 编辑收藏夹弹窗：仅非默认收藏夹可编辑 -->
    <el-dialog
      v-model="editFolderVisible"
      title="编辑收藏夹"
      width="480px"
      top="12vh"
      class="create-folder-dialog edit-folder-dialog"
      @closed="resetEditFolderForm"
    >
      <el-form :model="editFolderForm" label-position="top">
        <el-form-item label="收藏标题">
          <el-input v-model="editFolderForm.name" placeholder="收藏标题" maxlength="64" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="收藏描述（可选）">
          <el-input v-model="editFolderForm.description" type="textarea" :rows="3" placeholder="收藏描述 (可选)" maxlength="256" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editFolderVisible = false">取消</el-button>
        <el-button class="btn-confirm-folder" type="primary" :loading="editFolderSubmitting" @click="submitEditFolder">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建专栏弹窗 -->
    <el-dialog
      v-model="createColumnVisible"
      title="新建专栏"
      width="480px"
      top="12vh"
      class="create-folder-dialog create-column-dialog"
      @closed="resetCreateColumnForm"
    >
      <el-form :model="createColumnForm" label-position="top">
        <el-form-item label="专栏名称">
          <el-input v-model="createColumnForm.name" placeholder="专栏名称" maxlength="128" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="专栏描述（可选）">
          <el-input v-model="createColumnForm.description" type="textarea" :rows="3" placeholder="专栏描述 (可选)" maxlength="512" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="封面图（可选）">
          <div class="column-cover-upload" :class="{ 'has-cover': !!createColumnForm.cover }">
            <div class="column-cover-preview" :style="createColumnForm.cover ? { backgroundImage: `url(${createColumnForm.cover})` } : {}">
              <input
                ref="columnCoverFileInputRef"
                type="file"
                accept="image/*"
                class="cover-file-input"
                @change="onColumnCoverFileChange"
              />
              <button v-if="!createColumnForm.cover" type="button" class="cover-upload-btn column-cover-upload-btn" @click="triggerColumnCoverUpload">
                <el-icon><Camera /></el-icon>
                上传封面图片
              </button>
              <template v-else>
                <button type="button" class="cover-upload-btn column-cover-upload-btn" @click="triggerColumnCoverUpload">更换</button>
                <button type="button" class="column-cover-remove" @click.stop="createColumnForm.cover = ''">移除</button>
              </template>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createColumnVisible = false">取消</el-button>
        <el-button class="btn-confirm-folder" type="primary" :loading="createColumnSubmitting" @click="submitCreateColumn">确认</el-button>
      </template>
    </el-dialog>

    <!-- 专栏封面裁剪弹窗：比例与专栏卡片封面一致 -->
    <el-dialog
      v-model="columnCropDialogVisible"
      title="选择封面区域"
      width="440px"
      :close-on-click-modal="false"
      class="cover-crop-dialog column-cover-crop-dialog"
      @closed="resetColumnCropState"
    >
      <div class="crop-viewport column-crop-viewport" ref="columnCropViewportRef">
        <img
          v-if="columnCropImageUrl"
          ref="columnCropImageRef"
          :src="columnCropImageUrl"
          class="crop-image"
          :style="columnCropImageStyle"
          draggable="false"
          @mousedown.prevent="onColumnCropMouseDown"
          @load="onColumnCropImageLoad"
        />
      </div>
      <p class="crop-tip">拖动图片调整位置，将截取与封面相同比例的区域</p>
      <template #footer>
        <el-button @click="columnCropDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="columnCoverUploading" @click="confirmColumnCrop">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import CreationCenter from '@/components/CreationCenter.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, Search, ArrowDown, ArrowUp, Plus, Loading, Location, Briefcase, User, Document, ChatDotRound, View, Star, Collection, FolderOpened } from '@element-plus/icons-vue'
import { getMe, updateMe, type UpdateProfilePayload } from '@/api/user'
import { getContentsMe, getContentsByIds, type ContentListItem } from '@/api/content'
import { getContentLikesMe } from '@/api/contentLike'
import { getCollectionFoldersMe, createCollectionFolder, updateCollectionFolder, deleteCollectionFolder, type CollectionFolderItem } from '@/api/collectionFolder'
import { getColumnsMe, createColumn, type ColumnItem } from '@/api/column'
import { getFollowMe } from '@/api/follow'
import { uploadImage } from '@/api/upload'
import provincesData from 'china-division/dist/provinces.json'
import pcData from 'china-division/dist/pc.json'

const userStore = useUserStore()

type ProvinceItem = { code: string; name: string }
type PcData = Record<string, string[]>
const provinces = provincesData as ProvinceItem[]
const pc = pcData as PcData

function buildResidenceCascaderOptions(): { name: string; children?: { name: string }[] }[] {
  return provinces.map((p) => ({
    name: p.name,
    children: (pc[p.name] || []).map((c) => ({ name: c })),
  }))
}
const residenceCascaderOptions = buildResidenceCascaderOptions()

function parseResidenceToCascader(residence: string): string[] {
  if (!residence || !residence.startsWith('现居')) return []
  const rest = residence.slice(2).trim()
  if (!rest) return []
  for (const p of provinces) {
    const cities = pc[p.name] || []
    if (cities.includes(rest)) return [p.name, rest]
    if (p.name === rest) return [p.name]
  }
  return []
}
function cascaderToResidence(arr: string[]): string {
  if (!arr?.length) return ''
  const last = arr[arr.length - 1]
  return last ? '现居' + last : ''
}
const detailCollapsed = ref(true)
const currentTab = ref('dynamic')

const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户')
const avatarUrl = computed(() => (userStore.userInfo as { avatar?: string })?.avatar || '')

const profile = ref<{
  nickname?: string
  residence?: string
  industry?: string
  cover?: string
  intro?: string
  gender?: string
  bio?: string
  wechatId?: string
}>({
  residence: '',
  industry: '',
  cover: '',
})

const residenceCascaderValue = ref<string[]>([])

const industryOptions = [
  '计算机软件', '互联网', '金融', '教育', '医疗健康', '制造业', '文化传媒', '电子商务', '学生', '其他',
]

// 编辑个人资料
const editProfileVisible = ref(false)
const editProfileSubmitting = ref(false)
const avatarFileInputRef = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const editForm = ref({
  nickname: '',
  avatar: '',
  intro: '',
  gender: '',
  residence: '',
  industry: '',
  bio: '',
})

function onResidenceChange(val: string[] | undefined) {
  editForm.value.residence = cascaderToResidence(val || [])
}

function openEditProfile() {
  const u = userStore.userInfo as Record<string, unknown> | null
  if (u) {
    const residence = (u.residence as string) ?? (profile.value.residence ?? '')
    editForm.value = {
      nickname: (u.nickname as string) ?? '',
      avatar: (u.avatar as string) ?? '',
      intro: (u.intro as string) ?? (profile.value.intro ?? ''),
      gender: (u.gender as string) ?? (profile.value.gender ?? ''),
      residence,
      industry: (u.industry as string) ?? (profile.value.industry ?? ''),
      bio: (u.bio as string) ?? (profile.value.bio ?? ''),
    }
    residenceCascaderValue.value = parseResidenceToCascader(residence)
  }
  editProfileVisible.value = true
}

function triggerAvatarUpload() {
  if (avatarUploading.value) return
  avatarFileInputRef.value?.click()
}

function onAvatarFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  avatarUploading.value = true
  uploadImage(file, 'avatar')
    .then((meta) => { editForm.value.avatar = meta.url })
    .finally(() => { avatarUploading.value = false })
  input.value = ''
}

function submitEditProfile() {
  const f = editForm.value
  const payload: UpdateProfilePayload = {
    nickname: f.nickname.trim(),
    avatar: f.avatar.trim(),
    intro: f.intro.trim(),
    gender: f.gender || undefined,
    residence: f.residence || undefined,
    industry: f.industry || undefined,
    bio: f.bio.trim(),
  }
  editProfileSubmitting.value = true
  updateMe(payload)
    .then((user) => {
      userStore.setUserInfo(user)
      profile.value.nickname = user.nickname
      profile.value.cover = user.cover ?? profile.value.cover
      profile.value.intro = user.intro
      profile.value.gender = user.gender
      profile.value.residence = user.residence
      profile.value.industry = user.industry
      profile.value.bio = user.bio
      profile.value.wechatId = user.wechatId
      editProfileVisible.value = false
    })
    .finally(() => { editProfileSubmitting.value = false })
}

function resetEditForm() {
  editForm.value = {
    nickname: '',
    avatar: '',
    intro: '',
    gender: '',
    residence: '',
    industry: '',
    bio: '',
  }
  residenceCascaderValue.value = []
}
const coverStyle = computed(() => {
  const url = profile.value.cover || (userStore.userInfo as { cover?: string })?.cover || ''
  return url ? { backgroundImage: `url(${url})` } : {}
})

// 封面裁剪：与页面封面同比例 1200:280，弹窗内视口 600x140
const COVER_ASPECT_W = 1200
const COVER_ASPECT_H = 280
const CROP_VIEWPORT_W = 600
const CROP_VIEWPORT_H = 140

const coverFileInputRef = ref<HTMLInputElement | null>(null)
const cropDialogVisible = ref(false)
const cropImageUrl = ref('')
const cropImageRef = ref<HTMLImageElement | null>(null)
const cropViewportRef = ref<HTMLDivElement | null>(null)
const coverUploading = ref(false)

const cropState = ref({
  naturalW: 0,
  naturalH: 0,
  scale: 1,
  translateX: 0,
  translateY: 0,
  isDragging: false,
  startX: 0,
  startY: 0,
  startTranslateX: 0,
  startTranslateY: 0,
})

const cropImageStyle = computed(() => {
  const s = cropState.value
  const w = s.naturalW * s.scale
  const h = s.naturalH * s.scale
  return {
    width: `${w}px`,
    height: `${h}px`,
    transform: `translate(${s.translateX}px, ${s.translateY}px)`,
    cursor: s.isDragging ? 'grabbing' : 'grab',
  }
})

function triggerCoverUpload() {
  coverFileInputRef.value?.click()
}

function onCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  cropImageUrl.value = URL.createObjectURL(file)
  cropDialogVisible.value = true
  input.value = ''
}

function onCropImageLoad() {
  const img = cropImageRef.value
  if (!img || !cropViewportRef.value) return
  const nw = img.naturalWidth
  const nh = img.naturalHeight
  const scale = Math.max(CROP_VIEWPORT_W / nw, CROP_VIEWPORT_H / nh)
  const displayW = nw * scale
  const displayH = nh * scale
  cropState.value = {
    ...cropState.value,
    naturalW: nw,
    naturalH: nh,
    scale,
    translateX: (CROP_VIEWPORT_W - displayW) / 2,
    translateY: (CROP_VIEWPORT_H - displayH) / 2,
  }
}

function onCropMouseDown(e: MouseEvent) {
  cropState.value.isDragging = true
  cropState.value.startX = e.clientX
  cropState.value.startY = e.clientY
  cropState.value.startTranslateX = cropState.value.translateX
  cropState.value.startTranslateY = cropState.value.translateY
  const onMove = (e2: MouseEvent) => {
    const dx = e2.clientX - cropState.value.startX
    const dy = e2.clientY - cropState.value.startY
    const s = cropState.value
    const displayW = s.naturalW * s.scale
    const displayH = s.naturalH * s.scale
    let tx = s.startTranslateX + dx
    let ty = s.startTranslateY + dy
    tx = Math.max(CROP_VIEWPORT_W - displayW, Math.min(0, tx))
    ty = Math.max(CROP_VIEWPORT_H - displayH, Math.min(0, ty))
    cropState.value.translateX = tx
    cropState.value.translateY = ty
  }
  const onUp = () => {
    cropState.value.isDragging = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function confirmCrop() {
  const img = cropImageRef.value
  if (!img || !img.complete) return
  const s = cropState.value
  const sx = -s.translateX / s.scale
  const sy = -s.translateY / s.scale
  const sw = CROP_VIEWPORT_W / s.scale
  const sh = CROP_VIEWPORT_H / s.scale
  const canvas = document.createElement('canvas')
  canvas.width = COVER_ASPECT_W
  canvas.height = COVER_ASPECT_H
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, COVER_ASPECT_W, COVER_ASPECT_H)
  coverUploading.value = true
  canvas.toBlob(
    (blob) => {
      if (!blob) {
        coverUploading.value = false
        return
      }
      const file = new File([blob], 'cover.jpg', { type: 'image/jpeg' })
      uploadImage(file, 'cover')
        .then((meta) => updateMe({ cover: meta.url }))
        .then((user) => {
          userStore.setUserInfo(user)
          profile.value.cover = user.cover || ''
          cropDialogVisible.value = false
        })
        .finally(() => { coverUploading.value = false })
    },
    'image/jpeg',
    0.9
  )
}

function resetCropState() {
  if (cropImageUrl.value) URL.revokeObjectURL(cropImageUrl.value)
  cropImageUrl.value = ''
  cropState.value = {
    naturalW: 0,
    naturalH: 0,
    scale: 1,
    translateX: 0,
    translateY: 0,
    isDragging: false,
    startX: 0,
    startY: 0,
    startTranslateX: 0,
    startTranslateY: 0,
  }
}

// 专栏封面裁剪：比例与列表展示一致 120:84（约 10:7）
const COLUMN_COVER_VIEWPORT_W = 400
const COLUMN_COVER_VIEWPORT_H = 280
const COLUMN_COVER_OUTPUT_W = 400
const COLUMN_COVER_OUTPUT_H = 280

const columnCoverFileInputRef = ref<HTMLInputElement | null>(null)
const columnCropDialogVisible = ref(false)
const columnCropImageUrl = ref('')
const columnCropImageRef = ref<HTMLImageElement | null>(null)
const columnCropViewportRef = ref<HTMLDivElement | null>(null)
const columnCoverUploading = ref(false)

const columnCropState = ref({
  naturalW: 0,
  naturalH: 0,
  scale: 1,
  translateX: 0,
  translateY: 0,
  isDragging: false,
  startX: 0,
  startY: 0,
  startTranslateX: 0,
  startTranslateY: 0,
})

const columnCropImageStyle = computed(() => {
  const s = columnCropState.value
  const w = s.naturalW * s.scale
  const h = s.naturalH * s.scale
  return {
    width: `${w}px`,
    height: `${h}px`,
    transform: `translate(${s.translateX}px, ${s.translateY}px)`,
    cursor: s.isDragging ? 'grabbing' : 'grab',
  }
})

function triggerColumnCoverUpload() {
  columnCoverFileInputRef.value?.click()
}

function onColumnCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  columnCropImageUrl.value = URL.createObjectURL(file)
  columnCropDialogVisible.value = true
  input.value = ''
}

function onColumnCropImageLoad() {
  const img = columnCropImageRef.value
  if (!img || !columnCropViewportRef.value) return
  const nw = img.naturalWidth
  const nh = img.naturalHeight
  const scale = Math.max(COLUMN_COVER_VIEWPORT_W / nw, COLUMN_COVER_VIEWPORT_H / nh)
  const displayW = nw * scale
  const displayH = nh * scale
  columnCropState.value = {
    ...columnCropState.value,
    naturalW: nw,
    naturalH: nh,
    scale,
    translateX: (COLUMN_COVER_VIEWPORT_W - displayW) / 2,
    translateY: (COLUMN_COVER_VIEWPORT_H - displayH) / 2,
  }
}

function onColumnCropMouseDown(e: MouseEvent) {
  columnCropState.value.isDragging = true
  columnCropState.value.startX = e.clientX
  columnCropState.value.startY = e.clientY
  columnCropState.value.startTranslateX = columnCropState.value.translateX
  columnCropState.value.startTranslateY = columnCropState.value.translateY
  const onMove = (e2: MouseEvent) => {
    const dx = e2.clientX - columnCropState.value.startX
    const dy = e2.clientY - columnCropState.value.startY
    const s = columnCropState.value
    const displayW = s.naturalW * s.scale
    const displayH = s.naturalH * s.scale
    let tx = s.startTranslateX + dx
    let ty = s.startTranslateY + dy
    tx = Math.max(COLUMN_COVER_VIEWPORT_W - displayW, Math.min(0, tx))
    ty = Math.max(COLUMN_COVER_VIEWPORT_H - displayH, Math.min(0, ty))
    columnCropState.value.translateX = tx
    columnCropState.value.translateY = ty
  }
  const onUp = () => {
    columnCropState.value.isDragging = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function confirmColumnCrop() {
  const img = columnCropImageRef.value
  if (!img || !img.complete) return
  const s = columnCropState.value
  const sx = -s.translateX / s.scale
  const sy = -s.translateY / s.scale
  const sw = COLUMN_COVER_VIEWPORT_W / s.scale
  const sh = COLUMN_COVER_VIEWPORT_H / s.scale
  const canvas = document.createElement('canvas')
  canvas.width = COLUMN_COVER_OUTPUT_W
  canvas.height = COLUMN_COVER_OUTPUT_H
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, COLUMN_COVER_OUTPUT_W, COLUMN_COVER_OUTPUT_H)
  columnCoverUploading.value = true
  canvas.toBlob(
    (blob) => {
      if (!blob) {
        columnCoverUploading.value = false
        return
      }
      const file = new File([blob], 'column-cover.jpg', { type: 'image/jpeg' })
      uploadImage(file, 'cover')
        .then((meta) => {
          createColumnForm.value.cover = meta.url
          columnCropDialogVisible.value = false
        })
        .finally(() => { columnCoverUploading.value = false })
    },
    'image/jpeg',
    0.9
  )
}

function resetColumnCropState() {
  if (columnCropImageUrl.value) URL.revokeObjectURL(columnCropImageUrl.value)
  columnCropImageUrl.value = ''
  columnCropState.value = {
    naturalW: 0,
    naturalH: 0,
    scale: 1,
    translateX: 0,
    translateY: 0,
    isDragging: false,
    startX: 0,
    startY: 0,
    startTranslateX: 0,
    startTranslateY: 0,
  }
}

onMounted(() => {
  if (!userStore.isLoggedIn) return
  getMe().then((user) => {
    userStore.setUserInfo(user)
    profile.value.nickname = user.nickname
    profile.value.cover = user.cover ?? profile.value.cover
    profile.value.intro = user.intro
    profile.value.gender = user.gender
    profile.value.residence = user.residence
    profile.value.industry = user.industry
    profile.value.bio = user.bio
    profile.value.wechatId = user.wechatId
  }).catch(() => {})
  if (currentTab.value === 'blog') fetchBlogList()
  if (currentTab.value === 'dynamic') fetchDynamicBlogs()
  // 预拉取收藏夹、专栏列表、关注统计，使 Tab 数量与右侧关注数正确显示
  fetchFolderList()
  fetchColumnList()
  fetchFollowStats()
})

const tabs = computed(() => [
  { key: 'dynamic', label: '动态', count: undefined as number | undefined },
  { key: 'blog', label: '博客', count: blogTotal.value },
  { key: 'collection', label: '收藏', count: folderList.value.length },
  { key: 'column', label: '专栏', count: columnList.value.length },
])

const sectionTitle = computed(() => {
  const map: Record<string, string> = {
    dynamic: '我的动态',
    blog: '我的博客',
    collection: '我的收藏',
    column: '我的专栏',
  }
  return map[currentTab.value] || '我的动态'
})

// 动态：赞同了文章（来自 interaction-service content-likes/me + contents/by-ids）
const likedActivities = ref<{ id: string; actionText: string; time: string; title: string; contentId: string; authorName?: string; authorDesc?: string }[]>([])
// 动态：混合时间线，滚动到底时按页拉取博客并追加（不一次全加载）
const dynamicBlogList = ref<ContentListItem[]>([])
const dynamicBlogTotal = ref(0)
const dynamicBlogPage = ref(0)       // 已拉到第几页（0 表示未拉过）
const dynamicPageSize = 10
const dynamicLoading = ref(false)
const dynamicLoadingMore = ref(false)
const dynamicTabWrapRef = ref<HTMLElement | null>(null)
const dynamicSentinelRef = ref<HTMLElement | null>(null)

const hasMoreDynamic = computed(
  () => dynamicBlogList.value.length < dynamicBlogTotal.value
)

function parseTimeForSort(t: string): number {
  if (!t) return 0
  const s = t.replace(/-/g, '').replace(/:/g, '').replace(/\s/g, '')
  return parseInt(s.slice(0, 12), 10) || 0
}

async function fetchDynamicBlogs() {
  if (!userStore.isLoggedIn) return
  dynamicLoading.value = true
  dynamicBlogPage.value = 0
  dynamicBlogList.value = []
  likedActivities.value = []
  try {
    const [blogRes, likedRes] = await Promise.all([
      getContentsMe({
        page: 1,
        pageSize: dynamicPageSize,
        sortBy: 'time',
        order: 'desc',
      }),
      getContentLikesMe({ page: 1, pageSize: 20 }),
    ])
    dynamicBlogList.value = blogRes.list
    dynamicBlogTotal.value = blogRes.total
    dynamicBlogPage.value = 1
    if (likedRes.list.length > 0) {
      const ids = likedRes.list.map((l) => l.contentId)
      const contents = await getContentsByIds(ids)
      const contentMap = Object.fromEntries(contents.map((c) => [c.id, c]))
      likedActivities.value = likedRes.list.map((l) => ({
        id: String(l.contentId),
        actionText: '赞同了文章',
        time: l.likedAt,
        title: contentMap[l.contentId]?.title ?? '',
        contentId: String(l.contentId),
        authorName: undefined as string | undefined,
        authorDesc: undefined as string | undefined,
      }))
    }
  } finally {
    dynamicLoading.value = false
  }
}

async function loadMoreDynamic() {
  if (!userStore.isLoggedIn || dynamicLoadingMore.value || !hasMoreDynamic.value) return
  dynamicLoadingMore.value = true
  try {
    const nextPage = dynamicBlogPage.value + 1
    const res = await getContentsMe({
      page: nextPage,
      pageSize: dynamicPageSize,
      sortBy: 'time',
      order: 'desc',
    })
    dynamicBlogList.value = [...dynamicBlogList.value, ...res.list]
    dynamicBlogTotal.value = res.total
    dynamicBlogPage.value = nextPage
  } finally {
    dynamicLoadingMore.value = false
  }
}

// 混合时间线：赞同了文章 + 发表了博客，按时间倒序
const dynamicFeedList = computed(() => {
  const likeItems = likedActivities.value.map((a) => ({
    id: 'like-' + a.id,
    actionText: a.actionText,
    time: a.time,
    title: a.title,
    contentId: a.contentId,
    authorName: a.authorName,
    authorDesc: a.authorDesc,
    _sort: parseTimeForSort(a.time),
  }))
  const blogItems = dynamicBlogList.value.map((b) => ({
    id: 'blog-' + b.id,
    actionText: '发表了博客',
    time: formatCreatedAt(b.createdAt) || b.createdAt || '',
    title: b.title,
    contentId: String(b.id),
    authorName: undefined as string | undefined,
    authorDesc: undefined as string | undefined,
    _sort: parseTimeForSort(b.createdAt || ''),
  }))
  const merged = [...likeItems, ...blogItems]
  merged.sort((a, b) => b._sort - a._sort)
  return merged.map(({ _sort, ...rest }) => rest)
})

// 动态无限滚动：触底时加载下一页
let dynamicSentinelObserver: IntersectionObserver | null = null
function setupDynamicSentinelObserver() {
  const el = dynamicSentinelRef.value
  dynamicSentinelObserver?.disconnect()
  dynamicSentinelObserver = null
  if (!el || currentTab.value !== 'dynamic') return
  dynamicSentinelObserver = new IntersectionObserver(
    (entries) => {
      if (!entries[0]?.isIntersecting) return
      if (currentTab.value !== 'dynamic' || !hasMoreDynamic.value || dynamicLoadingMore.value) return
      loadMoreDynamic()
    },
    { rootMargin: '80px', threshold: 0 }
  )
  dynamicSentinelObserver.observe(el)
}
watch(
  () => [currentTab.value, dynamicLoading.value],
  () => {
    if (currentTab.value === 'dynamic' && !dynamicLoading.value) {
      nextTick(() => setupDynamicSentinelObserver())
    } else {
      dynamicSentinelObserver?.disconnect()
      dynamicSentinelObserver = null
    }
  }
)
onUnmounted(() => {
  dynamicSentinelObserver?.disconnect()
  dynamicSentinelObserver = null
})

const blogList = ref<ContentListItem[]>([])
const blogTotal = ref(0)
const blogPage = ref(1)
const blogPageSize = 10
const blogLoading = ref(false)
const blogVisibility = ref<'ALL' | 'SELF' | 'FANS'>('ALL')
const blogSortBy = ref<'time' | 'likes' | 'views'>('time')
const blogOrder = ref<'asc' | 'desc'>('desc')
const blogVisibilityLabel = computed(() => {
  const map = { ALL: '全部可见', SELF: '仅我可见', FANS: '粉丝可见' }
  return map[blogVisibility.value]
})
function setVisibility(v: 'ALL' | 'SELF' | 'FANS') {
  blogVisibility.value = v
  blogPage.value = 1
  fetchBlogList()
}
function setSort(field: 'time' | 'likes' | 'views') {
  if (blogSortBy.value === field) {
    blogOrder.value = blogOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    blogSortBy.value = field
    blogOrder.value = 'desc'
  }
  blogPage.value = 1
  fetchBlogList()
}
async function fetchBlogList() {
  if (!userStore.isLoggedIn) return
  blogLoading.value = true
  try {
    const res = await getContentsMe({
      page: blogPage.value,
      pageSize: blogPageSize,
      visibility: blogVisibility.value,
      sortBy: blogSortBy.value,
      order: blogOrder.value,
    })
    blogList.value = res.list
    blogTotal.value = res.total
  } finally {
    blogLoading.value = false
  }
}
function formatCount(n: number) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
function formatCreatedAt(s: string) {
  if (!s) return ''
  return s.slice(0, 10)
}
watch([currentTab, blogPage], () => {
  if (currentTab.value === 'dynamic') fetchDynamicBlogs()
  if (currentTab.value === 'blog') fetchBlogList()
  if (currentTab.value === 'collection') fetchFolderList()
  if (currentTab.value === 'column') fetchColumnList()
})
// 收藏夹列表（我的收藏下展示所有收藏夹）
const folderList = ref<CollectionFolderItem[]>([])
const folderLoading = ref(false)
const createFolderVisible = ref(false)
const createFolderSubmitting = ref(false)
const createFolderForm = ref({ name: '', description: '' })
function openCreateFolder() {
  createFolderForm.value = { name: '', description: '' }
  createFolderVisible.value = true
}
function resetCreateFolderForm() {
  createFolderForm.value = { name: '', description: '' }
}
async function fetchFolderList() {
  if (!userStore.isLoggedIn) return
  folderLoading.value = true
  try {
    const list = await getCollectionFoldersMe()
    folderList.value = list
  } finally {
    folderLoading.value = false
  }
}
function submitCreateFolder() {
  const name = createFolderForm.value.name?.trim()
  if (!name) return
  createFolderSubmitting.value = true
  createCollectionFolder({
    name,
    description: createFolderForm.value.description?.trim() || undefined,
  })
    .then((created) => {
      // 确保有创建时间（部分环境接口可能未立即回填，前端兜底）
      const item: CollectionFolderItem = {
        ...created,
        createdAt: created.createdAt ?? new Date().toISOString().slice(0, 10),
      }
      folderList.value = [...folderList.value, item]
      createFolderVisible.value = false
      resetCreateFolderForm()
    })
    .finally(() => { createFolderSubmitting.value = false })
}

// 编辑收藏夹（仅非默认收藏夹显示编辑按钮，默认收藏夹不能修改名字和简介）
const editFolderVisible = ref(false)
const editFolderId = ref<number | null>(null)
const editFolderSubmitting = ref(false)
const editFolderForm = ref({ name: '', description: '' })
function openEditFolder(folder: CollectionFolderItem) {
  if (folder.isDefault) return
  editFolderId.value = folder.id
  editFolderForm.value = {
    name: folder.name,
    description: folder.description ?? '',
  }
  editFolderVisible.value = true
}
function resetEditFolderForm() {
  editFolderId.value = null
  editFolderForm.value = { name: '', description: '' }
}
function submitEditFolder() {
  const id = editFolderId.value
  if (id == null) return
  const name = editFolderForm.value.name?.trim()
  if (!name) return
  editFolderSubmitting.value = true
  updateCollectionFolder(id, {
    name,
    description: editFolderForm.value.description?.trim() || undefined,
  })
    .then((updated) => {
      folderList.value = folderList.value.map((f) => (f.id === id ? updated : f))
      editFolderVisible.value = false
      resetEditFolderForm()
    })
    .finally(() => { editFolderSubmitting.value = false })
}
function confirmDeleteFolder(folder: CollectionFolderItem) {
  if (folder.isDefault) return
  ElMessageBox.confirm(`确定要删除收藏夹「${folder.name}」吗？`, '删除收藏夹', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => deleteCollectionFolder(folder.id))
    .then(() => {
      folderList.value = folderList.value.filter((f) => f.id !== folder.id)
    })
}
const columnList = ref<ColumnItem[]>([])
const columnLoading = ref(false)
const createColumnVisible = ref(false)
const createColumnSubmitting = ref(false)
const createColumnForm = ref({ name: '', description: '', cover: '' })

async function fetchColumnList() {
  if (!userStore.isLoggedIn) return
  columnLoading.value = true
  try {
    const list = await getColumnsMe()
    columnList.value = list
  } finally {
    columnLoading.value = false
  }
}

function openCreateColumn() {
  createColumnForm.value = { name: '', description: '', cover: '' }
  createColumnVisible.value = true
}
function resetCreateColumnForm() {
  createColumnForm.value = { name: '', description: '', cover: '' }
}
function submitCreateColumn() {
  const name = createColumnForm.value.name?.trim()
  if (!name) {
    ElMessage.warning('请输入专栏名称')
    return
  }
  createColumnSubmitting.value = true
  createColumn({
    name,
    description: createColumnForm.value.description?.trim() || undefined,
    cover: createColumnForm.value.cover?.trim() || undefined,
  })
    .then((created) => {
      columnList.value = [...columnList.value, created]
      createColumnVisible.value = false
      resetCreateColumnForm()
      ElMessage.success('专栏已创建')
    })
    .finally(() => { createColumnSubmitting.value = false })
}

const followingCount = ref(0)
const followerCount = ref(0)

async function fetchFollowStats() {
  if (!userStore.isLoggedIn) return
  try {
    const stats = await getFollowMe()
    followingCount.value = stats.followingCount
    followerCount.value = stats.followerCount
  } catch {
    // 忽略错误，保持 0
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 48px;
  background: var(--el-bg-color-page, #f5f5f5);
  min-height: calc(100vh - 64px);
}

/* 封面 */
.profile-header {
  position: relative;
  margin-bottom: 0;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.profile-cover {
  height: 280px;
  background: #e8e8e8;
  background-size: cover;
  background-position: center;
  position: relative;
}

.cover-upload-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  color: #555;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.cover-upload-btn:hover {
  background: #fff;
  color: #111;
}

.cover-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}

/* 背景图裁剪弹窗：视口比例与封面一致 1200:280 */
.cover-crop-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.crop-viewport {
  width: 600px;
  height: 140px;
  margin: 0 auto;
  overflow: hidden;
  position: relative;
  background: #e8e8e8;
  border-radius: 8px;
}
.crop-image {
  position: absolute;
  left: 0;
  top: 0;
  display: block;
  user-select: none;
  pointer-events: none;
}
.crop-viewport .crop-image {
  pointer-events: auto;
}
.crop-tip {
  margin: 12px 0 0;
  font-size: 13px;
  color: #666;
}

/* 新建专栏：封面上传区（点击上传 + 裁剪） */
.column-cover-upload {
  width: 100%;
}
.column-cover-preview {
  position: relative;
  width: 100%;
  height: 140px;
  background-color: #e8e8e8;
  background-size: cover;
  background-position: center;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.column-cover-preview .cover-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
}
.column-cover-upload-btn {
  position: relative;
  z-index: 1;
  padding: 8px 16px;
  font-size: 14px;
  color: #666;
  background: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.column-cover-upload-btn:hover {
  color: #333;
  background: #f5f5f5;
}
.column-cover-upload .has-cover .column-cover-preview .column-cover-upload-btn {
  margin-right: 8px;
}
.column-cover-remove {
  position: relative;
  z-index: 1;
  padding: 8px 16px;
  font-size: 14px;
  color: #666;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.column-cover-remove:hover {
  color: #BB1919;
  background: #fff;
}
.column-cover-crop-dialog .column-crop-viewport {
  width: 400px;
  height: 280px;
  margin: 0 auto;
}

/* 编辑个人资料弹窗：头像置顶居中、圆形、低透明白遮罩、居中加号 icon；保存按钮配色与全站一致；禁止蓝色 */
.edit-profile-dialog,
.edit-profile-dialog :deep(.el-dialog__body) {
  --el-color-primary: #BB1919;
}
.edit-profile-dialog :deep(.el-dialog__header) {
  color: #111;
}
.edit-profile-dialog :deep(.el-input__wrapper) {
  --el-input-focus-border-color: #BB1919;
  --el-input-hover-border-color: #BB1919;
}
.edit-profile-dialog :deep(.el-input.is-focus .el-input__wrapper),
.edit-profile-dialog :deep(.el-input__wrapper:focus-within),
.edit-profile-dialog :deep(.el-select .el-input.is-focus .el-input__wrapper),
.edit-profile-dialog :deep(.el-select .el-input__wrapper:focus-within),
.edit-profile-dialog :deep(.el-cascader .el-input.is-focus .el-input__wrapper),
.edit-profile-dialog :deep(.el-cascader .el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.edit-profile-dialog :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.edit-avatar-row {
  margin-bottom: 20px;
}
.edit-avatar-row :deep(.el-form-item__content) {
  margin-left: 0 !important;
  justify-content: center;
}
.edit-avatar-wrap {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  background: #e8e8e8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: box-shadow 0.2s;
}
.edit-avatar-wrap:hover {
  box-shadow: 0 0 0 2px rgba(187, 25, 25, 0.4);
}
.edit-avatar-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}
.edit-avatar-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.edit-avatar-ph {
  position: absolute;
  inset: 0;
  font-size: 36px;
  font-weight: 600;
  color: #fff;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
}
.edit-avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.25);
  pointer-events: none;
}
.edit-avatar-plus {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.edit-avatar-plus .el-icon {
  font-size: 28px;
}
.edit-avatar-loading {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.85);
  color: #BB1919;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  z-index: 1;
}
.edit-profile-dialog .btn-save-profile {
  background: #BB1919;
  border-color: #BB1919;
}
.edit-profile-dialog .btn-save-profile:hover {
  background: #9e1515;
  border-color: #9e1515;
}

/* 头像与资料行：头像左对齐封面；白块左缘与头像右缘对齐（参考图），不重叠 */
.profile-info-row {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  padding: 24px 24px 24px 24px;
  margin-top: -90px;
  position: relative;
  z-index: 1;
  background: #fff;
}

.profile-avatar-wrap {
  flex-shrink: 0;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  border: 3px solid #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  object-fit: cover;
  display: block;
  background: #111;
}

.profile-avatar-ph {
  background: #111;
  color: #fff;
  font-size: 48px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-meta {
  flex: 1;
  min-width: 0;
  padding-top: 12px;
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px;
}

.profile-line {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 14px;
  color: #555;
  margin: 4px 0;
}
.profile-line-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: #888;
  margin-top: 1px;
}
.profile-bio {
  white-space: pre-wrap;
  word-break: break-word;
}
.profile-intro-plain {
  color: #555;
  font-size: 14px;
  margin: 4px 0;
}

.profile-label {
  color: #888;
  margin-right: 4px;
  flex-shrink: 0;
}

.profile-toggle-detail {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding: 0;
  font-size: 13px;
  color: #666;
  background: none;
  border: none;
  cursor: pointer;
}

.profile-toggle-detail:hover {
  color: #BB1919;
}

.profile-actions {
  flex-shrink: 0;
}

.btn-edit-profile {
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  background: #BB1919;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-edit-profile:hover {
  background: #9e1515;
}

/* Tab：与上方资料区左缘对齐 */
.profile-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 24px 0 0;
  background: #fff;
  border-bottom: 1px solid #eee;
  margin-top: 0;
  border-radius: 0 0 8px 8px;
  overflow-x: auto;
}

.profile-tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 14px 16px;
  font-size: 15px;
  color: #555;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.profile-tab:hover {
  color: #111;
}

.profile-tab.active {
  color: #BB1919;
  font-weight: 600;
  border-bottom-color: #BB1919;
}

.tab-count {
  font-size: 13px;
  color: #999;
}

.profile-tab.active .tab-count {
  color: #BB1919;
}

.profile-tab-search {
  margin-left: auto;
  padding: 10px;
}

/* 主体 */
.profile-body {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.profile-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0 0 20px;
}

.activity-list,
.profile-card-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.activity-item,
.profile-card-item {
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.profile-card-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

/* 博客列表：左侧缩略图，上沿与标题对齐 */
.profile-card-item--blog {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.profile-card-thumb {
  flex-shrink: 0;
  width: 120px;
  height: 84px;
  border-radius: 6px;
  overflow: hidden;
  background: #e8e8e8;
  display: block;
}
.profile-card-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.profile-card-thumb-ph {
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e0e0e0 0%, #eee 100%);
}
.profile-card-item--blog .profile-card-body {
  flex: 1;
  min-width: 0;
}
.profile-card-item--blog .profile-card-title {
  margin-top: 0;
}

.profile-card-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #111;
  margin-bottom: 6px;
  text-decoration: none;
}

.profile-card-title:hover {
  color: #BB1919;
}

.profile-card-stats {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 16px;
  margin: 8px 0 4px;
  font-size: 13px;
  line-height: 20px;
  height: 20px;
  color: #666;
}
.profile-card-stats .stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 20px;
  line-height: 20px;
  font-size: 13px;
}
.profile-card-stats .stat :deep(.el-icon) {
  font-size: 14px;
  width: 14px;
  height: 14px;
  color: #999;
  flex-shrink: 0;
}
.profile-card-stats .stat :deep(.el-icon svg) {
  width: 14px;
  height: 14px;
  vertical-align: middle;
}
.profile-card-stats .profile-card-time {
  font-size: 13px;
  line-height: 20px;
  height: 20px;
  color: #999;
  display: inline-flex;
  align-items: center;
  transform: translateY(2px);
}
.profile-card-meta,
.profile-card-time {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px;
}

.profile-card-time {
  font-size: 13px;
  color: #999;
}

.blog-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.blog-header-row .section-title {
  margin: 0;
}
.collection-header-row {
  margin-bottom: 20px;
}
.btn-new-folder {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  color: #BB1919;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}
.btn-new-folder:hover {
  color: #9e1515;
}
.btn-new-folder .el-icon {
  font-size: 16px;
}
.create-folder-dialog,
.create-folder-dialog :deep(.el-dialog__body) {
  --el-color-primary: #BB1919;
}
.create-folder-dialog :deep(.el-dialog__header) {
  color: #111;
}
.create-folder-dialog :deep(.el-input__wrapper) {
  --el-input-focus-border-color: #BB1919;
  --el-input-hover-border-color: #BB1919;
}
.create-folder-dialog :deep(.el-input.is-focus .el-input__wrapper),
.create-folder-dialog :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.create-folder-dialog .btn-confirm-folder {
  background: #BB1919;
  border-color: #BB1919;
}
.create-folder-dialog .btn-confirm-folder:hover {
  background: #9e1515;
  border-color: #9e1515;
}
.collection-empty {
  padding: 48px 24px;
}
.btn-new-folder-inline {
  margin-top: 16px;
  padding: 8px 20px;
  font-size: 14px;
  color: #BB1919;
  background: #fff;
  border: 1px solid #BB1919;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.btn-new-folder-inline:hover {
  background: #BB1919;
  color: #fff;
}
.folder-list .folder-item .folder-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.folder-list .folder-name {
  font-size: 16px;
  font-weight: 600;
  color: #111;
}
.folder-tag-default {
  font-size: 12px;
}
.folder-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.folder-action-btn {
  padding: 4px 10px;
  font-size: 13px;
  color: #666;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}
.folder-action-btn:hover {
  color: #BB1919;
  background: rgba(187, 25, 25, 0.06);
}
.folder-action-btn--danger:hover {
  color: #c45656;
  background: rgba(196, 86, 86, 0.08);
}
.edit-folder-dialog .btn-confirm-folder {
  background: #BB1919;
  border-color: #BB1919;
}
.edit-folder-dialog .btn-confirm-folder:hover {
  background: #9e1515;
  border-color: #9e1515;
}
.folder-desc {
  color: #666;
  margin-bottom: 2px;
}
.folder-meta {
  font-size: 13px;
  color: #999;
  margin: 0;
}
.blog-filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.blog-sort {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  user-select: none;
}
.blog-sort:hover,
.blog-sort.active {
  color: #BB1919;
}
.blog-sort--dropdown {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.blog-sort-arrow {
  font-size: 12px;
  color: #999;
}
.blog-sort--dropdown:hover .blog-sort-arrow {
  color: #BB1919;
}
.blog-tab-wrap,
.dynamic-tab-wrap,
.column-tab-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.column-list .profile-card-item--column {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.column-list .profile-card-item--column .profile-card-body {
  flex: 1;
  min-width: 0;
}
.column-list .profile-card-item--column .profile-card-title {
  margin-top: 0;
}
.column-list .column-cover {
  flex-shrink: 0;
  width: 120px;
  height: 84px;
  border-radius: 6px;
  overflow: hidden;
  background: #e8e8e8;
  display: block;
}
.column-list .column-cover .profile-card-thumb-img,
.column-list .column-cover .profile-card-thumb-ph {
  width: 100%;
  height: 100%;
  display: block;
}
.column-list .column-cover .profile-card-thumb-img {
  object-fit: cover;
}
.column-list .column-cover .profile-card-thumb-ph {
  background: linear-gradient(135deg, #e0e0e0 0%, #eee 100%);
}
.dynamic-sentinel {
  height: 1px;
  visibility: hidden;
  pointer-events: none;
}
.dynamic-load-more {
  padding: 16px 0;
  margin-top: 0;
}
.dynamic-no-more {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 16px 0;
  margin: 0;
}
.blog-loading {
  padding: 24px 0;
  text-align: center;
  color: #888;
  font-size: 14px;
}
.blog-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}
.blog-empty-icon {
  font-size: 80px;
  color: #d0d0d0;
  margin-bottom: 16px;
}
.blog-empty-text {
  margin: 0;
  font-size: 15px;
  color: #999;
}
.blog-pagination-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}
.blog-total-text {
  font-size: 14px;
  color: var(--el-text-color-regular, #606266);
  line-height: 32px;
}
.blog-total-num {
  display: inline-block;
  transform: translateY(-2px);
}
.blog-pagination {
  justify-content: flex-start;
}
.blog-pagination :deep(.el-pager li.is-active) {
  background-color: #BB1919;
  color: #fff;
}

.activity-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.activity-action {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px;
}

.activity-time {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px;
}

.activity-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #111;
  margin-bottom: 8px;
  text-decoration: none;
}

.activity-title:hover {
  color: #BB1919;
}

.activity-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.activity-author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #666;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.activity-author-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.activity-author-desc {
  font-size: 13px;
  color: #888;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

/* 右侧栏 */
.profile-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.profile-sidebar > :deep(.creation-center) {
  margin-bottom: 16px;
}

.sidebar-stats {
  display: flex;
  padding: 20px 0;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-top: 3px solid #BB1919;
  border-radius: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.stat-item:first-child {
  border-right: 1px solid #e8e8e8;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #111;
}

.stat-label {
  font-size: 13px;
  color: #888;
}
</style>

<style>
/* 编辑资料弹窗：弹窗被 teleport 到 body，scoped 不生效，此处强制所有框红色边框（无蓝） */
.edit-profile-dialog .el-dialog__body {
  --el-color-primary: #BB1919;
  --el-input-border-color: #dcdfe6;
  --el-input-focus-border-color: #BB1919;
  --el-input-hover-border-color: #BB1919;
}
.edit-profile-dialog .el-input__wrapper {
  box-shadow: 0 0 0 1px #dcdfe6 inset !important;
}
.edit-profile-dialog .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px #BB1919 inset !important;
}
.edit-profile-dialog .el-input.is-focus .el-input__wrapper,
.edit-profile-dialog .el-input__wrapper:focus-within,
.edit-profile-dialog .el-select .el-input.is-focus .el-input__wrapper,
.edit-profile-dialog .el-select .el-input__wrapper:focus-within,
.edit-profile-dialog .el-cascader .el-input.is-focus .el-input__wrapper,
.edit-profile-dialog .el-cascader .el-input__wrapper:focus-within {
  box-shadow: 0 0 0 1px #BB1919 inset !important;
}
.edit-profile-dialog .el-textarea__inner {
  box-shadow: 0 0 0 1px #dcdfe6 inset !important;
}
.edit-profile-dialog .el-textarea__inner:hover {
  box-shadow: 0 0 0 1px #BB1919 inset !important;
}
.edit-profile-dialog .el-textarea.is-focus .el-textarea__inner,
.edit-profile-dialog .el-textarea__inner:focus {
  box-shadow: 0 0 0 1px #BB1919 inset !important;
}

/* 编辑资料弹窗内下拉：全部蓝色改红色、选中背景、去掉圆形图标（下拉挂到 body，故用非 scoped） */
.edit-profile-cascader-popper {
  --el-color-primary: #BB1919;
  --el-color-primary-light-3: rgba(187, 25, 25, 0.5);
  --el-color-primary-light-5: rgba(187, 25, 25, 0.3);
  --el-color-primary-light-7: rgba(187, 25, 25, 0.15);
  --el-color-primary-light-8: rgba(187, 25, 25, 0.12);
  --el-color-primary-light-9: rgba(187, 25, 25, 0.08);
  --el-cascader-menu-selected-text-color: #BB1919;
}
.edit-profile-cascader-popper .el-cascader-node.in-checked,
.edit-profile-cascader-popper .el-cascader-node.is-active {
  color: #BB1919;
  background-color: rgba(187, 25, 25, 0.12);
}
.edit-profile-cascader-popper .el-cascader-node.in-checked:hover,
.edit-profile-cascader-popper .el-cascader-node.is-active:hover {
  background-color: rgba(187, 25, 25, 0.18);
}
.edit-profile-cascader-popper .el-radio__inner,
.edit-profile-cascader-popper .el-radio__input {
  display: none !important;
}
.edit-profile-select-popper {
  --el-color-primary: #BB1919;
}
.edit-profile-select-popper .el-select-dropdown__item.is-selected {
  color: #BB1919;
}
.edit-profile-select-popper .el-select-dropdown__item.is-selected::after {
  background-color: #BB1919;
}
.edit-profile-select-popper .el-select-dropdown__item.is-selected .el-icon {
  display: none !important;
}
.edit-profile-select-popper .el-select-dropdown__item.is-hovering {
  background-color: rgba(187, 25, 25, 0.08);
}
.edit-profile-cascader-popper .el-cascader-node:hover {
  background-color: rgba(187, 25, 25, 0.08);
}

/* 我的博客 - 可见性下拉：红色主题，去掉蓝色 */
.blog-visibility-dropdown {
  --el-color-primary: #BB1919;
}
.blog-visibility-dropdown .el-dropdown-menu__item {
  color: #606266;
}
.blog-visibility-dropdown .el-dropdown-menu__item:hover {
  color: #BB1919;
  background-color: rgba(187, 25, 25, 0.08);
}
</style>
