package com.oleg.pavliukov.proxycollector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties("parsers")
public class ParsersConfig {

    private FreeProxyListConfig freeProxyListConfig;

    @Data
    public static class FreeProxyListConfig {
        private String url;
        private String tableClass;
        private Set<String> enabledAnonymities = new HashSet<>();
    }
}
