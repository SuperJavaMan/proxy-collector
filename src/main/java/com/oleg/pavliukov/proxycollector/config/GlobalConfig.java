package com.oleg.pavliukov.proxycollector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GlobalConfig {

    @Bean
    public WebClient checkQualityClient() {
        return WebClient.builder()
                .build();
    }
}
