package com.oleg.pavliukov.proxycollector.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.oleg.pavliukov.proxycollector.model.Proxy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final ProxyAddressParser proxyAddressParser;
    private final ProxyQualityService proxyQualityService;
    private final ProxyPriorityCache proxyPriorityCache;

    @Override
    public Mono<Proxy> getAddress() {
        return proxyPriorityCache.getNext();
    }

    @Override
    public Mono<Proxy> getFreshAndRecheckLast(Mono<Proxy> recheckAddress) {
        return proxyPriorityCache.getFreshAndInvalidate(recheckAddress);
    }

    @SneakyThrows
    @Cacheable("proxyCache")
    public Mono<PriorityQueue<Proxy>> getCachedProxy() {
        return proxyQualityService.calcQuality(proxyAddressParser.parsePage())
                .filter(Proxy::isActive)
                .collect(PriorityQueue::new, PriorityQueue::add);
    }
}
