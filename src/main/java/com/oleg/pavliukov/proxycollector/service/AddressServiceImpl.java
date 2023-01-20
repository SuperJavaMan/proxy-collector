package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.model.Proxy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final ProxyPriorityCache proxyPriorityCache;

    @Override
    public Mono<Proxy> getAddress() {
        return proxyPriorityCache.getNext();
    }

    @Override
    public Mono<Proxy> getFreshAndRecheckLast(Mono<Proxy> recheckAddress) {
        return proxyPriorityCache.getFreshAndInvalidate(recheckAddress);
    }
}
