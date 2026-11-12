package com.example.showyourvoice.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @Async 메서드를 Spring Boot가 만드는 applicationTaskExecutor 스레드 풀에서 실행한다.
@Configuration
@EnableAsync
public class AsyncConfig {
}
