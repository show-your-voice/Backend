package com.example.showyourvoice.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailNotificationProperties.class)
public class MailConfig {
}
