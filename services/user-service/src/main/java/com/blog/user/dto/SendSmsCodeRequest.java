package com.blog.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendSmsCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入11位中国大陆手机号")
    private String phone;

    /** LOGIN、REGISTER 或 RESET_PASSWORD（找回密码） */
    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "^(LOGIN|REGISTER|RESET_PASSWORD)$", message = "scene 必须为 LOGIN、REGISTER 或 RESET_PASSWORD")
    private String scene;
}
