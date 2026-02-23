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
        <h2 class="section-title">我的动态</h2>
        <div class="activity-list">
          <article v-for="item in activities" :key="item.id" class="activity-item">
            <p class="activity-action">{{ item.actionText }}</p>
            <p class="activity-time">{{ item.time }}</p>
            <router-link :to="`/article/${item.contentId}`" class="activity-title">{{ item.title }}</router-link>
            <div class="activity-author">
              <span class="activity-author-avatar">{{ item.authorName.charAt(0) }}</span>
              <span class="activity-author-name">{{ item.authorName }}</span>
              <span v-if="item.authorDesc" class="activity-author-desc">{{ item.authorDesc }}</span>
            </div>
          </article>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import CreationCenter from '@/components/CreationCenter.vue'
import { Camera, Search, ArrowDown, ArrowUp, Plus, Loading, Location, Briefcase, User, Document, ChatDotRound } from '@element-plus/icons-vue'
import { getMe, updateMe, type UpdateProfilePayload } from '@/api/user'
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
})

const tabs = ref([
  { key: 'dynamic', label: '动态', count: undefined },
  { key: 'answers', label: '回答', count: 0 },
  { key: 'videos', label: '视频', count: 0 },
  { key: 'questions', label: '提问', count: 0 },
  { key: 'articles', label: '文章', count: 0 },
  { key: 'columns', label: '专栏', count: 0 },
  { key: 'ideas', label: '想法', count: 0 },
  { key: 'collections', label: '收藏', count: 1 },
  { key: 'highlights', label: '划线', count: 0 },
])

const activities = ref([
  {
    id: '1',
    actionText: '赞同了文章',
    time: '2026-02-23 11:33',
    title: '硅谷公司核心高管残酷预言:会做事的人越来越值钱,会管人的正在贬值',
    authorName: '非著名程序员',
    authorDesc: '公众号「非著名程序员」和「MOSS 进化论」主理人。',
    contentId: '1',
  },
])

const followingCount = ref(4)
const followerCount = ref(1)
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

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.activity-item {
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
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
  gap: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
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
</style>
