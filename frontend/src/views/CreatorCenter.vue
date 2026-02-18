<template>
  <div class="creator-layout">
    <!-- 顶部栏 -->
    <header class="creator-header">
      <div class="header-left">
        <router-link to="/recommend" class="logo-link">
          <img src="/logo3.png" alt="Logo" class="creator-logo" />
        </router-link>
        <span class="creator-title">创作者中心</span>
      </div>
      <div class="header-right">
        <span class="creator-avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-img" />
          <span v-else class="avatar-initial">{{ avatarInitial }}</span>
        </span>
      </div>
    </header>

    <!-- 主体：左侧栏 + 中间 + 右侧 -->
    <div class="creator-body">
      <aside class="creator-sidebar-left">
        <el-button class="create-btn" type="primary">
          <el-icon><Plus /></el-icon>
          创作
        </el-button>
        <nav class="creator-nav">
          <router-link to="/creator" :class="['nav-item', { active: route.path === '/creator' }]">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </router-link>
        </nav>
      </aside>

      <main class="creator-main">
        <!-- 用户信息卡片 -->
        <div class="card profile-card">
          <div class="profile-left">
            <span class="profile-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
              <span v-else class="avatar-initial">{{ avatarInitial }}</span>
            </span>
            <div class="profile-info">
              <div class="profile-name">
                {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
              </div>
              <div class="profile-stats">
                <span>0 原创</span>
                <span>0 粉丝</span>
                <span>0 积分</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 数据指标卡片 -->
        <div class="card metrics-card">
          <div class="metric-item">
            <div class="metric-value">0</div>
            <div class="metric-label">总阅读量</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">0</div>
            <div class="metric-label">累计收益</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">0</div>
            <div class="metric-label">粉丝数</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">0</div>
            <div class="metric-label">收藏数</div>
          </div>
        </div>

        <!-- 近期文章 -->
        <div class="card articles-card">
          <div class="card-header">
            <span class="card-title">近期文章</span>
            <span class="card-action">查看全部 &gt;</span>
          </div>
          <el-table :data="articles" stripe style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="views" label="展现" width="100" />
            <el-table-column prop="reads" label="阅读" width="100" />
            <el-table-column prop="comments" label="评论" width="100" />
            <el-table-column prop="likes" label="点赞" width="100" />
            <el-table-column prop="collections" label="收藏" width="100" />
          </el-table>
        </div>
      </main>

      <aside class="creator-sidebar-right">
        <!-- 右侧占位，后续补充 -->
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Plus, House } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const articles = ref<any[]>([])

const avatarUrl = computed(() => (userStore.userInfo as { avatar?: string })?.avatar || '')
const avatarInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || '用'
  return name.charAt(0).toUpperCase()
})
</script>

<style scoped>
.creator-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.creator-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  text-decoration: none;
}

.creator-logo {
  height: 40px;
  width: auto;
  transition: transform 0.2s ease;
}

.logo-link:hover .creator-logo {
  transform: scale(1.08);
}

.creator-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
}

.creator-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #111;
  overflow: hidden;
}

.creator-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-avatar .avatar-initial {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.creator-body {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
}

.creator-sidebar-left {
  width: 200px;
  flex-shrink: 0;
}

.create-btn {
  width: 100%;
  margin-bottom: 20px;
  background: #111 !important;
  border-color: #111 !important;
}

.create-btn:hover {
  background: #333 !important;
  border-color: #333 !important;
}

.creator-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  color: #444;
  text-decoration: none;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.nav-item:hover {
  background: #f0f0f0;
  color: #111;
}

.nav-item.active {
  background: #f0f0f0;
  color: #111;
  font-weight: 500;
}

.creator-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.creator-sidebar-right {
  width: 280px;
  flex-shrink: 0;
}

.card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  border: 1px solid #e8e8e8;
}

.profile-card {
  display: flex;
  align-items: center;
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar .avatar-initial {
  color: #fff;
  font-size: 24px;
  font-weight: 600;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: #111;
  margin-bottom: 8px;
}

.profile-stats {
  font-size: 14px;
  color: #666;
}

.profile-stats span {
  margin-right: 16px;
}

.metrics-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.metric-item {
  text-align: center;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}

.metric-label {
  font-size: 14px;
  color: #666;
}

.articles-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111;
}

.card-action {
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.card-action:hover {
  color: #111;
}
</style>
