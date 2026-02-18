<template>
  <header class="app-header">
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
            <el-menu-item index="/follow"><span class="nav-text">关注</span></el-menu-item>
            <el-menu-item index="/recommend"><span class="nav-text">推荐</span></el-menu-item>
            <el-menu-item index="/hot"><span class="nav-text">热榜</span></el-menu-item>
            <el-menu-item index="/knowledge"><span class="nav-text">知识库</span></el-menu-item>
          </el-menu>
          <div class="nav-indicator" :style="indicatorStyle"></div>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索文章、标签或作者"
          class="search-input"
          size="large"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </nav>

      <!-- 右侧：操作区（简化版） -->
      <div class="action-area">
        <el-button text @click="goWrite">写文章</el-button>
        <el-button
          v-if="!userStore.isLoggedIn"
          type="primary"
          @click="goLogin"
        >
          登录 / 注册
        </el-button>
        <el-dropdown v-else>
          <span class="user-dropdown">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goAdmin">进入后台</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
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
import { Search, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const menuWrapperRef = ref<HTMLElement | null>(null)
const indicatorStyle = ref({ left: '0px', width: '0px' })

function updateIndicator() {
  nextTick(() => {
    const wrapper = menuWrapperRef.value
    const activeItem = wrapper?.querySelector('.el-menu-item.is-active')
    if (!wrapper || !activeItem) return
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
  if (path.startsWith('/login')) return '/login'
  if (path === '/blog' || path.startsWith('/article')) return '/blog'
  return path
})

watch(activeMenu, updateIndicator)
onMounted(() => {
  updateIndicator()
  window.addEventListener('resize', updateIndicator)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIndicator)
})

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function goAdmin() {
  router.push('/admin')
}

function logout() {
  userStore.logout()
  router.push('/')
}

function goWrite() {
  // 先简单跳后台文章管理，后续可单独做写作页
  router.push('/admin/articles')
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
  background: #000;
  transition: left 0.4s ease, width 0.4s ease;
}

.search-input {
  max-width: 320px;
  /* 在中间区域尽量居中，左右留一点空隙 */
  margin: 0 auto;
}

.action-area {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 24px;
}

.user-dropdown {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
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
  transition: font-weight 0.15s ease, color 0.15s ease, background-color 0.15s ease;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item.is-active) {
  color: #000 !important;
  font-weight: 900;
  border-bottom-color: transparent !important;
  -webkit-text-stroke: 0.25px #000;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item .nav-text) {
  display: inline-block;
}

.app-header :deep(.el-menu--horizontal > .el-menu-item:hover) {
  background-color: rgba(0, 0, 0, 0.03);
  font-weight: 900; /* 悬停时与选中态同粗 */
  color: #000 !important;
}

/* 搜索框：去掉 Element Plus 默认的蓝色高亮，用黑/灰色替代 */
.search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 20px;
}

.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #000 inset;
  border-color: #000;
}

.search-input :deep(.el-input__inner) {
  color: #222;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: #999;
}

.action-area :deep(.el-button--text) {
  color: #333;
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

