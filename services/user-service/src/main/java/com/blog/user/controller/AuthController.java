package com.blog.user.controller;

import com.blog.user.dto.ResetPasswordRequest;
import com.blog.user.dto.SendSmsCodeRequest;
import com.blog.user.service.AliyunSmsVerifyService;
import com.blog.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AliyunSmsVerifyService aliyunSmsVerifyService;
    private final UserService userService;

    @PostMapping("/sms/send")
    public ResponseEntity<Void> sendSms(@Valid @RequestBody SendSmsCodeRequest req) {
        aliyunSmsVerifyService.sendVerifyCode(req.getPhone(), req.getScene());
        return ResponseEntity.noContent().build();
    }

    /** 通过手机验证码设置新密码，无需登录 */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPasswordByPhone(req.getPhone(), req.getSmsCode(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
