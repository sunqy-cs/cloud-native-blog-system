package com.blog.user.service;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.blog.user.config.AliyunDypnsProperties;
import com.blog.user.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 封装阿里云号码认证：SendSmsVerifyCode、CheckSmsVerifyCode（OpenAPI RPC）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsVerifyService {

    private static final String DOMAIN = "dypnsapi.aliyuncs.com";
    private static final String VERSION = "2017-05-25";
    private static final String ACTION_SEND = "SendSmsVerifyCode";
    private static final String ACTION_CHECK = "CheckSmsVerifyCode";

    private final AliyunDypnsProperties props;
    private final ObjectProvider<IAcsClient> acsClientProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 发送短信验证码（TemplateParam 含 ##code## 时由阿里云生成并可通过 Check 核验） */
    public void sendVerifyCode(String phone, String scene) {
        if (!props.isEnabled()) {
            throw BusinessException.badRequest("短信服务未启用，请在配置中设置 aliyun.dypns.enabled=true 并填写密钥与模板");
        }
        IAcsClient client = acsClientProvider.getIfAvailable();
        if (client == null) {
            throw BusinessException.badRequest("短信客户端未初始化，请检查 AccessKey 配置");
        }
        if (!StringUtils.hasText(props.getSignName()) || !StringUtils.hasText(props.getTemplateCode())) {
            throw BusinessException.badRequest("请配置 aliyun.dypns.sign-name 与 template-code");
        }
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(DOMAIN);
        request.setSysVersion(VERSION);
        request.setSysAction(ACTION_SEND);
        request.putQueryParameter("PhoneNumber", normalizePhone(phone));
        request.putQueryParameter("SignName", props.getSignName());
        request.putQueryParameter("TemplateCode", props.getTemplateCode());
        request.putQueryParameter("TemplateParam", props.getTemplateParamJson());
        if (props.getCodeType() != null) {
            request.putQueryParameter("CodeType", String.valueOf(props.getCodeType()));
        }
        if (props.getCodeLength() != null) {
            request.putQueryParameter("CodeLength", String.valueOf(props.getCodeLength()));
        }
        if (props.getValidTimeSeconds() != null) {
            request.putQueryParameter("ValidTime", String.valueOf(props.getValidTimeSeconds()));
        }
        if (props.getSendIntervalSeconds() != null) {
            request.putQueryParameter("Interval", String.valueOf(props.getSendIntervalSeconds()));
        }
        if (StringUtils.hasText(props.getSchemeName())) {
            request.putQueryParameter("SchemeName", props.getSchemeName());
        }
        request.putQueryParameter("CountryCode", "86");
        // 业务侧区分登录/注册流水（可选）
        if (StringUtils.hasText(scene)) {
            request.putQueryParameter("OutId", scene + ":" + System.currentTimeMillis());
        }
        request.putQueryParameter("ReturnVerifyCode", "false");

        try {
            CommonResponse response = client.getCommonResponse(request);
            assertRpcOk(response);
        } catch (ClientException e) {
            log.warn("SendSmsVerifyCode ClientException: {}", e.getMessage());
            throw translateAliyunException(e);
        }
    }

    /** 调用 CheckSmsVerifyCode，仅当 Model.VerifyResult 为 PASS 时通过 */
    public void checkVerifyCode(String phone, String verifyCode) {
        if (!props.isEnabled()) {
            throw BusinessException.badRequest("短信服务未启用");
        }
        IAcsClient client = acsClientProvider.getIfAvailable();
        if (client == null) {
            throw BusinessException.badRequest("短信客户端未初始化");
        }
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(DOMAIN);
        request.setSysVersion(VERSION);
        request.setSysAction(ACTION_CHECK);
        request.putQueryParameter("PhoneNumber", normalizePhone(phone));
        request.putQueryParameter("VerifyCode", verifyCode.trim());
        request.putQueryParameter("CountryCode", "86");
        if (StringUtils.hasText(props.getSchemeName())) {
            request.putQueryParameter("SchemeName", props.getSchemeName());
        }
        try {
            CommonResponse response = client.getCommonResponse(request);
            assertCheckPass(response);
        } catch (ClientException e) {
            log.warn("CheckSmsVerifyCode ClientException: {}", e.getMessage());
            throw translateAliyunException(e);
        }
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.trim().replaceAll("\\s+", "");
    }

    private void assertRpcOk(CommonResponse response) {
        String data = response.getData();
        try {
            JsonNode root = objectMapper.readTree(data);
            String code = text(root, "Code");
            if (!"OK".equalsIgnoreCase(code)) {
                String msg = text(root, "Message");
                throw BusinessException.badRequest("短信发送失败: " + (msg != null ? msg : code));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Parse SendSmsVerifyCode response: {}", data, e);
            throw BusinessException.badRequest("短信服务响应异常");
        }
    }

    private void assertCheckPass(CommonResponse response) {
        String data = response.getData();
        try {
            JsonNode root = objectMapper.readTree(data);
            String code = text(root, "Code");
            if (!"OK".equalsIgnoreCase(code)) {
                String msg = text(root, "Message");
                throw BusinessException.unauthorized(msg != null ? msg : "验证码校验失败");
            }
            JsonNode model = root.get("Model");
            if (model == null || !model.has("VerifyResult")) {
                throw BusinessException.unauthorized("验证码无效或已过期");
            }
            String vr = model.get("VerifyResult").asText();
            if (!"PASS".equalsIgnoreCase(vr)) {
                throw BusinessException.unauthorized("验证码错误或已失效");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Parse CheckSmsVerifyCode response: {}", data, e);
            throw BusinessException.badRequest("验证码核验响应异常");
        }
    }

    private static String text(JsonNode root, String field) {
        if (root == null || !root.has(field) || root.get(field).isNull()) return null;
        return root.get(field).asText();
    }

    private static RuntimeException translateAliyunException(ClientException e) {
        String err = e.getErrCode();
        if ("MOBILE_NUMBER_ILLEGAL".equalsIgnoreCase(err)) {
            return BusinessException.badRequest("手机号格式不正确");
        }
        if ("FREQUENCY_FAIL".equalsIgnoreCase(err)) {
            return BusinessException.badRequest("发送过于频繁，请稍后再试");
        }
        if ("BUSINESS_LIMIT_CONTROL".equalsIgnoreCase(err)) {
            return BusinessException.badRequest("该号码今日发送次数已达上限");
        }
        return BusinessException.badRequest("短信服务异常: " + e.getMessage());
    }
}
