<template>
  <div class="page-placeholder">
    <main class="page-main">
      <h1 class="title">推荐</h1>
      <p class="desc">此页面为占位，后续补充推荐内容等。</p>
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.page-placeholder {
  min-height: calc(100vh - 64px);
}

.page-main {
  max-width: 960px;
  margin: 24px auto;
  padding: 24px;
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
