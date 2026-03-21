package com.blog.user.dto;

import lombok.Data;

/**
 * 登录：二选一
 * <ul>
 *   <li>用户名 + 密码：username、password</li>
 *   <li>手机号 + 短信验证码：phone、smsCode（须先调发送接口，且模板使用 ##code## 以便走阿里云核验）</li>
 * </ul>
 */
@Data
public class LoginRequest {

    private String username;
    private String password;
    private String phone;
    private String smsCode;
}
