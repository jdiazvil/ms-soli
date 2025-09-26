package pe.crediya.solicitudes.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import pe.crediya.solicitudes.api.config.CorrelationIdWebFilter;
import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.usecase.solicitud.SolicitudUseCase;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerV1 {
    private final SolicitudUseCase solicitudUseCase;
    private final TransactionalOperator tx;

    // HU-1: Registrar solicitud
    public Mono<ServerResponse> crearSolicitud(ServerRequest request) {
        return request.bodyToMono(SolicitudRequest.class)
                .map(this::toDomain)
                .flatMap(s ->
                        Mono.deferContextual(ctx -> {
                            String cid = ctx.getOrDefault(CorrelationIdWebFilter.CONTEXT_KEY, "n/a");
                            return solicitudUseCase.crear(s, cid)
                                    .as(tx::transactional)
                                    .map(result -> new Object[] {result, cid});
                        })
                )
                .flatMap(resultAndCid -> {
                    Solicitud result = (Solicitud) resultAndCid[0];
                    String cid = (String) resultAndCid[1];
                    log.info("solicitud_creada cid={} id={} monto={}", cid, result.getIdSolicitud(), result.getMonto());
                    return ServerResponse.created(URI.create("/api/v1/solicitudes/" + result.getIdSolicitud()))
                            .contentType(APPLICATION_JSON)
                            .bodyValue(result);
                });
    }

    // HU-2: Obtener solicitud por ID
    public Mono<ServerResponse> obtenerSolicitudPorId(ServerRequest request) {
        Long idSolicitud = Long.valueOf(request.pathVariable("id"));
        return solicitudUseCase.obtenerPorId(idSolicitud)
                .flatMap(s -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(s))
                .switchIfEmpty(ServerResponse.notFound().build())
                .doOnError(e -> log.error("Error al obtener solicitud con id {}: {}", idSolicitud, e.getMessage()));
    }

    public Mono<ServerResponse> listarPendientes(ServerRequest request) {
        List<String> estados = request.queryParams().getOrDefault("estados",
                List.of("PENDIENTE", "PREAPROBADO"));

        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(10);

        return solicitudUseCase.listarPendientesRevision(estados, page, size)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> cambiarEstado(ServerRequest request) {
        return request.bodyToMono(CambiarEstadoRequest.class)
                .defaultIfEmpty(new CambiarEstadoRequest(null, null))
                .flatMap(req ->
                        Mono.deferContextual(ctx -> {
                            String correlationId = ctx.getOrDefault(CorrelationIdWebFilter.CONTEXT_KEY,"n/a");
                            return solicitudUseCase
                                .cambiarEstado(req.idSolicitud(), req.nuevoEstado(),correlationId)
                                .as(tx::transactional);
                        })
                )
                .flatMap(s -> ServerResponse.ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(s));
    }

    @PreAuthorize("hasRole('permissionGET')")
    public Mono<ServerResponse> listenGETUseCase(ServerRequest serverRequest) {
        // useCase.logic();
        return ServerResponse.ok().bodyValue("");
    }

    @PreAuthorize("hasRole('permissionGETOther')")
    public Mono<ServerResponse> listenGETOtherUseCase(ServerRequest serverRequest) {
        // useCase2.logic();
        return ServerResponse.ok().bodyValue("");
    }

    @PreAuthorize("hasRole('permissionPOST')")
    public Mono<ServerResponse> listenPOSTUseCase(ServerRequest serverRequest) {
        // useCase.logic();
        return ServerResponse.ok().bodyValue("");
    }

    public record SolicitudRequest(
            BigDecimal monto,
            Integer plazo,
            String email,
            Long id_estado,
            Long id_tipo_prestamo
    ) {}

    public record CambiarEstadoRequest(Long idSolicitud, String nuevoEstado) {}

    private Solicitud toDomain(SolicitudRequest r) {
        return Solicitud.builder()
                .monto(r.monto())
                .plazo(r.plazo())
                .email(r.email())
                .idEstado(r.id_estado())
                .idTipoPrestamo(r.id_tipo_prestamo())
                .build();
    }
}
