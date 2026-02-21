<template>
  <div class="knowledge-page">
    <!-- 左侧：知识库导航栏（顶栏下方固定，可展开/收起） -->
    <aside class="knowledge-sidebar" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-sidebar-inner">
        <!-- 折叠标：放在导航栏最上面 -->
        <button
          type="button"
          class="knowledge-collapse-btn"
          :title="sidebarExpanded ? '收起' : '展开'"
          @click="sidebarExpanded = !sidebarExpanded"
        >
          <span class="knowledge-collapse-icon" :class="{ expanded: sidebarExpanded }">
            <span class="collapse-panel collapse-panel-left">
              <span class="collapse-line"></span>
              <span class="collapse-line"></span>
            </span>
            <span class="collapse-panel collapse-panel-right"></span>
          </span>
        </button>

        <!-- 1. 文档 -->
        <a href="#" class="knowledge-nav-item" title="文档">
          <el-icon><Document /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">文档</span>
        </a>
        <!-- 2. 搜索 -->
        <router-link to="/knowledge" class="knowledge-nav-item" title="搜索">
          <el-icon><Search /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">搜索</span>
        </router-link>
        <!-- 3. 知识库（当前选中） -->
        <router-link to="/knowledge" class="knowledge-nav-item active" title="知识库">
          <el-icon><FolderOpened /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">知识库</span>
        </router-link>
        <!-- 4. 收藏 -->
        <a href="#" class="knowledge-nav-item" title="收藏">
          <el-icon><Star /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">收藏</span>
        </a>
        <!-- 5. 历史 -->
        <a href="#" class="knowledge-nav-item" title="历史">
          <el-icon><Clock /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">历史</span>
        </a>

        <div class="knowledge-nav-spacer"></div>

        <div class="knowledge-sidebar-footer">
          <router-link to="/creator" class="knowledge-user" title="个人中心">
            <img v-if="userStore.user?.avatar" :src="userStore.user.avatar" alt="" class="knowledge-user-avatar" />
            <span v-else class="knowledge-user-avatar knowledge-user-ph">{{ userInitial }}</span>
          </router-link>
        </div>
      </div>
    </aside>

    <!-- 右侧：主内容区（大块白色区域） -->
    <main class="knowledge-main" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-main-inner">
        <h1 class="knowledge-title">用提问发现世界</h1>
        <div class="knowledge-input-wrap">
          <div class="knowledge-input-box">
            <input
              v-model="question"
              type="text"
              class="knowledge-input"
              placeholder="输入你的问题，或使用「@快捷引用」对答主、知识库进行提问"
              @keyup.enter="onSubmit"
            />
            <div class="knowledge-input-footer">
              <span class="knowledge-input-opt">
                <span>智能思考</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <span class="knowledge-input-opt">
                <span>知识库</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <div class="knowledge-input-actions">
                <span class="knowledge-input-action" title="引用">@</span>
                <span class="knowledge-input-action" title="附件">
                  <el-icon><Paperclip /></el-icon>
                </span>
                <button type="button" class="knowledge-input-send" title="发送" @click="onSubmit">
                  <el-icon><Top /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="knowledge-cols">
          <section class="knowledge-col knowledge-col-left">
            <h2 class="knowledge-col-title">推荐知识库</h2>
            <div class="knowledge-list">
              <router-link
                v-for="item in recommendedKbs"
                :key="item.id"
                :to="`/knowledge?kb=${item.id}`"
                class="knowledge-list-item"
              >
                <div class="knowledge-list-thumb">
                  <img v-if="item.cover" :src="item.cover" :alt="item.name" />
                  <span v-else class="knowledge-list-thumb-ph">{{ item.name.charAt(0) }}</span>
                </div>
                <div class="knowledge-list-body">
                  <span class="knowledge-list-name">{{ item.name }}</span>
                  <span class="knowledge-list-meta">{{ item.owner }} · {{ item.subCount }}人订阅 · {{ item.contentCount }}个内容</span>
                </div>
              </router-link>
            </div>
            <router-link to="/knowledge/square" class="knowledge-col-link">前往知识广场 →</router-link>
          </section>
          <section class="knowledge-col knowledge-col-right">
            <h2 class="knowledge-col-title">想了解点什么?</h2>
            <div class="knowledge-questions">
              <button
                v-for="(q, i) in suggestedQuestions"
                :key="i"
                type="button"
                class="knowledge-question-btn"
                @click="question = q; onSubmit()"
              >
                {{ q }}
              </button>
            </div>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Document, FolderOpened, Star, Clock, ArrowDown, Paperclip, Top } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const sidebarExpanded = ref(false)

const userInitial = computed(() => {
  const n = userStore.user?.nickname || userStore.user?.username || '用'
  return n.charAt(0)
})

const question = ref('')

const recommendedKbs = ref([
  { id: '1', name: '职场"保护伞"', owner: '童先生', subCount: 283, contentCount: 100, cover: '' },
  { id: '2', name: '大语言模型知识库', owner: '数学人生', subCount: 995, contentCount: 116, cover: '' },
])

const suggestedQuestions = ref([
  '自注意力机制如何影响模型的训练速度?',
  '推荐适合学生用的平板电脑?',
  '适合编程初学者看的书有哪些?',
])

function onSubmit() {
  const q = question.value?.trim()
  if (!q) return
  router.push({ path: '/knowledge', query: { q } })
}
</script>

<style scoped>
.knowledge-page {
  display: flex;
  min-height: calc(100vh - 64px);
  background: #fff;
}

/* 左侧导航栏：顶栏下方固定，可展开/收起 */
.knowledge-sidebar {
  position: fixed;
  top: 64px;
  left: 0;
  bottom: 0;
  width: 72px;
  background: #fff;
  border-right: 1px solid #eee;
  z-index: 100;
  transition: width 0.2s ease;
}

.knowledge-sidebar.expanded {
  width: 200px;
}

.knowledge-sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 12px 14px 12px 14px;
  min-width: 72px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-sidebar-inner {
  padding-left: 14px;
  padding-right: 14px;
  align-items: center;
}

.knowledge-sidebar.expanded .knowledge-sidebar-inner {
  padding-left: 14px;
  padding-right: 14px;
}

/* 统一：所有导航项同高、同内边距、同图标尺寸，左对齐 */
.knowledge-nav-item {
  height: 44px;
  min-width: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 12px;
  color: #555;
  text-decoration: none;
  transition: color 0.2s, background 0.2s;
  margin-bottom: 2px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-nav-item {
  justify-content: center;
  padding: 0;
}

.knowledge-nav-item:hover {
  color: #111;
  background: #f0f0f0;
}

.knowledge-nav-item.active {
  color: #111;
  background: #f0f0f0;
  font-weight: 500;
}

.knowledge-nav-item .el-icon {
  font-size: 20px;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
}

.knowledge-nav-text {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
}

.knowledge-nav-spacer {
  flex: 1;
  min-height: 12px;
}

/* 折叠标：与导航项同宽同高，对齐 */
.knowledge-collapse-btn {
  width: 44px;
  height: 44px;
  margin-bottom: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
}

.knowledge-sidebar:not(.expanded) .knowledge-collapse-btn {
  width: 44px;
}

.knowledge-sidebar.expanded .knowledge-collapse-btn {
  width: 100%;
  min-width: 0;
  justify-content: flex-start;
  padding-left: 12px;
}

.knowledge-collapse-btn:hover {
  background: #f5f5f5;
}

/* 展开/折叠 icon：两栏并排，左侧带两条横线（打开的书/分栏） */
.knowledge-collapse-icon {
  position: relative;
  width: 20px;
  height: 14px;
  display: flex;
  border: 1.5px solid #555;
  border-radius: 3px;
  box-sizing: border-box;
  overflow: hidden;
}

.knowledge-collapse-btn:hover .knowledge-collapse-icon {
  border-color: #333;
}

.collapse-panel {
  box-sizing: border-box;
}

.collapse-panel-left {
  width: 7px;
  flex-shrink: 0;
  border-right: 1.5px solid #555;
  position: relative;
  background: #fff;
}

.knowledge-collapse-btn:hover .collapse-panel-left {
  border-color: #333;
}

.collapse-panel-left .collapse-line {
  position: absolute;
  left: 2px;
  width: 2px;
  height: 1px;
  background: #555;
  border-radius: 0.5px;
}

.knowledge-collapse-btn:hover .collapse-line {
  background: #333;
}

.collapse-panel-left .collapse-line:first-child {
  top: 4px;
}

.collapse-panel-left .collapse-line:last-child {
  top: 8px;
}

.collapse-panel-right {
  flex: 1;
  min-width: 0;
  background: #fff;
}

/* 展开时图标左右对调，表示“可收起” */
.knowledge-collapse-icon.expanded {
  flex-direction: row-reverse;
}

.knowledge-collapse-icon.expanded .collapse-panel-left {
  border-right: none;
  border-left: 1.5px solid #555;
}

.knowledge-collapse-btn:hover .knowledge-collapse-icon.expanded .collapse-panel-left {
  border-left-color: #333;
}

.knowledge-sidebar-footer {
  flex-shrink: 0;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.knowledge-user {
  display: block;
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.knowledge-user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.knowledge-user-ph {
  background: #e0e0e0;
  color: #666;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

/* 右侧主内容区 */
.knowledge-main {
  flex: 1;
  margin-left: 72px;
  min-height: calc(100vh - 64px);
  background: #fff;
  padding: 48px 56px 80px;
  transition: margin-left 0.2s ease;
}

.knowledge-main.expanded {
  margin-left: 200px;
}

.knowledge-main-inner {
  max-width: 900px;
  margin: 0 auto;
}

.knowledge-title {
  font-size: 32px;
  font-weight: 700;
  color: #111;
  text-align: center;
  margin: 0 0 40px;
  letter-spacing: 0.02em;
}

.knowledge-input-wrap {
  margin-bottom: 56px;
}

.knowledge-input-box {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px 20px 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.knowledge-input-box:focus-within {
  border-color: #BB1919;
  box-shadow: 0 0 0 2px rgba(187, 25, 25, 0.12);
}

.knowledge-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 16px;
  line-height: 1.5;
  color: #111;
  background: transparent;
  display: block;
  margin-bottom: 12px;
}

.knowledge-input::placeholder {
  color: #999;
}

.knowledge-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-input-opt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #888;
  cursor: pointer;
}

.knowledge-input-opt:hover {
  color: #111;
}

.knowledge-input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.knowledge-input-action {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
}

.knowledge-input-action:hover {
  color: #BB1919;
  background: #fafafa;
}

.knowledge-input-send {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #BB1919;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.knowledge-input-send:hover {
  background: #9e1515;
}

/* 两列 */
.knowledge-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
}

.knowledge-col-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0 0 20px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.knowledge-list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  border: 1px solid transparent;
  transition: border-color 0.2s, background 0.2s;
}

.knowledge-list-item:hover {
  background: #fafafa;
  border-color: #eee;
}

.knowledge-list-thumb {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #eee;
}

.knowledge-list-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-list-thumb-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #999;
}

.knowledge-list-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.knowledge-list-name {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.knowledge-list-meta {
  font-size: 13px;
  color: #888;
}

.knowledge-col-link {
  font-size: 14px;
  color: #BB1919;
  text-decoration: none;
}

.knowledge-col-link:hover {
  text-decoration: underline;
}

.knowledge-questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.knowledge-question-btn {
  width: 100%;
  padding: 14px 18px;
  text-align: left;
  font-size: 15px;
  color: #333;
  background: #f7f7f7;
  border: 1px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.knowledge-question-btn:hover {
  background: #f0f0f0;
  border-color: #e0e0e0;
  color: #111;
}

@media (max-width: 900px) {
  .knowledge-cols {
    grid-template-columns: 1fr;
  }
}
</style>
