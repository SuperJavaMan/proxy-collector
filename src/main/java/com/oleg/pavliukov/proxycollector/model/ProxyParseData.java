package com.oleg.pavliukov.proxycollector.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ProxyParseData {
    private String ip;
    private Integer port;
    private String country;
    private boolean isHttps;
}
