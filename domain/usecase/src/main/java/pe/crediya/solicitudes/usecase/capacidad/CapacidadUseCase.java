package pe.crediya.solicitudes.usecase.capacidad;

import lombok.RequiredArgsConstructor;
import pe.crediya.solicitudes.model.common.ErrorCode;
import pe.crediya.solicitudes.model.estado.gateways.EstadoRepository;
import pe.crediya.solicitudes.model.estadocambiadoevent.EstadoCambiadoEvent;
import pe.crediya.solicitudes.model.estadocambiadoevent.PlanPago;
import pe.crediya.solicitudes.model.estadocambiadoevent.gateways.EstadoCambiadoEventRepository;
import pe.crediya.solicitudes.model.exception.BusinessException;
import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.model.solicitud.gateways.SolicitudRepository;
import pe.crediya.solicitudes.model.tipoprestamo.gateways.TipoPrestamoRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class CapacidadUseCase {
    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoRepository estadoRepository;
    private final EstadoCambiadoEventRepository eventRepository;

    public Mono<Solicitud> aplicarDecision(Long idSolicitud, String decision,
                                           String correlationId, BigDecimal cuotaPrestamoNuevo,
                                           List<PlanPago> planPago) {

        if (idSolicitud == null || decision == null || decision.isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Datos inválidos"));
        }
        final String target = switch (decision) {
            case "APROBADO" -> "APROBADO";
            case "RECHAZADO" -> "RECHAZADO";
            case "REVISION_MANUAL" -> "REVISION_MANUAL";
            default -> throw new IllegalArgumentException("Decision no reconocida: " + decision);
        };
        final String cid = (correlationId == null || correlationId.isBlank()) ? "n/a" : correlationId;
        // 1) Resolver idEstado por nombre
        return estadoRepository.findByNombre(target)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Estado no configurado: " + target)))
                // 2) Cargar solicitud y actualizar atómicamente
                .flatMap(est ->
                        solicitudRepository.findById(idSolicitud)
                                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Solicitud no existe")))
                                .flatMap(sol -> {
                                    sol.setIdEstado(est.getIdEstado());
                                    return solicitudRepository.save(sol)
                                            // 3) Publicar evento a notificaciones (punto 6)
                                            .flatMap(saved -> tipoPrestamoRepository.findById(saved.getIdTipoPrestamo())
                                                    .switchIfEmpty(Mono.error(new BusinessException(
                                                            ErrorCode.VALIDATION_ERROR,"Tipo de prestamo no encontrado"
                                                    )))
                                                    .flatMap(tipoPrestamo -> {
                                                        List<PlanPago> planEvento = null;
                                                        BigDecimal cuotaEvento = null;

                                                        if (planPago != null && !planPago.isEmpty()) {
                                                            planEvento = planPago.stream()
                                                                    .map(p -> PlanPago.builder()
                                                                            .nro(p.getNro())
                                                                            .fechaVencimiento(p.getFechaVencimiento())
                                                                            .cuota(p.getCuota())
                                                                            .interes(p.getInteres())
                                                                            .capital(p.getCapital())
                                                                            .saldo(p.getSaldo())
                                                                            .build()
                                                                    ).toList();
                                                            cuotaEvento = cuotaPrestamoNuevo;
                                                        }

                                                        var evt = EstadoCambiadoEvent.builder()
                                                                .idSolicitud(saved.getIdSolicitud())
                                                                .nuevoEstado(target)
                                                                .emailSolicitante(saved.getEmail())
                                                                .monto(saved.getMonto())
                                                                .tipoPrestamo(tipoPrestamo.getNombre())
                                                                .fecha(Instant.now())
                                                                .correlationId(cid)
                                                                .cuotaPrestamoNuevo(cuotaEvento)
                                                                .planPago(planEvento)
                                                                .build();
                                                        return eventRepository.publicarEstadoCambiado(evt)
                                                                .onErrorResume(ex -> Mono.empty())
                                                                .thenReturn(saved);
                                                    })
                                            );
                                })
                );
    }
}
