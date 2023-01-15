package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.model.Proxy;
import com.oleg.pavliukov.proxycollector.model.ProxyParseData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProxyQualityService {
    Flux<Proxy> calcQuality(Flux<ProxyParseData> addresses);
    Mono<Proxy> recheckQuality(Mono<Proxy> address);
}
