package com.blog.user.config;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AliyunDypnsClientConfig {

    private final AliyunDypnsProperties props;

    @Bean
    @ConditionalOnProperty(prefix = "aliyun.dypns", name = "enabled", havingValue = "true")
    public IAcsClient aliyunDypnsClient() {
        DefaultProfile profile = DefaultProfile.getProfile(
                props.getRegionId(),
                props.getAccessKeyId(),
                props.getAccessKeySecret());
        return new DefaultAcsClient(profile);
    }
}
