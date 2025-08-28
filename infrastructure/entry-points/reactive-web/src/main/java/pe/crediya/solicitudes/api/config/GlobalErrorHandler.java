package pe.crediya.solicitudes.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import pe.crediya.solicitudes.model.exception.BusinessException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        return Mono.deferContextual(ctx -> {
            String correlationId = ctx.getOrDefault(CorrelationIdWebFilter.CONTEXT_KEY, "n/a");

            log.info("Contextual CorrelationId: {}", correlationId);

            HttpStatus status;
            String code;
            String message;

            if (ex instanceof BusinessException be) {
                switch (be.getCode()) {
                    case VALIDATION_ERROR -> {
                        status = HttpStatus.BAD_REQUEST;
                        code = "VALIDATION_ERROR";
                    }
                    case DUPLICATE_KEY -> {
                        status = HttpStatus.CONFLICT;
                        code = "CONFLICT";
                    }
                    case FOREIGN_KEY_VIOLATION, CHECK_VIOLATION -> {
                        status = HttpStatus.BAD_REQUEST;
                        code = be.getCode().name();
                    }
                    case DATA_INTEGRITY -> {
                        status = HttpStatus.CONFLICT;
                        code = "DATA_INTEGRITY";
                    }
                    default -> {
                        status = HttpStatus.INTERNAL_SERVER_ERROR;
                        code = "INTERNAL_SERVER_ERROR";
                    }
                }
                message = be.getMessage();
            } else if (ex instanceof IllegalArgumentException iae) {
                status = HttpStatus.BAD_REQUEST;
                code = "VALIDATION_ERROR";
                message = iae.getMessage();
            } else if (ex instanceof ResponseStatusException rse) {
                HttpStatusCode sc = rse.getStatusCode();
                status = (sc instanceof HttpStatus http) ? http : HttpStatus.valueOf(sc.value());
                code = status.is4xxClientError() ? "CLIENT_ERROR" : "SERVER_ERROR";
                message = (rse.getReason() == null || rse.getReason().isBlank())
                        ? status.getReasonPhrase()
                        : rse.getReason();
            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                code = "INTERNAL_SERVER_ERROR";
                message = "Ocurrió un error inesperado";
            }

            log.error("http_error path={} status={} code={} msg={} correlationId={}",
                    exchange.getRequest().getPath().value(), status.value(), code, message, correlationId);


            var error = new ApiError(
                    code,
                    message,
                    exchange.getRequest().getPath().value(),
                    //correlationId,
                    status.value(),
                    OffsetDateTime.now().toString()
            );

            log.info("error: {}",error);

            var response = exchange.getResponse();
            if (response.isCommitted()) {
                return Mono.error(ex);
            }

            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            log.info("error2: {}",error);

            try {
                byte[] body = objectMapper.writeValueAsBytes(error);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
            } catch (Exception jacksonEx) {
                byte[] fallback = ("{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Serialization error\"}")
                        .getBytes(StandardCharsets.UTF_8);
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
            }
        });
    }

    public record ApiError(
            String code,
            String message,
            String path,
            //String correlationId,
            int status,
            String timestamp
    ) {}

}
