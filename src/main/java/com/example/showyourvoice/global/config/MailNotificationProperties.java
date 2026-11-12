package com.example.showyourvoice.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "notification.mail")
public record MailNotificationProperties(String from, String frontendUrl) {

    public String managerReportUrl(Long postId) {
        return StringUtils.trimTrailingCharacter(frontendUrl, '/') + "/manager/reports/" + postId;
    }
}
