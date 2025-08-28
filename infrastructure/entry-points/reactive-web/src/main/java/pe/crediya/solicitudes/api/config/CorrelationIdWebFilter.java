package pe.crediya.solicitudes.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);
    public static final String HEADER = "X-Correlation-Id";
    public static final String CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String cid = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (!StringUtils.hasText(cid)) {
            cid = UUID.randomUUID().toString();
        }

        exchange.getResponse().getHeaders().set(HEADER, cid);

        long start = System.currentTimeMillis();
        final String cidFinal = cid;
        var req = exchange.getRequest();

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CONTEXT_KEY, cidFinal))
                .doOnSubscribe(s ->
                        log.info("IN cid={} {} {}", cidFinal, req.getMethod(), req.getURI().getPath()))
                .doOnSuccess(v ->
                        log.info("OUT cid={} status={} elapsedMs={}",
                                cidFinal,
                                exchange.getResponse().getStatusCode(),
                                System.currentTimeMillis() - start))
                .doOnError(err ->
                        log.error("ERR cid={} status={} ex={}",
                                cidFinal,
                                exchange.getResponse().getStatusCode(), err.toString()));
    }
}
