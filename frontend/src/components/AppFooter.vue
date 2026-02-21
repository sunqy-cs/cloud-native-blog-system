<template>
  <footer class="app-footer">
    <div class="app-footer-line"></div>
    <div class="app-footer-inner">
      <!-- Logo：与顶部一致 -->
      <div class="app-footer-logo">
        <router-link to="/">
          <img src="/logo3.png" alt="云原生博客 Logo" class="app-footer-logo-img" />
        </router-link>
      </div>

      <!-- 主链接行 -->
      <nav class="app-footer-nav">
        <router-link to="/">首页</router-link>
        <router-link to="/recommend">推荐</router-link>
        <router-link to="/follow">关注</router-link>
        <router-link to="/hot">热榜</router-link>
        <router-link to="/creator/write">写文章</router-link>
        <router-link to="/creator">创作中心</router-link>
        <a href="#">知识库</a>
        <a href="#">设置</a>
      </nav>

      <!-- 页脚说明（纯文案，无跳转） -->
      <p class="app-footer-misc">
        本站内容仅供参考。基于云原生技术构建，欢迎交流与反馈。
      </p>
      <p class="app-footer-copy">
        © 2026 博客云. 保留所有权利。
      </p>
    </div>
  </footer>
</template>

<script setup lang="ts">
</script>

<style scoped>
.app-footer {
  background: #fff;
  width: 100%;
}

.app-footer-line {
  height: 1px;
  background: #e0e0e0;
  margin: 0;
}

.app-footer-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.app-footer-logo {
  margin-bottom: 20px;
}

.app-footer-logo a {
  display: inline-block;
}

.app-footer-logo-img {
  height: 36px;
  width: auto;
  display: block;
}

.app-footer-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  margin-bottom: 16px;
}

.app-footer-nav a {
  font-size: 15px;
  color: #1a1a1a;
  text-decoration: none;
}

.app-footer-nav a:hover {
  text-decoration: underline;
}

.app-footer-misc {
  font-size: 13px;
  color: #888;
  margin: 0 0 8px;
  line-height: 1.5;
}

.app-footer-copy {
  font-size: 12px;
  color: #888;
  margin: 0;
  line-height: 1.5;
}
</style>
