import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/follow',
    name: 'follow',
    component: () => import('@/views/Follow.vue'),
    meta: { title: '关注' },
  },
  {
    path: '/recommend',
    name: 'recommend',
    component: () => import('@/views/Recommend.vue'),
    meta: { title: '推荐' },
  },
  {
    path: '/hot',
    name: 'hot',
    component: () => import('@/views/Hot.vue'),
    meta: { title: '热榜' },
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库' },
  },
  {
    path: '/article/:id',
    name: 'article',
    component: () => import('@/views/ArticleDetail.vue'),
    meta: { title: '文章详情' },
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { title: '后台', requireAuth: true },
    children: [
      {
        path: '',
        redirect: '/admin/articles',
      },
      {
        path: 'articles',
        name: 'admin-articles',
        component: () => import('@/views/admin/ArticleManage.vue'),
        meta: { title: '文章管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) ? `${to.meta.title} - 云原生博客` : '云原生博客'
  if (to.meta.requireAuth) {
    const userStore = useUserStore()
    if (!userStore.token) {
      next({ name: 'login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
