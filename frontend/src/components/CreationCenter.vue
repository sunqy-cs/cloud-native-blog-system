<template>
  <aside class="creation-center">
    <div class="creation-header">
      <h3 class="creation-title">
        <el-icon class="creation-icon"><EditPen /></el-icon>
        创作中心
      </h3>
      <span class="creation-drafts" @click="goDrafts">草稿箱 ({{ draftCount }})</span>
    </div>
    <p class="creation-desc">
      开启你的创作之旅，记录思考与见解。
    </p>
    <el-button class="creation-btn" type="primary" @click="goWrite">
      + 开始创作
    </el-button>
  </aside>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { requestLogin } from '@/stores/loginModal'

const router = useRouter()
const userStore = useUserStore()
const draftCount = 0

function goWrite() {
  if (!userStore.isLoggedIn) {
    requestLogin('/creator')
    return
  }
  router.push('/creator')
}

function goDrafts() {
  if (!userStore.isLoggedIn) {
    requestLogin('/creator')
    return
  }
  router.push('/creator')
}
</script>

<style scoped>
.creation-center {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-top: 3px solid #000;
  border-radius: 0;
  padding: 20px;
  min-width: 280px;
}

.creation-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.creation-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Georgia', 'Times New Roman', 'Songti SC', serif;
  font-size: 18px;
  font-weight: 700;
  color: #000;
  letter-spacing: 0.02em;
}

.creation-icon {
  font-size: 20px;
}

.creation-drafts {
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.creation-drafts:hover {
  color: #000;
}

.creation-desc {
  font-size: 14px;
  color: #444;
  line-height: 1.6;
  margin-bottom: 16px;
}

.creation-btn {
  width: 100%;
  background-color: #111 !important;
  border-color: #111 !important;
  font-weight: 500;
}

.creation-btn:hover {
  background-color: #333 !important;
  border-color: #333 !important;
}
</style>
