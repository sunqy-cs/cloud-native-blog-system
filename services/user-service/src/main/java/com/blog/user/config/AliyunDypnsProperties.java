package com.blog.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云号码认证（DYPNS）SendSmsVerifyCode / CheckSmsVerifyCode 配置。
 * 控制台：<a href="https://dypns.console.aliyun.com">号码认证</a>，须使用赠送签名与配套模板。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.dypns")
public class AliyunDypnsProperties {

    /** 为 false 时发送接口将返回业务错误（本地开发可关） */
    private boolean enabled = false;

    private String accessKeyId = "";
    private String accessKeySecret = "";
    /** 如 cn-hangzhou */
    private String regionId = "cn-hangzhou";

    /** 赠送签名名称 */
    private String signName = "";

    /** 赠送模板 CODE */
    private String templateCode = "";

    /**
     * 模板变量 JSON（须与控制台模板变量名一致）。使用 ##code## 由阿里云生成验证码时须配合 codeType。
     * 示例：{"code":"##code##","min":"5"}
     */
    private String templateParamJson = "{\"code\":\"##code##\",\"min\":\"5\"}";

    /** 1：纯数字（与 ##code## 联用） */
    private Integer codeType = 1;

    private Integer codeLength = 6;
    private Integer validTimeSeconds = 300;
    private Integer sendIntervalSeconds = 60;

    /** 频控：默认方案名可空 */
    private String schemeName = "";
}
