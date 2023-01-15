package com.oleg.pavliukov.proxycollector.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.oleg.pavliukov.proxycollector.model.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProxyPriorityCache {
    private final ProxyAddressParser proxyAddressParser;
    private final ProxyQualityService proxyQualityService;

    private final LoadingCache<String, PriorityQueue<Proxy>> cache;
    public static final String KEY = "KEY";

    @Autowired
    public ProxyPriorityCache(ProxyAddressParser proxyAddressParser,
                              ProxyQualityService proxyQualityService) {
        this.proxyAddressParser = proxyAddressParser;
        this.proxyQualityService = proxyQualityService;
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public PriorityQueue<Proxy> load(String key) throws Exception {
                        PriorityQueue<Proxy> proxyPriorityQueue = new PriorityQueue<>();
                        proxyQualityService.calcQuality(proxyAddressParser.parsePage())
                                .filter(Proxy::isActive)
                                .subscribe(proxyPriorityQueue::add);
                        return proxyPriorityQueue;
                    }
                });
        this.cache.refresh(KEY);
    }

    public Mono<Proxy> getNext() {
        return getQueue().map(PriorityQueue::peek);
    }

    public Mono<Proxy> getFreshAndInvalidate(Mono<Proxy> current) {
        return current.zipWith(getQueue(), (proxy, queue) -> {
                    queue.remove(proxy);
                    log.info("Proxy removed from queue: " + proxy);
                    if (queue.isEmpty()) {
                        log.info("Queue is empty. Reloading cache");
                        cache.refresh(KEY);
                    }
                    return queue;
                })
                .flatMap(__ -> getNext());
    }

    private Mono<PriorityQueue<Proxy>> getQueue() {
        return Mono.just(cache.getUnchecked(KEY))
                .onErrorContinue((e, o) -> log.error("Error occured", e));
    }
}
