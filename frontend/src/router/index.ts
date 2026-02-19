import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { requestLogin } from '@/stores/loginModal'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/recommend',
  },
  {
    path: '/blog',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '博客', requireLogin: true },
  },
  {
    path: '/login',
    redirect: () => ({ path: '/recommend', query: { login: '1' } }),
  },
  {
    path: '/follow',
    name: 'follow',
    component: () => import('@/views/Follow.vue'),
    meta: { title: '关注', requireLogin: true },
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
    meta: { title: '知识库', requireLogin: true },
  },
  {
    path: '/creator',
    name: 'creator',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '创作者中心', requireLogin: true },
  },
  {
    path: '/creator/write',
    name: 'creator-write',
    component: () => import('@/views/CreatorWrite.vue'),
    meta: { title: '发布文章', requireLogin: true },
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
  const userStore = useUserStore()
  if (!userStore.token) {
    if (to.meta.requireAuth) {
      /* 后台等：重定向到推荐，不弹窗 */
      next({ path: '/recommend', query: { redirect: to.fullPath } })
      return
    }
    if (to.meta.requireLogin) {
      /* 博客、关注、知识库：不跳转，弹登录框 */
      requestLogin(to.fullPath)
      next(false)
      return
    }
  }
  next()
})

export default router
