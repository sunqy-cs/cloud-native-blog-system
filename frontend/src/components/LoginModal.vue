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
                  <el-form :model="form" label-position="top" class="login-form">
                    <el-form-item label="用户名">
                      <el-input
                        v-model="form.username"
                        placeholder="请输入用户名"
                        size="large"
                        clearable
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
                    <el-form-item class="login-options">
                      <el-checkbox v-model="form.remember">记住我</el-checkbox>
                      <span class="forgot-link">忘记密码？</span>
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
                        登录
                      </el-button>
                    </el-form-item>
                  </el-form>
                    </div>
                    <div v-else key="register" class="login-tab-pane">
                  <el-form :model="form" label-position="top" class="login-form">
                    <el-form-item label="用户名">
                      <el-input
                        v-model="form.username"
                        placeholder="请输入用户名"
                        size="large"
                        clearable
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
                        placeholder="请输入密码（至少 6 位）"
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
                    <el-form-item label="确认密码">
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
                      <el-button type="primary" :loading="loading" @click="handleRegister" class="login-btn">
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
  </Teleport>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, User, Lock, View, Hide, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const props = defineProps<{ visible: boolean; redirect?: string }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref<'login' | 'register'>('login')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  remember: false,
})
const loading = ref(false)

function close() {
  emit('update:visible', false)
}

watch(() => props.visible, (v) => {
  if (v) document.body.style.overflow = 'hidden'
  else document.body.style.overflow = ''
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const data: any = await request.post('/sessions', { username: form.username, password: form.password })
    const token = data?.data?.token ?? data?.token
    const user = data?.user ?? data?.data?.user
    if (token) {
      userStore.setToken(token)
      userStore.setUserInfo(user ? { id: user.id, username: user.username, nickname: user.nickname } : { username: form.username })
      ElMessage.success('登录成功')
      close()
      router.push(props.redirect || '/recommend')
    } else {
      ElMessage.error('登录失败，未返回 Token')
    }
  } catch {
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
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
    await request.post('/users', { username: form.username, password: form.password })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    form.confirmPassword = ''
  } catch {
  } finally {
    loading.value = false
  }
}
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
