<template>
  <el-dialog
    :model-value="modelValue"
    title="通过手机号找回密码"
    width="440px"
    append-to-body
    destroy-on-close
    class="forgot-password-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    @opened="onDialogOpened"
    @closed="onClosed"
  >
    <p class="forgot-hint">将向您的注册手机号发送验证码，验证后即可设置新密码。</p>
    <el-form label-position="top" class="forgot-form" autocomplete="off">
      <!-- 吸收浏览器对「用户名/密码」的误填充，避免塞进验证码、新密码框 -->
      <div class="forgot-autofill-trap" aria-hidden="true">
        <input type="text" tabindex="-1" autocomplete="username" />
        <input type="password" tabindex="-1" autocomplete="current-password" />
      </div>
      <el-form-item label="手机号">
        <el-input
          v-model="forgotForm.phone"
          placeholder="请输入11位手机号"
          maxlength="11"
          size="large"
          clearable
          name="forgot-recovery-tel"
          autocomplete="off"
          :readonly="forgotAutofillLock"
          @focus="onForgotFieldFocus"
        >
          <template #prefix>
            <el-icon><Cellphone /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="验证码">
        <div class="sms-row">
          <el-input
            v-model="forgotForm.smsCode"
            placeholder="请输入6位验证码"
            maxlength="6"
            size="large"
            clearable
            name="forgot-sms-otp"
            autocomplete="one-time-code"
            :readonly="forgotAutofillLock"
            @focus="onForgotFieldFocus"
            @keyup.enter="submitForgotReset"
          />
          <el-button
            class="sms-btn"
            :disabled="forgotSmsCooldown > 0 || smsSending"
            :loading="smsSending"
            @click="handleSendForgotSms"
          >
            {{ forgotSmsCooldown > 0 ? `${forgotSmsCooldown}s` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="forgotForm.newPassword"
          :type="showPwd1 ? 'text' : 'password'"
          placeholder="至少 6 位"
          size="large"
          clearable
          name="forgot-new-password"
          autocomplete="new-password"
          :readonly="forgotAutofillLock"
          @focus="onForgotFieldFocus"
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
          <template #suffix>
            <el-icon class="password-toggle" @click="showPwd1 = !showPwd1">
              <View v-if="!showPwd1" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input
          v-model="forgotForm.confirmPassword"
          :type="showPwd2 ? 'text' : 'password'"
          placeholder="请再次输入新密码"
          size="large"
          clearable
          name="forgot-new-password-2"
          autocomplete="new-password"
          :readonly="forgotAutofillLock"
          @focus="onForgotFieldFocus"
          @keyup.enter="submitForgotReset"
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
          <template #suffix>
            <el-icon class="password-toggle" @click="showPwd2 = !showPwd2">
              <View v-if="!showPwd2" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="submit-btn" :loading="forgotSubmitting" @click="submitForgotReset">
          重置密码
        </el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, View, Hide, Cellphone } from '@element-plus/icons-vue'
import { sendSmsCode, resetPasswordByPhone } from '@/api/auth'
import { isValidCnMobile, normalizeCnMobile } from '@/utils/phone'

const props = withDefaults(
  defineProps<{ modelValue: boolean; initialPhone?: string }>(),
  { initialPhone: '' }
)
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'success'): void }>()

const forgotForm = reactive({
  phone: '',
  smsCode: '',
  newPassword: '',
  confirmPassword: '',
})
const forgotSubmitting = ref(false)
const smsSending = ref(false)
const showPwd1 = ref(false)
const showPwd2 = ref(false)
const forgotSmsCooldown = ref(0)
/** 弹窗打开瞬间为 true，避免 Chrome 把账号密码填进验证码/新密码；用户聚焦任一输入框后关闭 */
const forgotAutofillLock = ref(false)
let forgotTimer: ReturnType<typeof setInterval> | null = null

function resetForgotForm() {
  forgotForm.phone = ''
  forgotForm.smsCode = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  showPwd1.value = false
  showPwd2.value = false
}

function onClosed() {
  resetForgotForm()
  forgotAutofillLock.value = false
  if (forgotTimer) {
    clearInterval(forgotTimer)
    forgotTimer = null
  }
  forgotSmsCooldown.value = 0
}

function onForgotFieldFocus() {
  forgotAutofillLock.value = false
}

function clearBrowserAutofillNoise() {
  forgotForm.smsCode = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
}

function onDialogOpened() {
  forgotAutofillLock.value = true
  clearBrowserAutofillNoise()
  nextTick(() => {
    clearBrowserAutofillNoise()
  })
  window.setTimeout(() => {
    clearBrowserAutofillNoise()
  }, 100)
}

watch(
  () => props.modelValue,
  (v) => {
    if (!v) return
    forgotAutofillLock.value = true
    clearBrowserAutofillNoise()
    if (props.initialPhone && isValidCnMobile(normalizeCnMobile(props.initialPhone))) {
      forgotForm.phone = normalizeCnMobile(props.initialPhone)
    } else {
      forgotForm.phone = ''
    }
  }
)

function startForgotCooldown() {
  const sec = 60
  forgotSmsCooldown.value = sec
  if (forgotTimer) clearInterval(forgotTimer)
  forgotTimer = setInterval(() => {
    forgotSmsCooldown.value--
    if (forgotSmsCooldown.value <= 0 && forgotTimer) {
      clearInterval(forgotTimer)
      forgotTimer = null
    }
  }, 1000)
}

async function handleSendForgotSms() {
  const phone = normalizeCnMobile(forgotForm.phone)
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }
  smsSending.value = true
  try {
    await sendSmsCode(phone, 'RESET_PASSWORD')
    ElMessage.success('验证码已发送')
    startForgotCooldown()
  } catch {
    // 拦截器已提示
  } finally {
    smsSending.value = false
  }
}

async function submitForgotReset() {
  const phone = normalizeCnMobile(forgotForm.phone)
  const code = (forgotForm.smsCode || '').trim()
  if (!isValidCnMobile(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!code || code.length < 4) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!forgotForm.newPassword || forgotForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次新密码输入不一致')
    return
  }
  forgotSubmitting.value = true
  try {
    await resetPasswordByPhone({
      phone,
      smsCode: code,
      newPassword: forgotForm.newPassword,
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 拦截器
  } finally {
    forgotSubmitting.value = false
  }
}

onUnmounted(() => {
  if (forgotTimer) clearInterval(forgotTimer)
})
</script>

<style scoped>
.forgot-autofill-trap {
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
  pointer-events: none;
}

.forgot-hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.forgot-form {
  position: relative;
}

.forgot-form :deep(.el-form-item__label) {
  font-weight: 500;
  font-size: 15px;
}

.forgot-form :deep(.el-form-item) {
  margin-bottom: 18px;
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

.password-toggle {
  cursor: pointer;
  color: #999;
}

.password-toggle:hover {
  color: #333;
}

.submit-btn {
  min-width: 120px;
  padding: 10px 24px;
  background-color: #111 !important;
  border-color: #111 !important;
}

.submit-btn:hover {
  background-color: #333 !important;
  border-color: #333 !important;
}
</style>
