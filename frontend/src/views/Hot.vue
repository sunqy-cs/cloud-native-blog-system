<template>
  <div class="page-layout">
    <main class="page-main">
      <h1 class="title">热榜</h1>
      <p class="desc">此页面为占位，后续补充热榜排行等内容。</p>
    </main>
    <aside class="page-sidebar">
      <CreationCenter />
    </aside>
  </div>
</template>

<script setup lang="ts">
import CreationCenter from '@/components/CreationCenter.vue'
</script>

<style scoped>
.page-layout {
  min-height: calc(100vh - 64px);
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
}

.page-main {
  flex: 1;
  min-width: 0;
}

.page-sidebar {
  flex-shrink: 0;
}

.title {
  font-size: 28px;
  margin-bottom: 12px;
}

.desc {
  color: var(--el-text-color-secondary);
  line-height: 1.8;
}
</style>
