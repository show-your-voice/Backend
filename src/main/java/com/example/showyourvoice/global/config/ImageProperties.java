package com.example.showyourvoice.global.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "image")
public record ImageProperties(Path uploadDir) {

    public Path resolvedUploadDir() {
        return uploadDir.toAbsolutePath().normalize();
    }
}
