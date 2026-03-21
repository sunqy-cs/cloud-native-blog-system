import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'blog_token'
const USER_KEY = 'blog_user'

type UserInfo = { id?: number; username?: string; nickname?: string; role?: string; avatar?: string } | null

function readStoredAuth(): { token: string; user: UserInfo; storage: 'session' | 'local' } {
  const st = sessionStorage.getItem(TOKEN_KEY)
  if (st) {
    let user: UserInfo = null
    try {
      const raw = sessionStorage.getItem(USER_KEY)
      user = raw ? JSON.parse(raw) : null
    } catch {
      user = null
    }
    return { token: st, user, storage: 'session' }
  }
  const lt = localStorage.getItem(TOKEN_KEY)
  if (lt) {
    let user: UserInfo = null
    try {
      const raw = localStorage.getItem(USER_KEY)
      user = raw ? JSON.parse(raw) : null
    } catch {
      user = null
    }
    return { token: lt, user, storage: 'local' }
  }
  return { token: '', user: null, storage: 'local' }
}

export const useUserStore = defineStore('user', () => {
  const initial = readStoredAuth()
  const token = ref<string>(initial.token)
  const userInfo = ref<UserInfo>(initial.user)
  /** 当前 token 与用户信息写入 sessionStorage 还是 localStorage */
  const tokenStorage = ref<'session' | 'local'>(initial.storage)

  const isLoggedIn = computed(() => !!token.value)

  /** 与库表 role 字段一致：ADMIN / USER */
  const isAdmin = computed(() => (userInfo.value?.role || '').toUpperCase() === 'ADMIN')

  function clearAllStorage() {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /**
   * @param remember 密码登录「记住我」：true 写入 localStorage（关闭浏览器仍保持）；false 仅 sessionStorage（关标签/浏览器后需重登）。未传时默认 true，兼容旧调用。
   */
  function setToken(t: string, remember = true) {
    token.value = t
    if (!t) {
      clearAllStorage()
      tokenStorage.value = 'local'
      return
    }
    const useLocal = remember !== false
    tokenStorage.value = useLocal ? 'local' : 'session'
    if (useLocal) {
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(USER_KEY)
      localStorage.setItem(TOKEN_KEY, t)
    } else {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      sessionStorage.setItem(TOKEN_KEY, t)
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    if (!info) {
      if (tokenStorage.value === 'session') {
        sessionStorage.removeItem(USER_KEY)
      } else {
        localStorage.removeItem(USER_KEY)
      }
      return
    }
    const raw = JSON.stringify(info)
    if (tokenStorage.value === 'session') {
      sessionStorage.setItem(USER_KEY, raw)
    } else {
      localStorage.setItem(USER_KEY, raw)
    }
  }

  function logout() {
    setToken('')
    setUserInfo(null)
  }

  return { token, userInfo, tokenStorage, isLoggedIn, isAdmin, setToken, setUserInfo, logout }
})
