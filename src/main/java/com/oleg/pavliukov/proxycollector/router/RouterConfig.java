package com.oleg.pavliukov.proxycollector.router;

import com.oleg.pavliukov.proxycollector.model.Proxy;
import com.oleg.pavliukov.proxycollector.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private final AddressService addressService;

    public static final String BASE_PATH = "/api/v1";

    @Bean
    RouterFunction<ServerResponse> getEmployeeByIdRoute() {
        return route(GET(BASE_PATH + "/ip"),
                req -> ServerResponse.ok().body(addressService.getAddress(), Proxy.class)
        )
                .andRoute(POST(BASE_PATH + "/ip").and(accept(MediaType.APPLICATION_JSON)),
                        req -> {
                            Mono<Proxy> body = req.body(toMono(Proxy.class));
                            return ServerResponse.ok().body(addressService.getFreshAndRecheckLast(body), Proxy.class);
                        }
                );
    }
}
