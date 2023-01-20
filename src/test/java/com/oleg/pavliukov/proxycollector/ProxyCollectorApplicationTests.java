package com.oleg.pavliukov.proxycollector;

import com.oleg.pavliukov.proxycollector.config.ParsersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ProxyCollectorApplicationTests {

    @Autowired
    private ParsersConfig parsersConfig;

    @Test
    void contextLoads() {}

    @Test
    void configsInitialized() {
        var freeProxyListConfig = parsersConfig.getFreeProxyListConfig();
        assertNotNull(freeProxyListConfig);
        assertNotNull(freeProxyListConfig.getUrl());
        assertNotNull(freeProxyListConfig.getTableClass());
        assertFalse(freeProxyListConfig.getEnabledAnonymities().isEmpty());
    }
}
