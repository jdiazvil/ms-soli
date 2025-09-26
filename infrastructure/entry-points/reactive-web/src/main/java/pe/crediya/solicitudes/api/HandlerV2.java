package pe.crediya.solicitudes.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import pe.crediya.solicitudes.model.estadocambiadoevent.PlanPago;
import pe.crediya.solicitudes.usecase.capacidad.CapacidadUseCase;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
public class HandlerV2 {
    private final CapacidadUseCase capacidadUseCase;
    private final TransactionalOperator tx;

    public Mono<ServerResponse> callbackCapacidad(ServerRequest request) {
        return request.bodyToMono(CapacidadRequest.class)
                .defaultIfEmpty(new CapacidadRequest(null, null, null,
                        null, null, null, null, null, null))
                .flatMap(req -> {
                            List<PlanPago> planes = Optional.ofNullable(req.planPago)
                                    .orElse(List.of())
                                    .stream()
                                    .map(this::toDomain)
                                    .toList();

                            return capacidadUseCase.aplicarDecision(
                                            req.idSolicitud, req.decision, req.correlationId, req.cuotaPrestamoNuevo, planes)
                                    .as(tx::transactional)
                                    .flatMap(s -> ServerResponse.ok()
                                            .contentType(APPLICATION_JSON)
                                            .bodyValue(s));
                        }
                );
    }

    public record CapacidadRequest(
            Long idSolicitud,
            String decision,
            BigDecimal cuotaPrestamoNuevo,
            BigDecimal capacidadMaxima,
            BigDecimal deudaMensualActual,
            BigDecimal capacidadDisponible,
            List<PlanPagoRequest>planPago,
            String correlationId,
            String motivo
    ){}

    public record PlanPagoRequest (
        Integer nro,
        String fechaVencimiento,
        BigDecimal cuota,
        BigDecimal interes,
        BigDecimal capital,
        BigDecimal saldo){
    }

    private PlanPago toDomain(PlanPagoRequest r){
        return PlanPago.builder()
                .nro(r.nro)
                .fechaVencimiento(r.fechaVencimiento)
                .cuota(r.cuota)
                .interes(r.interes)
                .capital(r.capital)
                .saldo(r.saldo)
                .build();
    }


}
