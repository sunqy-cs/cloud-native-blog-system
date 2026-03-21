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
    meta: { title: '热榜', requireLogin: true },
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('@/views/Search.vue'),
    meta: { title: '搜索' },
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库', requireLogin: true },
  },
  {
    path: '/audit',
    name: 'audit',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '审核中心', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/articles',
    name: 'audit-articles',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '待审文章', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/comments',
    name: 'audit-comments',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '评论审核', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/logs',
    name: 'audit-logs',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '审核记录', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/knowledge',
    name: 'audit-knowledge',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '公开知识库审核', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/columns',
    name: 'audit-columns',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '专栏审核', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/knowledge-base',
    name: 'audit-knowledge-base',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '知识库审核', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/audit/profile',
    name: 'audit-profile',
    component: () => import('@/views/AuditCenter.vue'),
    meta: { title: '资料审核', requireLogin: true, requireAdmin: true },
  },
  {
    path: '/creator',
    name: 'creator',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '创作者中心', requireLogin: true },
  },
  {
    path: '/creator/content',
    name: 'creator-content',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '内容管理', requireLogin: true },
  },
  {
    path: '/creator/comments',
    name: 'creator-comments',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '评论管理', requireLogin: true },
  },
  {
    path: '/creator/columns',
    name: 'creator-columns',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '专栏管理', requireLogin: true },
  },
  {
    path: '/creator/ai/blog',
    name: 'creator-ai-blog',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '博客机器人', requireLogin: true },
  },
  {
    path: '/creator/analytics',
    name: 'creator-analytics',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { title: '数据分析', requireLogin: true },
  },
  { path: '/creator/analytics/overview', redirect: '/creator/analytics' },
  { path: '/creator/analytics/content', redirect: '/creator/analytics' },
  { path: '/creator/analytics/audience', redirect: '/creator/analytics' },
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
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人主页', requireLogin: true },
  },
  {
    path: '/inbox',
    name: 'inbox',
    component: () => import('@/views/Inbox.vue'),
    meta: { title: '收件箱', requireLogin: true },
  },
  {
    path: '/column/:id',
    name: 'column-detail',
    component: () => import('@/views/ColumnDetail.vue'),
    meta: { title: '专栏' },
  },
  {
    path: '/collection/:id',
    name: 'collection-detail',
    component: () => import('@/views/CollectionFolderDetail.vue'),
    meta: { title: '收藏夹', requireLogin: true },
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { title: '后台', requireAuth: true },
    children: [
      {
        path: '',
        name: 'admin-default',
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
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    // 推荐页内仅 query 变化（如点「全部」/分类）时不滚到顶部，由 Recommend.vue 滚到对应区块
    if (to.path === '/recommend' && from.path === '/recommend') return
    return { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) ? `${to.meta.title} - 云原生博客` : '云原生博客'
  const userStore = useUserStore()
  if (to.meta.requireAdmin) {
    if (!userStore.token) {
      requestLogin(to.fullPath)
      next(false)
      return
    }
    const role = (userStore.userInfo?.role || '').toUpperCase()
    if (role !== 'ADMIN') {
      next({ path: '/recommend' })
      return
    }
  }
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
