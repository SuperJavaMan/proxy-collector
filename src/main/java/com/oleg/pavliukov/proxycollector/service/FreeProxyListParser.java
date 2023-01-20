package com.oleg.pavliukov.proxycollector.service;

import com.oleg.pavliukov.proxycollector.config.ParsersConfig;
import com.oleg.pavliukov.proxycollector.model.ProxyParseData;
import com.oleg.pavliukov.proxycollector.util.GlobalConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class FreeProxyListParser implements ProxyAddressParser {

    private final ParsersConfig parsersConfig;
    private final RetryTemplate freeProxyRetryTemplate;
    private final Timer retrieveDocumentTimer = Metrics.timer("proxy_collector.free_proxy_parser.get_doc_timer");
    private final Counter retrieveDocumentCounter = Metrics.counter("proxy_collector.free_proxy_parser.get_doc_counter");
    private final Counter retrieveDocumentCritCounter = Metrics.counter("proxy_collector.free_proxy_parser.get_doc_crit");

    @Override
    public Flux<ProxyParseData> parsePage() {
        log.info("Requested parse page");
        return Flux.just(getPage())
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
                .filter(it -> Objects.nonNull(it.getIp()) && it.getIp().matches(GlobalConstants.IP_PATTERN))
                .filter(it -> Objects.nonNull(it.getPort()))
                .onErrorContinue((error, obj) -> log.error("Error parsing {}: {}", obj, error.getMessage()));
    }

    private Document getPage() {
        retrieveDocumentCounter.increment();
        return freeProxyRetryTemplate.execute(
                __ -> retrieveDocumentTimer.wrap((Supplier<Document>) () -> {
                    try {
                        return Jsoup.connect(parsersConfig.getFreeProxyListConfig().getUrl()).get();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).get(),
                (retryContext) -> {
                    retrieveDocumentCritCounter.increment();
                    String msg = "Can not get document for parsing";
                    log.error(msg, retryContext.getLastThrowable());
                    throw new RuntimeException(retryContext.getLastThrowable());
                }
        );
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
