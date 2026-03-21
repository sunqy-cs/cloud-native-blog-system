<template>
  <div class="inbox-page">
    <section class="inbox-card">
      <header class="inbox-head">
        <div>
          <h1 class="inbox-title">收件箱</h1>
          <p class="inbox-subtitle">系统通知、审核结果和互动消息</p>
        </div>
        <el-button class="inbox-read-all" text @click="onReadAll">全部已读</el-button>
      </header>
      <div class="inbox-filter-row">
        <el-radio-group v-model="unreadOnly" size="small" class="inbox-filter-group" @change="onFilterChange">
          <el-radio-button :label="true">仅未读</el-radio-button>
          <el-radio-button :label="false">全部</el-radio-button>
        </el-radio-group>
      </div>
      <div v-if="loading" class="inbox-empty">
        <p class="inbox-empty-text">加载中...</p>
      </div>
      <div v-else-if="records.length === 0" class="inbox-empty">
        <el-icon class="inbox-empty-icon"><Bell /></el-icon>
        <p class="inbox-empty-text">暂无消息</p>
      </div>
      <div v-else class="inbox-list">
        <article
          v-for="item in records"
          :key="item.id"
          :class="['inbox-item', { unread: !item.read }]"
          @click="onRead(item)"
        >
          <div class="inbox-item-head">
            <h3 class="inbox-item-title">{{ item.title }}</h3>
            <time class="inbox-item-time">{{ formatTime(item.createdAt) }}</time>
          </div>
          <p class="inbox-item-body">{{ item.body || '无正文' }}</p>
          <div class="inbox-item-meta">
            <span v-if="messageStatusLabel(item)" :class="['inbox-status-pill', messageStatusClass(item)]">
              {{ messageStatusLabel(item) }}
            </span>
            <span v-if="!item.read" class="inbox-unread-dot"></span>
          </div>
        </article>
      </div>
      <div v-if="total > pageSize" class="inbox-pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="reload"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listUserMessages, markAllMessagesRead, markUserMessageRead, type UserMessageItem } from '@/api/userMessage'

const records = ref<UserMessageItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const unreadOnly = ref(true)
const loading = ref(false)

function onFilterChange() {
  page.value = 1
  reload()
}

async function reload() {
  loading.value = true
  try {
    const data = await listUserMessages({
      page: page.value,
      pageSize,
      unreadOnly: unreadOnly.value,
    })
    records.value = Array.isArray(data?.records) ? data.records : []
    total.value = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function onRead(item: UserMessageItem) {
  if (item.read) return
  await markUserMessageRead(item.id)
  item.read = true
  item.readAt = new Date().toISOString()
  if (unreadOnly.value) {
    await reload()
  }
}

async function onReadAll() {
  await markAllMessagesRead()
  records.value.forEach((i) => {
    i.read = true
  })
  ElMessage.success('已全部标记为已读')
}

function formatTime(s: string) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 16)
}

/** 审核类消息：未通过显示红标，通过/待人工显示对应标签；非审核不显示 */
function messageStatusLabel(item: UserMessageItem): string {
  if (String(item.msgType || '').toUpperCase() !== 'AUDIT') return ''
  const t = item.title || ''
  if (t.includes('未通过')) return '未通过'
  if (t.includes('待人工') || t.includes('待审核')) return '待人工'
  if (t.includes('通过') && !t.includes('未通过')) return '已通过'
  return ''
}

function messageStatusClass(item: UserMessageItem): string {
  const label = messageStatusLabel(item)
  if (label === '未通过') return 'inbox-status-danger'
  if (label === '已通过') return 'inbox-status-ok'
  if (label === '待人工') return 'inbox-status-warn'
  return ''
}

onMounted(() => {
  reload()
})
</script>

<style scoped>
.inbox-page {
  max-width: 980px;
  margin: 24px auto;
  padding: 0 20px 24px;
  --el-color-primary: #bb1919;
  --el-color-primary-light-3: #c74848;
  --el-color-primary-light-5: #d77171;
  --el-color-primary-light-7: #e69f9f;
  --el-color-primary-light-8: #efbaba;
  --el-color-primary-light-9: #f8dede;
  --el-color-primary-dark-2: #8f1515;
}

.inbox-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.inbox-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 14px;
  margin-bottom: 18px;
}

.inbox-title {
  margin: 0 0 6px;
  font-size: 24px;
  color: #111;
}

.inbox-subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.inbox-read-all {
  color: #bb1919;
  font-weight: 600;
}

.inbox-filter-row {
  margin-bottom: 14px;
}

.inbox-filter-group :deep(.el-radio-button__inner) {
  border-color: #e3e3e3;
  color: #555;
}

.inbox-filter-group :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: #bb1919;
  border-color: #bb1919;
  color: #fff;
  box-shadow: -1px 0 0 0 #bb1919;
}

.inbox-empty {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.inbox-empty-icon {
  font-size: 72px;
  color: #d0d0d0;
  margin-bottom: 12px;
}

.inbox-empty-text {
  margin: 0;
  color: #999;
  font-size: 15px;
}

.inbox-list {
  display: flex;
  flex-direction: column;
}

.inbox-item {
  padding: 14px 0;
  border-bottom: 1px solid #f2f2f2;
  cursor: pointer;
}

.inbox-item:last-child {
  border-bottom: none;
}

.inbox-item.unread .inbox-item-title {
  color: #111;
  font-weight: 700;
}

.inbox-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.inbox-item-title {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.inbox-item-time {
  color: #999;
  font-size: 12px;
  white-space: nowrap;
}

.inbox-item-body {
  margin: 8px 0 10px;
  color: #555;
  font-size: 14px;
  line-height: 1.6;
}

.inbox-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inbox-status-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
}

.inbox-status-pill.inbox-status-danger {
  color: #bb1919;
  background: #fdecec;
}

.inbox-status-pill.inbox-status-ok {
  color: #1f8f4d;
  background: #eaf7ef;
}

.inbox-status-pill.inbox-status-warn {
  color: #b76a00;
  background: #fff4e5;
}

.inbox-unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #bb1919;
}

.inbox-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.inbox-pagination :deep(.el-pager li.is-active) {
  color: #bb1919;
}

.inbox-pagination :deep(.el-pagination button:hover),
.inbox-pagination :deep(.el-pager li:hover) {
  color: #bb1919;
}
</style>
