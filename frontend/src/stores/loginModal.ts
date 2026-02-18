import { ref } from 'vue'

/** 待显示的登录弹窗：redirect 为登录成功后要跳转的路径 */
export const pendingLogin = ref<string | null>(null)

export function requestLogin(redirect: string) {
  pendingLogin.value = redirect
}
