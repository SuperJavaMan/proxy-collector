package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.config.ParsersConfig;
import com.oleg.pavliukov.proxycollector.model.ProxyParseData;
import com.oleg.pavliukov.proxycollector.util.GlobalConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FreeProxyListParser implements ProxyAddressParser {

    private final ParsersConfig parsersConfig;

    @Override
    public Flux<ProxyParseData> parsePage() throws Exception {
        log.info("Requested parse page");
        return Flux.just(Jsoup.connect(parsersConfig.getFreeProxyListConfig().getUrl()).get())
                .map(it -> it.select(parsersConfig.getFreeProxyListConfig().getTableClass())) // take table with addresses
                .map(it -> it.select("tr"))// take rows
                .flatMap(it -> Flux.fromStream(it.stream().map(col -> col.select("td")).map(Elements::eachText)))
                .filter(it -> it.size() == 8)
                .filter(it -> parsersConfig.getFreeProxyListConfig().getEnabledAnonymities()
                        .contains(it.get(COLUMNS.ANONYMITY.ordinal()))
                )
                .map(it -> ProxyParseData.builder()
                        .ip(it.get(COLUMNS.IP.ordinal()))
                        .port(Integer.parseInt(it.get(COLUMNS.PORT.ordinal())))
                        .country(Optional.of(it.get(COLUMNS.COUNTRY.ordinal())).orElse("Empty country field"))
                        .isHttps(Optional.of(it.get(COLUMNS.HTTPS.ordinal())).filter(str -> str.contains("y")).isPresent())
                        .build()
                )
                .filter(it -> Objects.nonNull(it.getIp()))
                .filter(it -> Objects.nonNull(it.getPort()))
                .filter(it -> it.getIp().matches(GlobalConstants.IP_PATTERN))
                .onErrorContinue((error, obj) -> log.error("Error parsing {}: {}", obj, error.getMessage()));
    }

    private enum COLUMNS {
        IP,
        PORT,
        CODE,
        COUNTRY,
        ANONYMITY,
        GOOGLE,
        HTTPS,
        LAST_CHECKED
    }
}
