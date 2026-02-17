import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'blog_token'
const USER_KEY = 'blog_user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  let initialUser: { id?: number; username?: string; nickname?: string; role?: string } | null = null
try {
  const raw = localStorage.getItem(USER_KEY)
  initialUser = raw ? JSON.parse(raw) : null
} catch {
  initialUser = null
}
const userInfo = ref<typeof initialUser>(initialUser)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
    if (t) localStorage.setItem(TOKEN_KEY, t)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function setUserInfo(info: typeof userInfo.value) {
    userInfo.value = info
    if (info) localStorage.setItem(USER_KEY, JSON.stringify(info))
    else localStorage.removeItem(USER_KEY)
  }

  function logout() {
    setToken('')
    setUserInfo(null)
  }

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, logout }
})
