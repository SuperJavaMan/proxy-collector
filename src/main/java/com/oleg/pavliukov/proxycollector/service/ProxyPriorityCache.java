package com.oleg.pavliukov.proxycollector.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.oleg.pavliukov.proxycollector.model.Proxy;
import io.micrometer.core.instrument.Metrics;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ProxyPriorityCache {

    private final LoadingCache<String, PriorityQueue<Proxy>> cache;
    public static final String KEY = "KEY";
    public static final int CACHE_EXPIRATION_TIME = 5;
    public static final int FILL_CACHE_DURATION = 2;

    private final AtomicInteger cacheSizeGauge = Metrics.gauge("proxy_cache.size", new AtomicInteger(0));

    @Autowired
    public ProxyPriorityCache(ProxyAddressParser proxyAddressParser,
                              ProxyQualityService proxyQualityService) {
        this.cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(CACHE_EXPIRATION_TIME, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public @NonNull PriorityQueue<Proxy> load(@NonNull String key) throws Exception {
                        PriorityQueue<Proxy> proxyPriorityQueue = new PriorityQueue<>();
                        log.info("Start updating cache");
                        proxyQualityService.calcQuality(proxyAddressParser.parsePage())
                                .filter(Proxy::isActive)
                                .take(Duration.ofMinutes(FILL_CACHE_DURATION))
                                .doOnComplete(() -> log.info("Proxy cache fully updated updated"))
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
        return Mono.just(this.cache.getUnchecked(KEY))
                .onErrorContinue((e, o) -> log.error("Error occured", e));
    }

    @Scheduled(
            fixedDelayString = "${schedulers.proxy-cache.internal-state.delay:}",
            initialDelayString = "${schedulers.proxy-cache.internal-state.initial-delay}")
    public void checkInternalStateJob() {
        log.info("Run checkInternalStateJob");
        if (cacheSizeGauge != null) {
            cacheSizeGauge.set(cache.getUnchecked(KEY).size());
        }
    }
}
