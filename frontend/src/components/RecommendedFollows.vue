<template>
  <aside class="recommended-follows">
    <div class="rec-follows-header">
      <el-icon class="rec-follows-icon"><UserFilled /></el-icon>
      <span class="rec-follows-title">推荐关注</span>
    </div>
    <ul class="rec-follows-list">
      <li
        v-for="user in currentPageList"
        :key="user.id"
        class="rec-follows-item"
      >
        <router-link :to="user.link" class="rec-follows-avatar-wrap">
          <img
            v-if="user.avatar"
            :src="user.avatar"
            :alt="user.name"
            class="rec-follows-avatar"
          />
          <span v-else class="rec-follows-avatar rec-follows-avatar-placeholder">
            {{ user.name.charAt(0) }}
          </span>
        </router-link>
        <div class="rec-follows-info">
          <router-link :to="user.link" class="rec-follows-name">{{ user.name }}</router-link>
          <p class="rec-follows-desc">{{ user.description }}</p>
        </div>
        <el-button
          type="primary"
          link
          class="rec-follows-btn"
          @click.prevent="follow(user)"
        >
          <el-icon class="btn-icon"><Plus /></el-icon>
          关注
        </el-button>
      </li>
    </ul>
    <div class="rec-follows-pagination">
      <button
        v-for="p in totalPages"
        :key="p"
        type="button"
        class="rec-follows-page-num"
        :class="{ active: currentPage === p }"
        @click="currentPage = p"
      >
        {{ p }}
      </button>
      <button
        type="button"
        class="rec-follows-page-next"
        :disabled="currentPage >= totalPages"
        @click="currentPage = Math.min(currentPage + 1, totalPages)"
      >
        <el-icon><ArrowRight /></el-icon>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { UserFilled, Plus, ArrowRight } from '@element-plus/icons-vue'

export interface RecommendedUser {
  id: string
  name: string
  description: string
  avatar?: string
  link?: string
}

const pageSize = 4
const currentPage = ref(1)

const mockUsers: RecommendedUser[] = [
  { id: '1', name: 'Tammy', description: '「汽车」领域答主', link: '/follow' },
  { id: '2', name: '水起波澜', description: '你看过 TA', link: '/follow' },
  { id: '3', name: 'ofo 在逃单车', description: '「科技」领域答主', link: '/follow' },
  { id: '4', name: '跨境小码农', description: '你看过 TA', link: '/follow' },
  { id: '5', name: '云原生实践者', description: '「技术」领域答主', link: '/follow' },
  { id: '6', name: '写作与表达', description: '你看过 TA', link: '/follow' },
  { id: '7', name: '开源爱好者', description: '「开源」领域答主', link: '/follow' },
  { id: '8', name: '产品思考', description: '你看过 TA', link: '/follow' },
]

const totalPages = computed(() => Math.ceil(mockUsers.length / pageSize) || 1)

const currentPageList = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return mockUsers.slice(start, start + pageSize)
})

function follow(user: RecommendedUser) {
  // 后续对接关注接口
  console.log('follow', user.id)
}
</script>

<style scoped>
/* BBC 风格：白底、红色顶条、蓝色关注链接、清晰层级 */
.recommended-follows {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-top: 3px solid #BB1919;
  border-radius: 0;
  padding: 16px 20px 20px;
  min-width: 280px;
  margin-top: 20px;
}

.rec-follows-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 12px;
}

.rec-follows-icon {
  font-size: 18px;
  color: #666;
}

.rec-follows-title {
  font-size: 16px;
  font-weight: 700;
  color: #111;
  letter-spacing: 0.02em;
}

.rec-follows-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.rec-follows-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.rec-follows-item:last-child {
  border-bottom: none;
}

.rec-follows-avatar-wrap {
  flex-shrink: 0;
  display: block;
}

.rec-follows-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  display: block;
}

.rec-follows-avatar-placeholder {
  background: #e8e8e8;
  color: #666;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.rec-follows-info {
  flex: 1;
  min-width: 0;
}

.rec-follows-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #111;
  text-decoration: none;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-follows-name:hover {
  color: #111;
  text-decoration: underline;
}

.rec-follows-desc {
  font-size: 12px;
  color: #999;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-follows-btn {
  flex-shrink: 0;
  padding: 0 8px;
  font-size: 14px;
  color: #BB1919 !important;
}

.rec-follows-btn .btn-icon {
  font-size: 12px;
  margin-right: 2px;
}

.rec-follows-btn:hover {
  color: #8b1212 !important;
}

.rec-follows-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
}

.rec-follows-page-num,
.rec-follows-page-next {
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s, background 0.2s;
}

.rec-follows-page-num:hover,
.rec-follows-page-next:hover:not(:disabled) {
  color: #111;
  background: #f0f0f0;
}

.rec-follows-page-num.active {
  font-weight: 700;
  color: #111;
  background: #f0f0f0;
}

.rec-follows-page-next:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.rec-follows-page-next .el-icon {
  font-size: 14px;
}
</style>
