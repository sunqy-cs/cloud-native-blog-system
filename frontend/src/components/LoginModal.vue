<template>
  <Teleport to="body">
    <Transition name="login-overlay">
      <div v-show="visible" class="login-modal-overlay" @click.self="close">
        <Transition name="login-dialog">
          <div v-show="visible" class="login-modal-dialog" @click.stop>
            <button class="login-modal-close" @click="close" aria-label="关闭">
              <el-icon><Close /></el-icon>
            </button>
            <div class="login-modal-content">
              <div class="login-modal-header">
                <el-icon class="login-modal-logo"><UserFilled /></el-icon>
                <h3 class="login-modal-title">登录 / 注册</h3>
              </div>
              <div class="login-tabs">
                <div class="login-tab-headers">
                  <div
                    :class="['login-tab-item', { active: activeTab === 'login' }]"
                    @click="activeTab = 'login'"
                  >
                    登录
                  </div>
                  <div
                    :class="['login-tab-item', { active: activeTab === 'register' }]"
                    @click="activeTab = 'register'"
                  >
                    注册
                  </div>
                </div>
                <div class="login-tab-content">
                  <Transition name="login-tab" mode="out-in">
                    <div v-if="activeTab === 'login'" key="login" class="login-tab-pane">
                      <div class="login-mode-toggle" role="tablist" aria-label="登录方式">
                        <div
                          class="login-mode-slider"
                          :class="{ 'login-mode-slider--sms': loginMode === 'sms' }"
                          aria-hidden="true"
                        />
                        <button
                          type="button"
                          role="tab"
                          class="login-mode-btn"
                          :class="{ active: loginMode === 'password' }"
                          :aria-selected="loginMode === 'password'"
                          @click="loginMode = 'password'"
                        >
                          密码登录
                        </button>
                        <button
                          type="button"
                          role="tab"
                          class="login-mode-btn"
                          :class="{ active: loginMode === 'sms' }"
                          :aria-selected="loginMode === 'sms'"
                          @click="loginMode = 'sms'"
                        >
                          验证码登录
                        </button>
                      </div>
                      <el-form :model="form" label-position="top" class="login-form">
                        <Transition name="login-field-switch" mode="out-in">
                          <div v-if="loginMode === 'password'" key="pwd-fields" class="login-fields-block">
                          <el-form-item label="用户名">
                            <el-input
                              v-model="form.username"
                              placeholder="请输入用户名"
                              size="large"
                              clearable
                              name="username"
                              autocomplete="username"
                            >
                              <template #prefix>
                                <el-icon><User /></el-icon>
                              </template>
                            </el-input>
                          </el-form-item>
                          <el-form-item label="密码">
                            <el-input
                              v-model="form.password"
                              :type="showPassword ? 'text' : 'password'"
                              placeholder="请输入密码"
                              size="large"
                              clearable
                              name="password"
                              autocomplete="current-password"
                              @keyup.enter="handleLogin"
                            >
                              <template #prefix>
                                <el-icon><Lock /></el-icon>
                              </template>
                              <template #suffix>
                                <el-icon class="password-toggle" @click="showPassword = !showPassword">
                                  <View v-if="!showPassword" />
                                  <Hide v-else />
                                </el-icon>
                              </template>
                            </el-input>
                          </el-form-item>
                          </div>
                          <div v-else key="sms-fields" class="login-fields-block">
                          <el-form-item label="手机号">
                            <el-input
                              v-model="form.phone"
                              placeholder="请输入11位手机号"
                              maxlength="11"
                              size="large"
                              clearable
                            >
                              <template #prefix>
                                <el-icon><Cellphone /></el-icon>
                              </template>
                            </el-input>
                          </el-form-item>
                          <el-form-item label="验证码">
                            <div class="sms-row">
                              <el-input
                                v-model="form.smsCode"
                                placeholder="请输入6位验证码"
                                maxlength="6"
                                size="large"
                                clearable
                                @keyup.enter="handleLogin"
                              />
                              <el-button
                                class="sms-btn"
                                :disabled="loginSmsCooldown > 0 || smsSending"
                                :loading="smsSending && smsSendingScene === 'LOGIN'"
                                @click="handleSendLoginSms"
                              >
                                {{ loginSmsCooldown > 0 ? `${loginSmsCooldown}s` : '获取验证码' }}
                              </el-button>
                            </div>
                          </el-form-item>
                          </div>
                        </Transition>
                        <el-form-item v-if="loginMode === 'password'" class="login-options">
                          <label class="bbc-remember">
                            <input v-model="form.remember" type="checkbox" class="bbc-remember-input" />
                            <span class="bbc-remember-box" aria-hidden="true" />
                            <span class="bbc-remember-text">记住我</span>
                          </label>
                          <span
                            class="forgot-link"
                            role="button"
                            tabindex="0"
                            @click="openForgotDialog"
                            @keydown.enter.prevent="openForgotDialog"
                          >忘记密码？</span>
                        </el-form-item>
                        <el-form-item>
                          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                            登录
                          </el-button>
                        </el-form-item>
                      </el-form>
                    </div>
                    <div v-else key="register" class="login-tab-pane">
                      <el-form :model="form" label-position="top" class="login-form">
                        <el-form-item label="手机号" required>
                          <el-input
                            v-model="form.phone"
                            placeholder="请输入手机号（必填）"
                            maxlength="11"
                            size="large"
                            clearable
                          >
                            <template #prefix>
                              <el-icon><Cellphone /></el-icon>
                            </template>
                          </el-input>
                        </el-form-item>
                        <el-form-item label="验证码" required>
                          <div class="sms-row">
                            <el-input
                              v-model="form.smsCode"
                              placeholder="请先获取验证码（必填）"
                              maxlength="6"
                              size="large"
                              clearable
                              @keyup.enter="handleRegister"
                            />
                            <el-button
                              class="sms-btn"
                              :disabled="registerSmsCooldown > 0 || smsSending"
                              :loading="smsSending && smsSendingScene === 'REGISTER'"
                              @click="handleSendRegisterSms"
                            >
                              {{ registerSmsCooldown > 0 ? `${registerSmsCooldown}s` : '获取验证码' }}
                            </el-button>
                          </div>
                        </el-form-item>
                        <el-form-item label="用户名" required>
                          <el-input
                            v-model="form.username"
                            placeholder="设置用户名（必填）"
                            size="large"
                            clearable
                          >
                            <template #prefix>
                              <el-icon><User /></el-icon>
                            </template>
                          </el-input>
                        </el-form-item>
                        <el-form-item label="密码" required>
                          <el-input
                            v-model="form.password"
                            :type="showPassword ? 'text' : 'password'"
                            placeholder="至少 6 位（必填）"
                            size="large"
                            clearable
                          >
                            <template #prefix>
                              <el-icon><Lock /></el-icon>
                            </template>
                            <template #suffix>
                              <el-icon class="password-toggle" @click="showPassword = !showPassword">
                                <View v-if="!showPassword" />
                                <Hide v-else />
                              </el-icon>
                            </template>
                          </el-input>
                        </el-form-item>
                        <el-form-item label="确认密码" required>
                          <el-input
                            v-model="form.confirmPassword"
                            :type="showConfirmPassword ? 'text' : 'password'"
                            placeholder="请再次输入密码"
                            size="large"
                            clearable
                            @keyup.enter="handleRegister"
                          >
                            <template #prefix>
                              <el-icon><Lock /></el-icon>
                            </template>
                            <template #suffix>
                              <el-icon class="password-toggle" @click="showConfirmPassword = !showConfirmPassword">
                                <View v-if="!showConfirmPassword" />
                                <Hide v-else />
                              </el-icon>
                            </template>
                          </el-input>
                        </el-form-item>
                        <el-form-item>
                          <el-button type="primary" :loading="loading" class="login-btn" @click="handleRegister">
                            注册
                          </el-button>
                        </el-form-item>
                      </el-form>
                    </div>
                  </Transition>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>

    <ForgotPasswordDialog
      v-model="forgotVisible"
      :initial-phone="form.phone"
      @success="onForgotSuccess"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, User, Lock, View, Hide, UserFilled, Cellphone } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMe } from '@/api/user'
import { sendSmsCode } from '@/api/auth'
import ForgotPasswordDialog from '@/components/ForgotPasswordDialog.vue'
import request from '@/api/request'
import { isValidCnMobile, normalizeCnMobile } from '@/utils/phone'

const props = defineProps<{ visible: boolean; redirect?: string }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref<'login' | 'register'>('login')
const loginMode = ref<'password' | 'sms'>('password')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  smsCode: '',
  remember: false,
})
const loading = ref(false)
const smsSending = ref(false)
const smsSendingScene = ref<'LOGIN' | 'REGISTER' | null>(null)
const loginSmsCooldown = ref(0)
const registerSmsCooldown = ref(0)
let loginTimer: ReturnType<typeof setInterval> | null = null
let registerTimer: ReturnType<typeof setInterval> | null = null

const forgotVisible = ref(false)

function openForgotDialog() {
  forgotVisible.value = true
}

function onForgotSuccess() {
  activeTab.value = 'login'
  loginMode.value = 'password'
  form.password = ''
}

function close() {
  emit('update:visible', false)
}

watch(() => props.visible, (v) => {
  if (v) document.body.style.overflow = 'hidden'
  else document.body.style.overflow = ''
})

function startCooldown(which: 'login' | 'register') {
  const sec = 60
  if (which === 'login') {
    loginSmsCooldown.value = sec
    if (loginTimer) clearInterval(loginTimer)
    loginTimer = setInterval(() => {
      loginSmsCooldown.value--
      if (loginSmsCooldown.value <= 0 && loginTimer) {
        clearInterval(loginTimer)
        loginTimer = null
      }
    }, 1000)
  } else {
    registerSmsCooldown.value = sec
    if (registerTimer) clearInterval(registerTimer)
    registerTimer = setInterval(() => {
      registerSmsCooldown.value--
      if (registerSmsCooldown.value <= 0 && registerTimer) {
        clearInterval(registerTimer)
        registerTimer = null
      }
    }, 1000)
  }
}

async function handleSendLoginSms() {
  const phone = normalizeCnMobile(form.phone)
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  smsSending.value = true
  smsSendingScene.value = 'LOGIN'
  try {
    await sendSmsCode(phone, 'LOGIN')
    ElMessage.success('验证码已发送')
    startCooldown('login')
  } catch {
    // 拦截器已提示
  } finally {
    smsSending.value = false
    smsSendingScene.value = null
  }
}

async function handleSendRegisterSms() {
  const phone = normalizeCnMobile(form.phone)
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  smsSending.value = true
  smsSendingScene.value = 'REGISTER'
  try {
    await sendSmsCode(phone, 'REGISTER')
    ElMessage.success('验证码已发送')
    startCooldown('register')
  } catch {
  } finally {
    smsSending.value = false
    smsSendingScene.value = null
  }
}

async function handleLogin() {
  if (loginMode.value === 'password') {
    if (!form.username || !form.password) {
      ElMessage.warning('请填写用户名和密码')
      return
    }
    loading.value = true
    try {
      const data: any = await request.post('sessions', { username: form.username, password: form.password })
      await finishLoginSuccess(data, form.remember)
    } catch {
      // 拦截器
    } finally {
      loading.value = false
    }
    return
  }

  const phone = normalizeCnMobile(form.phone)
  const code = (form.smsCode || '').trim()
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!code || code.length < 4) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const data: any = await request.post('sessions', { phone, smsCode: code })
    // 验证码登录无「记住我」选项，默认长期登录（与常见产品一致）
    await finishLoginSuccess(data, true)
  } catch {
  } finally {
    loading.value = false
  }
}

async function finishLoginSuccess(data: any, remember: boolean) {
  const token = data?.data?.token ?? data?.token
  const user = data?.user ?? data?.data?.user
  if (token) {
    userStore.setToken(token, remember)
    try {
      const fu = await getMe()
      userStore.setUserInfo(fu)
    } catch {
      userStore.setUserInfo(
        user ? { id: user.id, username: user.username, nickname: user.nickname } : { username: form.username }
      )
    }
    ElMessage.success('登录成功')
    close()
    router.push(props.redirect || '/recommend')
  } else {
    ElMessage.error('登录失败，未返回 Token')
  }
}

async function handleRegister() {
  const phone = normalizeCnMobile(form.phone)
  const code = (form.smsCode || '').trim()
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!code || code.length < 4) {
    ElMessage.warning('请填写短信验证码')
    return
  }
  if (!form.username?.trim()) {
    ElMessage.warning('请设置用户名')
    return
  }
  if (!form.password) {
    ElMessage.warning('请设置密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
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
    form.confirmPassword = ''
    form.smsCode = ''
  } catch {
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (loginTimer) clearInterval(loginTimer)
  if (registerTimer) clearInterval(registerTimer)
})
</script>

<style scoped>
.login-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-overlay-enter-active,
.login-overlay-leave-active {
  transition: opacity 0.5s ease;
}

.login-overlay-enter-from,
.login-overlay-leave-to {
  opacity: 0;
}

.login-dialog-enter-active,
.login-dialog-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.login-dialog-enter-from,
.login-dialog-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.login-modal-dialog {
  position: relative;
  background: #fff;
  border-radius: 8px;
  padding: 40px 48px 48px;
  min-width: 440px;
  max-width: 90vw;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}

.login-modal-close {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 6px;
  border: none;
  background: none;
  cursor: pointer;
  color: #666;
  font-size: 22px;
}

.login-modal-close:hover {
  color: #000;
}

.login-modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.login-modal-logo {
  font-size: 36px;
  color: #111;
}

.login-modal-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.login-tab-headers {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.login-tab-item {
  padding-bottom: 12px;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s ease, font-weight 0.2s ease;
  position: relative;
}

.login-tab-item:hover {
  color: #333;
}

.login-tab-item.active {
  color: #111;
  font-weight: 600;
}

.login-tab-item.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #111;
  transition: left 0.25s ease, right 0.25s ease;
}

.login-tab-content {
  overflow: hidden;
}

.login-tab-pane {
  min-height: 200px;
}

/* BBC 风格：登录方式双段切换 + 滑动底块 + 底部绯红条 */
.login-mode-toggle {
  position: relative;
  display: flex;
  width: 100%;
  margin-bottom: 22px;
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
  padding: 13px 10px;
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
  -webkit-tap-highlight-color: transparent;
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

.login-tab-enter-active,
.login-tab-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.login-tab-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.login-tab-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.login-options {
  margin-bottom: 8px !important;
}

.login-options :deep(.el-form-item__content) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* BBC 风格：大号方框、黑框、选中时黑底白勾，与顶栏登录方式切换一致 */
.bbc-remember {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  margin: 0;
  font-family: inherit;
}
/* 保留可聚焦以便键盘与读屏，视觉上由自定义方框呈现 */
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

.forgot-link {
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.forgot-link:hover {
  color: #111;
}

.password-toggle {
  cursor: pointer;
  color: #999;
}

.password-toggle:hover {
  color: #333;
}

.sms-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: stretch;
}

.sms-row :deep(.el-input) {
  flex: 1;
}

.sms-btn {
  flex-shrink: 0;
  white-space: nowrap;
  border: 2px solid #1a1a1a !important;
  background: #fff !important;
  color: #111 !important;
  font-weight: 600;
}

.sms-btn:hover:not(:disabled) {
  border-color: #bb1919 !important;
  color: #bb1919 !important;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  font-size: 15px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-btn {
  min-width: 120px;
  padding: 10px 24px;
  background-color: #111 !important;
  border-color: #111 !important;
}

.login-btn:hover {
  background-color: #333 !important;
  border-color: #333 !important;
}

</style>
