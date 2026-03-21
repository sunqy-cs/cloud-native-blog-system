<template>
  <div class="app-root" :class="{ 'app-root-knowledge': isKnowledgeRoute }">
    <AppHeader v-if="!isStandaloneWorkspace" :open-login-modal="openLoginModal" />
    <router-view />
    <AppFooter v-if="!isStandaloneWorkspace && !isKnowledgeRoute" />
    <LoginModal v-model:visible="showLoginModal" :redirect="loginRedirect" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'
import { getMe } from '@/api/user'
import AppFooter from '@/components/AppFooter.vue'
import LoginModal from '@/components/LoginModal.vue'
import { pendingLogin } from '@/stores/loginModal'

const route = useRoute()
const userStore = useUserStore()
const showLoginModal = ref(false)
/** 创作者中心 / 审核中心：独立顶栏，不显示全局 AppHeader 与页脚 */
const isStandaloneWorkspace = computed(
  () => route.path.startsWith('/creator') || route.path.startsWith('/audit')
)
const isKnowledgeRoute = computed(() => route.path.startsWith('/knowledge'))
const loginRedirect = ref('')

function openLoginModal(redirect?: string) {
  loginRedirect.value = redirect ?? route.fullPath
  showLoginModal.value = true
}

watch(pendingLogin, (redirect) => {
  if (redirect) {
    loginRedirect.value = redirect
    showLoginModal.value = true
    pendingLogin.value = null
  }
}, { immediate: true })

watch(() => route.query.login, (v) => {
  if (v === '1') {
    loginRedirect.value = (route.query.redirect as string) || '/recommend'
    showLoginModal.value = true
  }
}, { immediate: true })

// 已登录时补全资料：头像、role（管理员顶栏「审核」依赖 ADMIN）
onMounted(() => {
  if (!userStore.isLoggedIn) return
  const info = userStore.userInfo as { avatar?: string; role?: string } | null
  if (!info?.avatar || info.role == null || info.role === '') {
    getMe().then((u) => userStore.setUserInfo(u)).catch(() => {})
  }
})
</script>

<style>
#app {
  min-height: 100vh;
}

.app-root {
  min-height: 100vh;
  background-color: var(--el-bg-color-page, #f5f5f5);
}

/* 知识库页面：整页禁止滚动，仅左侧栏可滚动（同时隐藏页脚避免撑高） */
.app-root.app-root-knowledge {
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-root.app-root-knowledge .knowledge-page {
  flex: 1;
  min-height: 0;
}
</style>
