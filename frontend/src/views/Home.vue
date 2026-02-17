<template>
  <div class="home">
    <el-container>
      <el-header class="header">
        <div class="logo">云原生博客</div>
        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          router
          class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item v-if="!userStore.isLoggedIn" index="/login">登录</el-menu-item>
          <el-menu-item v-else index="/admin">后台</el-menu-item>
        </el-menu>
      </el-header>
      <el-main>
        <h1>欢迎</h1>
        <p>前端框架已就绪，可在此扩展首页与文章列表。</p>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.home { min-height: 100vh; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color);
}
.logo { font-size: 1.25rem; font-weight: 600; }
.nav-menu { flex: 1; justify-content: flex-end; }
.el-main { padding: 2rem; }
</style>
