package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.model.ProxyParseData;
import reactor.core.publisher.Flux;

public interface ProxyAddressParser {
    Flux<ProxyParseData> parsePage() throws Exception;
}
