<template>
  <div class="article-manage">
    <el-card>
      <template #header>
        <span>文章管理</span>
        <el-button type="primary" style="float: right">新建文章</el-button>
      </template>
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default>
            <el-button text type="primary">编辑</el-button>
            <el-button text type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="10"
        layout="total, prev, pager, next"
        :total="0"
        style="margin-top: 1rem"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const list = ref<any[]>([])
const page = ref(1)

onMounted(() => {
  // 后续对接 blog-service 文章列表接口
  list.value = []
})
</script>

<style scoped>
.article-manage { padding: 0; }
</style>
