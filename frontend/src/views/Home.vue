<template>
  <div class="home">
    <main class="home-main">
      <h1 class="title">欢迎</h1>
      <p class="desc">
        前端框架已就绪，下一步可以在这里接入文章列表、推荐内容等。
      </p>
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home {
  min-height: calc(100vh - 56px);
  /* 56px 对应顶部导航栏高度，避免视觉上“挤在一起” */
}

.home-main {
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
