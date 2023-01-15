package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.model.Proxy;
import com.oleg.pavliukov.proxycollector.model.ProxyParseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyQualityServiceImpl implements ProxyQualityService {

    private final WebClient checkQualityClient;

    private final Integer CHECK_BATCH_SIZE = 10;
    private final Integer CHECK_THREAD_COUNT = 10;
    private final Duration CHECK_BATCH_DELAY = Duration.ofMillis(100);
    private final String CHECK_URL = "http://www.google.com/";

    @Override
    public Flux<Proxy> calcQuality(Flux<ProxyParseData> addresses) {
        log.info("Request calc priorities for address");
        return addresses.buffer(CHECK_BATCH_SIZE)
                .delayElements(CHECK_BATCH_DELAY)
                .flatMap(batch -> Flux.fromIterable(batch)
                        .parallel(CHECK_THREAD_COUNT)
                        .runOn(Schedulers.parallel())
                        .map(it -> checkPerformanceAndConvert(it.getIp(), it.getPort()))
                );
    }

    public Proxy checkPerformanceAndConvert(String ip, Integer port) {
        log.info("Check ip: {}, port: {}", ip, port);
        Proxy.ProxyBuilder proxyBuilder = Proxy.builder()
                .ip(ip)
                .port(port)
                .isActive(true);
        long startTime = System.currentTimeMillis();

        try {
            RestTemplate restTemplate = prepareRestTemplate(ip, port);
            restTemplate.getForEntity(CHECK_URL, String.class);
            proxyBuilder.isActive(true);
        } catch (Exception e) {
            log.info("Address inactive: {}:{}. Msg: {}", ip, port, e.getMessage());
            proxyBuilder.isActive(false);
        }

        long endTime = System.currentTimeMillis();
        return proxyBuilder
                .quality(endTime - startTime)
                .build();
    }

    @Override
    public Mono<Proxy> recheckQuality(Mono<Proxy> address) {
        return address.map(it -> checkPerformanceAndConvert(it.getIp(), it.getPort()));
    }

    private RestTemplate prepareRestTemplate(String ip, Integer port) {
        var proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);
        return new RestTemplate(requestFactory);
    }
}
