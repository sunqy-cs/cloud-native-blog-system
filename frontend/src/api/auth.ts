import request from './request'

/** 短信验证码业务场景：登录 / 注册 / 找回密码 */
export type SmsScene = 'LOGIN' | 'REGISTER' | 'RESET_PASSWORD'

/**
 * 发送手机验证码
 * POST /api/auth/sms/send
 */
export function sendSmsCode(phone: string, scene: SmsScene): Promise<void> {
  return request.post('auth/sms/send', { phone, scene })
}

export interface ResetPasswordBody {
  phone: string
  smsCode: string
  newPassword: string
}

/** 通过手机验证码重置密码，无需登录。POST /api/auth/password/reset */
export function resetPasswordByPhone(body: ResetPasswordBody): Promise<void> {
  return request.post('auth/password/reset', body)
}
