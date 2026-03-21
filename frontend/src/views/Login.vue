<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <span>登录 / 注册</span>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <div class="login-mode-toggle" role="tablist" aria-label="登录方式">
            <div class="login-mode-slider" :class="{ 'login-mode-slider--sms': loginMode === 'sms' }" aria-hidden="true" />
            <button
              type="button"
              class="login-mode-btn"
              :class="{ active: loginMode === 'password' }"
              @click="loginMode = 'password'"
            >
              密码登录
            </button>
            <button type="button" class="login-mode-btn" :class="{ active: loginMode === 'sms' }" @click="loginMode = 'sms'">
              验证码登录
            </button>
          </div>
          <el-form :model="form" label-position="top">
            <Transition name="login-field-switch" mode="out-in">
              <div v-if="loginMode === 'password'" key="pwd" class="login-fields-block">
                <el-form-item label="用户名">
                  <el-input
                    v-model="form.username"
                    placeholder="用户名"
                    name="username"
                    autocomplete="username"
                  />
                </el-form-item>
                <el-form-item label="密码">
                  <el-input
                    v-model="form.password"
                    type="password"
                    placeholder="密码"
                    show-password
                    name="password"
                    autocomplete="current-password"
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>
                <el-form-item class="login-options-row">
                  <label class="bbc-remember">
                    <input v-model="form.remember" type="checkbox" class="bbc-remember-input" />
                    <span class="bbc-remember-box" aria-hidden="true" />
                    <span class="bbc-remember-text">记住我</span>
                  </label>
                  <span
                    class="forgot-link"
                    role="button"
                    tabindex="0"
                    @click="forgotVisible = true"
                    @keydown.enter.prevent="forgotVisible = true"
                  >忘记密码？</span>
                </el-form-item>
              </div>
              <div v-else key="sms" class="login-fields-block">
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" placeholder="11位手机号" maxlength="11" />
                </el-form-item>
                <el-form-item label="验证码">
                  <div class="sms-row">
                    <el-input v-model="form.smsCode" placeholder="验证码" maxlength="6" @keyup.enter="handleLogin" />
                    <el-button class="sms-code-btn" :disabled="loginCd > 0 || smsBusy" @click="sendLoginSms">
                      {{ loginCd > 0 ? `${loginCd}s` : '获取验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
              </div>
            </Transition>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="form" label-position="top">
            <el-form-item label="手机号（必填）">
              <el-input v-model="form.phone" maxlength="11" placeholder="手机号" />
            </el-form-item>
            <el-form-item label="验证码（必填）">
              <div class="sms-row">
                <el-input v-model="form.smsCode" maxlength="6" placeholder="验证码" />
                <el-button :disabled="regCd > 0 || smsBusy" @click="sendRegisterSms">
                  {{ regCd > 0 ? `${regCd}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="用户名（必填）">
              <el-input v-model="form.username" placeholder="用户名" />
            </el-form-item>
            <el-form-item label="密码（必填，至少6位）">
              <el-input v-model="form.password" type="password" show-password placeholder="密码" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="确认密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <ForgotPasswordDialog
      v-model="forgotVisible"
      :initial-phone="form.phone"
      @success="onForgotSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMe } from '@/api/user'
import { sendSmsCode } from '@/api/auth'
import ForgotPasswordDialog from '@/components/ForgotPasswordDialog.vue'
import request from '@/api/request'
import { isValidCnMobile, normalizeCnMobile } from '@/utils/phone'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loginMode = ref<'password' | 'sms'>('password')
const forgotVisible = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  smsCode: '',
  remember: false,
})

function onForgotSuccess() {
  form.password = ''
}
const loading = ref(false)
const smsBusy = ref(false)
const loginCd = ref(0)
const regCd = ref(0)
let tLogin: ReturnType<typeof setInterval> | null = null
let tReg: ReturnType<typeof setInterval> | null = null

function startCd(which: 'login' | 'register') {
  const n = 60
  if (which === 'login') {
    loginCd.value = n
    if (tLogin) clearInterval(tLogin)
    tLogin = setInterval(() => {
      loginCd.value--
      if (loginCd.value <= 0 && tLogin) {
        clearInterval(tLogin)
        tLogin = null
      }
    }, 1000)
  } else {
    regCd.value = n
    if (tReg) clearInterval(tReg)
    tReg = setInterval(() => {
      regCd.value--
      if (regCd.value <= 0 && tReg) {
        clearInterval(tReg)
        tReg = null
      }
    }, 1000)
  }
}

async function sendLoginSms() {
  const p = normalizeCnMobile(form.phone)
  if (!isValidCnMobile(p)) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  smsBusy.value = true
  try {
    await sendSmsCode(p, 'LOGIN')
    ElMessage.success('验证码已发送')
    startCd('login')
  } catch {
  } finally {
    smsBusy.value = false
  }
}

async function sendRegisterSms() {
  const p = normalizeCnMobile(form.phone)
  if (!isValidCnMobile(p)) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  smsBusy.value = true
  try {
    await sendSmsCode(p, 'REGISTER')
    ElMessage.success('验证码已发送')
    startCd('register')
  } catch {
  } finally {
    smsBusy.value = false
  }
}

async function handleLogin() {
  if (loginMode.value === 'password') {
    if (!form.username || !form.password) {
      ElMessage.warning('请填写用户名和密码')
      return
    }
  } else {
    const p = normalizeCnMobile(form.phone)
    if (!isValidCnMobile(p) || !form.smsCode?.trim()) {
      ElMessage.warning('请填写手机号和验证码')
      return
    }
  }
  loading.value = true
  try {
    const body =
      loginMode.value === 'password'
        ? { username: form.username, password: form.password }
        : { phone: normalizeCnMobile(form.phone), smsCode: form.smsCode.trim() }
    const data: any = await request.post('sessions', body)
    const token = data?.data?.token ?? data?.token
    const user = data?.user ?? data?.data?.user
    if (token) {
      const remember = loginMode.value === 'password' ? form.remember : true
      userStore.setToken(token, remember)
      try {
        userStore.setUserInfo(await getMe())
      } catch {
        userStore.setUserInfo(user ? { id: user.id, username: user.username, nickname: user.nickname } : { username: form.username })
      }
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      ElMessage.error('登录失败，未返回 Token')
    }
  } catch {
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const phone = normalizeCnMobile(form.phone)
  const code = form.smsCode?.trim() || ''
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请填写正确手机号')
    return
  }
  if (!code) {
    ElMessage.warning('请填写验证码')
    return
  }
  if (!form.username?.trim() || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await request.post('users', {
      username: form.username.trim(),
      password: form.password,
      phone,
      smsCode: code,
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginMode.value = 'password'
    form.smsCode = ''
    form.confirmPassword = ''
  } catch {
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (tLogin) clearInterval(tLogin)
  if (tReg) clearInterval(tReg)
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
}
.login-card {
  width: 440px;
  max-width: 96vw;
}
.login-mode-toggle {
  position: relative;
  display: flex;
  width: 100%;
  margin-bottom: 20px;
  border: 2px solid #1a1a1a;
  border-radius: 4px;
  background: #f4f4f4;
  box-sizing: border-box;
  overflow: hidden;
}
.login-mode-slider {
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 100%;
  background: linear-gradient(180deg, #252525 0%, #111 100%);
  border-bottom: 3px solid #bb1919;
  box-sizing: border-box;
  transition: transform 0.4s cubic-bezier(0.33, 1, 0.68, 1);
  transform: translateX(0);
  z-index: 0;
  pointer-events: none;
  will-change: transform;
}
.login-mode-slider--sms {
  transform: translateX(100%);
}
.login-mode-btn {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 12px 8px;
  margin: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: #555;
  transition: color 0.3s ease;
  font-family: inherit;
}
.login-mode-btn:hover:not(.active) {
  color: #1a1a1a;
}
.login-mode-btn.active {
  color: #fff;
}
.login-fields-block {
  min-height: 1px;
}
.login-field-switch-enter-active,
.login-field-switch-leave-active {
  transition: opacity 0.26s ease, transform 0.26s cubic-bezier(0.33, 1, 0.68, 1);
}
.login-field-switch-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.login-field-switch-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
.sms-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.sms-row :deep(.el-input) {
  flex: 1;
}
.sms-code-btn {
  border: 2px solid #1a1a1a !important;
  background: #fff !important;
  color: #111 !important;
  font-weight: 600;
}
.sms-code-btn:hover:not(:disabled) {
  border-color: #bb1919 !important;
  color: #bb1919 !important;
}
.login-options-row :deep(.el-form-item__content) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.forgot-link {
  font-size: 13px;
  color: #666;
  cursor: pointer;
}
.forgot-link:hover {
  color: #111;
}

.bbc-remember {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  margin: 0;
  font-family: inherit;
}
.bbc-remember-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
  opacity: 0;
}
.bbc-remember-box {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border: 2px solid #1a1a1a;
  background: #fff;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.bbc-remember:focus-within .bbc-remember-box {
  outline: 2px solid #bb1919;
  outline-offset: 2px;
}
.bbc-remember-input:checked + .bbc-remember-box {
  background: #1a1a1a;
  border-color: #1a1a1a;
}
.bbc-remember-input:checked + .bbc-remember-box::after {
  content: '';
  display: block;
  width: 5px;
  height: 10px;
  border: solid #fff;
  border-width: 0 2.5px 2.5px 0;
  transform: rotate(45deg) translate(-0.5px, -1px);
}
.bbc-remember-text {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.02em;
  line-height: 1.2;
}
</style>
