package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.model.Proxy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AddressService {
    Mono<Proxy> getAddress();
    Mono<Proxy> getFreshAndRecheckLast(Mono<Proxy> recheckAddress);
}
