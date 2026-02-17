<template>
  <div class="article-detail">
    <el-button class="back" text @click="router.push('/')">← 返回首页</el-button>
    <el-card v-if="article" shadow="never">
      <h1>{{ article.title }}</h1>
      <div class="meta">
        <span>{{ article.createdAt }}</span>
      </div>
      <div class="content">{{ article.content || '暂无正文' }}</div>
    </el-card>
    <el-empty v-else description="文章不存在或加载失败" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const article = ref<{ title?: string; content?: string; createdAt?: string } | null>(null)

const id = computed(() => route.params.id as string)

onMounted(() => {
  // 后续对接文章详情接口
  article.value = { title: '示例文章', content: '内容待对接 blog-service 接口。', createdAt: new Date().toISOString().slice(0, 10) }
})
</script>

<style scoped>
.article-detail { padding: 1rem 2rem; max-width: 800px; margin: 0 auto; }
.back { margin-bottom: 1rem; }
.meta { color: var(--el-text-color-secondary); margin: 0.5rem 0 1rem; }
.content { line-height: 1.8; white-space: pre-wrap; }
</style>
