<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="aside">
      <div class="logo">后台</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/admin/articles">文章管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ $route.meta.title ?? '后台' }}</span>
        <el-button text @click="userStore.logout(); $router.push('/')">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.admin-layout { height: 100vh; }
.aside { background: #304156; }
.logo { height: 50px; line-height: 50px; text-align: center; color: #fff; font-weight: 600; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color);
}
.el-main { background: var(--el-fill-color-light); }
</style>
