<template>
  <header class="app-header" :class="{ 'app-header--audit': showAuditButton }">
    <div class="header-inner">
      <!-- 左侧：Logo 占位 -->
      <div class="logo-area">
        <!-- 使用 public/logo.png 作为站点 Logo -->
        <img src="/logo3.png" alt="云原生博客 Logo" class="logo-img" />
      </div>

      <!-- 中间：导航 + 搜索（参考知乎） -->
      <nav class="nav-area">
        <div class="nav-menu-wrapper" ref="menuWrapperRef">
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="nav-menu"
            :ellipsis="false"
          >
            <el-menu-item index="/blog"><span class="nav-text">博客</span></el-menu-item>
            <el-menu-item index="/recommend"><span class="nav-text">推荐</span></el-menu-item>
            <el-menu-item index="/follow"><span class="nav-text">关注</span></el-menu-item>
            <el-menu-item index="/hot"><span class="nav-text">热榜</span></el-menu-item>
            <el-menu-item index="/knowledge"><span class="nav-text">知识库</span></el-menu-item>
          </el-menu>
          <div class="nav-indicator" :style="indicatorStyle"></div>
        </div>
        <div
          class="search-box"
          :class="{ hover: searchBoxHover }"
          @mouseenter="searchBoxHover = true"
          @mouseleave="searchBoxHover = false"
        >
          <input
            v-model="keyword"
            type="text"
            class="search-box-input"
            placeholder="搜索文章、标签或作者"
            @keyup.enter="onSearch"
          />
          <button type="button" class="search-box-btn" title="搜索" @click="onSearch">
            <el-icon><Search /></el-icon>
          </button>
        </div>
      </nav>

      <!-- 右侧：操作区（BBC 风格）；管理员在「创作」左侧显示「审核」 -->
      <div class="action-area">
        <div class="action-primary-btns">
          <button
            v-if="showAuditButton"
            type="button"
            class="btn-create btn-audit"
            @click="goAudit"
          >
            <span class="btn-create-text">审核</span>
          </button>
          <button type="button" class="btn-create" @click="goWrite">
            <span class="btn-create-plus">+</span>
            <span class="btn-create-text">创作</span>
          </button>
        </div>
        <el-button
          v-if="!userStore.isLoggedIn"
          type="primary"
          @click="goLogin"
        >
          登录 / 注册
        </el-button>
        <button
          v-if="userStore.isLoggedIn"
          type="button"
          class="btn-inbox"
          title="收件箱"
          @click="goInbox"
        >
          <el-icon><Bell /></el-icon>
          <span v-if="unreadCount > 0" class="inbox-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
        <el-dropdown v-if="userStore.isLoggedIn" popper-class="app-header-user-dropdown" @command="onDropdownCommand">
          <span class="user-dropdown">
            <span class="user-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-img" />
              <span v-else class="avatar-initial">{{ avatarInitial }}</span>
            </span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人主页</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Search, ArrowDown, Bell } from '@element-plus/icons-vue'
import { getUnreadMessageCount } from '@/api/userMessage'

const props = defineProps<{
  openLoginModal?: (redirect?: string) => void
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showAuditButton = computed(() => userStore.isLoggedIn && userStore.isAdmin)

const keyword = ref('')
const searchBoxHover = ref(false)
const menuWrapperRef = ref<HTMLElement | null>(null)
const unreadCount = ref(0)

// 在搜索页时同步 URL 关键词；从搜索页切到其他主导航时清空输入框
watch(
  () => ({ path: route.path, q: route.query.q }),
  (to, from) => {
    if (to.path === '/search') {
      const q = to.q
      keyword.value = typeof q === 'string' ? q : (Array.isArray(q) ? q[0] ?? '' : '')
    } else if (from?.path === '/search') {
      keyword.value = ''
    }
  },
  { immediate: true }
)

function onSearch() {
  const q = keyword.value.trim()
  router.push({ path: '/search', query: q ? { q } : {} })
}
const indicatorStyle = ref({ left: '0px', width: '0px' })

function updateIndicator() {
  nextTick(() => {
    const wrapper = menuWrapperRef.value
    const activeItem = wrapper?.querySelector('.el-menu-item.is-active')
    if (!wrapper || !activeItem) {
      indicatorStyle.value = { left: '0px', width: '0px' }
      return
    }
    const textEl = activeItem.querySelector('.nav-text') || activeItem
    const rect = textEl.getBoundingClientRect()
    const wrapperRect = wrapper.getBoundingClientRect()
    indicatorStyle.value = {
      left: `${rect.left - wrapperRect.left}px`,
      width: `${rect.width}px`,
    }
  })
}

const activeMenu = computed(() => {
  // 只高亮我们现有的几个主导航
  const path = route.path
  if (path.startsWith('/admin')) return '/admin'
  if (path === '/blog' || path.startsWith('/article')) return '/blog'
  return path
})

const avatarUrl = computed(() => (userStore.userInfo as { avatar?: string })?.avatar || '')
const avatarInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || '用'
  return name.charAt(0).toUpperCase()
})

watch(activeMenu, updateIndicator)
onMounted(() => {
  updateIndicator()
  window.addEventListener('resize', updateIndicator)
  refreshUnreadCount()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIndicator)
})

watch(() => userStore.isLoggedIn, () => {
  refreshUnreadCount()
}, { immediate: true })

function goLogin() {
  props.openLoginModal?.(route.fullPath)
}

function goProfile() {
  if (!userStore.isLoggedIn) {
    props.openLoginModal?.('/profile')
    return
  }
  router.push('/profile')
}

function onDropdownCommand(command: string) {
  if (command === 'profile') goProfile()
  else if (command === 'logout') logout()
}

function logout() {
  userStore.logout()
  router.push('/')
}

function goWrite() {
  if (!userStore.isLoggedIn) {
    props.openLoginModal?.('/creator')
    return
  }
  router.push('/creator')
}

function goAudit() {
  if (!userStore.isLoggedIn) {
    props.openLoginModal?.('/audit')
    return
  }
  if (!userStore.isAdmin) return
  router.push('/audit')
}

function goInbox() {
  router.push('/inbox')
}

async function refreshUnreadCount() {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  try {
    const data: any = await getUnreadMessageCount()
    unreadCount.value = Number(data?.count ?? data?.data?.count ?? 0)
  } catch {
    unreadCount.value = 0
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: #ffffff;
  /* 浅灰分割线，类似 BBC 顶栏风格 */
  border-bottom: 1px solid #e0e0e0;
}

.header-inner {
  /* 占满整行，两边留一点内边距，避免太拥挤 */
  width: 100%;
  margin: 0;
  padding: 0 24px;
  display: flex;
  align-items: center;
  height: 64px;
}

.logo-area {
  display: flex;
  align-items: center;
  margin-right: 24px;
  cursor: pointer;
}

.logo-img {
  height: 40px;
  width: auto;
  display: block;
  transition: transform 0.2s ease;
}

.logo-area:hover .logo-img {
  transform: scale(1.08);
}

.nav-area {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0; /* 右侧双按钮时允许中间区收缩 */
}

.app-header--audit .nav-menu-wrapper {
  flex-shrink: 0;
}

.app-header--audit .search-box {
  min-width: 200px;
  max-width: min(380px, 32vw);
}

.nav-menu-wrapper {
  position: relative;
  display: flex;
}

.nav-menu {
  border-bottom: none;
}

.nav-indicator {
  position: absolute;
  bottom: 10px;
  height: 4px;
  background: #BB1919; /* BBC 红 */
  transition: left 0.4s ease, width 0.4s ease;
}

/* 搜索框：白底 + 右侧黑按钮，悬浮边线加粗 */
.search-box {
  min-width: 380px;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  align-items: stretch;
  height: 40px;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  transition: border-width 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.search-box:hover,
.search-box.hover {
  border-width: 2px;
  border-color: #333;
  box-shadow: 0 0 0 1px #333;
}

.search-box-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 14px;
  border: none;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  color: #222;
  background: transparent;
  outline: none;
}

.search-box-input::placeholder {
  color: #9ca3af;
}

.search-box-btn {
  width: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-left: 1px solid #d0d0d0;
  border-radius: 0 4px 4px 0;
  background: #111;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}

.search-box-btn:hover {
  background: #333;
}

.search-box-btn .el-icon {
  font-size: 18px;
}

.action-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 16px;
  flex-shrink: 0;
}

.action-primary-btns {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-audit {
  padding: 0 14px;
}

.btn-inbox {
  position: relative;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  background: #fff;
  color: #222;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-inbox:hover {
  border-color: #bb1919;
  color: #bb1919;
  background: #fff5f5;
}

.btn-inbox .el-icon {
  font-size: 18px;
}

.inbox-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #bb1919;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  box-sizing: border-box;
}

/* +创作：BBC 风格主按钮 */
.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: #BB1919;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}

.btn-create:hover {
  background: #9e1515;
}

.btn-create:active {
  transform: scale(0.98);
}

.btn-create-plus {
  font-size: 18px;
  line-height: 1;
  font-weight: 400;
  opacity: 0.95;
}

.btn-create-text {
  letter-spacing: 0.02em;
}

.user-dropdown {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}

.action-area :deep(.el-dropdown__trigger) {
  outline: none !important;
  border: none !important;
  box-shadow: none !important;
}

.action-area :deep(.el-dropdown__trigger:focus),
.action-area :deep(.el-dropdown__trigger:focus-visible) {
  outline: none !important;
  border: none !important;
  box-shadow: none !important;
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #111;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initial {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

/* ---- 英伦黑白风格：重写 Element Plus 顶栏配色 ---- */

.app-header :deep(.el-menu--horizontal) {
  background-color: transparent;
  border-bottom: none;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item) {
  color: #444;
  font-weight: 500;
  font-size: 16px;
  border-bottom: 2px solid transparent !important; /* 覆盖 Element 默认，避免切换时闪黑线 */
  background-color: transparent !important;
  transition: font-weight 0.15s ease, color 0.15s ease;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item.is-active) {
  color: #000 !important;
  font-weight: 900;
  border-bottom-color: transparent !important;
  background-color: transparent !important;
  -webkit-text-stroke: 0.25px #000;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item:focus),
.app-header :deep(.el-menu--horizontal > .el-menu-item:focus-visible) {
  background-color: transparent !important;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item .nav-text) {
  display: inline-block;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item:hover) {
  background-color: rgba(0, 0, 0, 0.03) !important;
  font-weight: 900; /* 悬停时与选中态同粗 */
  color: #000 !important;
}


.action-area :deep(.el-button--primary) {
  background-color: #111;
  border-color: #111;
}

.action-area :deep(.el-button--primary:hover) {
  background-color: #333;
  border-color: #333;
}
</style>

<!-- 用户下拉框样式（popper 挂载到 body，需全局样式，与顶栏白/灰/红风格一致） -->
<style>
.app-header-user-dropdown.el-dropdown__popper .el-dropdown-menu__item {
  color: #333;
}
.app-header-user-dropdown.el-dropdown__popper .el-dropdown-menu__item:hover,
.app-header-user-dropdown.el-dropdown__popper .el-dropdown-menu__item:focus {
  background-color: #f5f5f5;
  color: #111;
}
.app-header-user-dropdown.el-dropdown__popper .el-dropdown-menu__item:not(.is-divided) {
  border-left: 3px solid transparent;
}
.app-header-user-dropdown.el-dropdown__popper .el-dropdown-menu__item:hover:not(.is-divided) {
  border-left-color: #BB1919;
}
</style>

