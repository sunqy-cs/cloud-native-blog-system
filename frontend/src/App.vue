<template>
  <div class="app-root">
    <AppHeader />
    <router-view />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
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
