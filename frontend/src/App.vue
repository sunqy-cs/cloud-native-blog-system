<template>
  <div class="app-root">
    <AppHeader v-if="!isCreatorRoute" :open-login-modal="openLoginModal" />
    <router-view />
    <LoginModal v-model:visible="showLoginModal" :redirect="loginRedirect" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import LoginModal from '@/components/LoginModal.vue'
import { pendingLogin } from '@/stores/loginModal'

const route = useRoute()
const showLoginModal = ref(false)
const isCreatorRoute = computed(() => route.path.startsWith('/creator'))
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
</script>

<style>
#app {
  min-height: 100vh;
}

.app-root {
  min-height: 100vh;
  background-color: var(--el-bg-color-page, #f5f5f5);
}
</style>
