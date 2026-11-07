package com.example.showyourvoice.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ImageProperties.class)
public class WebConfig implements WebMvcConfigurer {

    public static final String IMAGE_URL_PREFIX = "/images/";

    private final ImageProperties imageProperties;

    // 업로드된 사진을 /images/** 주소로 내려준다. 로그인 확인은 SecurityConfig가 한다.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(IMAGE_URL_PREFIX + "**")
                .addResourceLocations(imageProperties.resolvedUploadDir().toUri().toString());
    }
}
