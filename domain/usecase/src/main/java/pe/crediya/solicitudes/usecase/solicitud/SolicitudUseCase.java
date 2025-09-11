package pe.crediya.solicitudes.usecase.solicitud;

import lombok.RequiredArgsConstructor;
import pe.crediya.solicitudes.model.common.ErrorCode;
import pe.crediya.solicitudes.model.common.PageResponse;
import pe.crediya.solicitudes.model.estado.Estado;
import pe.crediya.solicitudes.model.estado.gateways.EstadoRepository;
import pe.crediya.solicitudes.model.exception.BusinessException;
import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.model.solicitud.SolicitudDetalle;
import pe.crediya.solicitudes.model.solicitud.gateways.SolicitudRepository;
import pe.crediya.solicitudes.model.tipoprestamo.TipoPrestamo;
import pe.crediya.solicitudes.model.tipoprestamo.gateways.TipoPrestamoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SolicitudUseCase {
    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoRepository estadoRepository;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String ESTADO_PENDIENTE_NOMBRE = "PENDIENTE"; //Pendiente de revisión

    public Mono<Solicitud> crear(Solicitud s) {
        return validarCrear(s)
                .then(
                    Mono.defer(() ->
                        tipoPrestamoRepository.findById(s.getIdTipoPrestamo())
                                .switchIfEmpty(Mono.error(new BusinessException(
                                        ErrorCode.VALIDATION_ERROR, "Tipo de préstamo no existe")))
                                .flatMap(tp -> validarMontoEnRango(s.getMonto(), tp))
                                .then(Mono.defer(() -> resolverEstadoInicial(s)))
                                .then(Mono.defer(() -> solicitudRepository.save(s)))
                    )
                );
    }

    public Mono<Solicitud> obtenerPorId(Long idSolicitud) {
        if (idSolicitud == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id de solicitud es requerido"));
        return solicitudRepository.findById(idSolicitud);
    }

    public Flux<Solicitud> listar() {
        return solicitudRepository.findAll();
    }

    public Mono<Void> eliminar(Long idSolicitud) {
        if (idSolicitud == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id de solicitud es requerido"));
        return solicitudRepository.deleteById(idSolicitud);
    }

    public Mono<Solicitud> cambiarEstado(Long idSolicitud, Long idEstado) {
        if (idSolicitud == null || idEstado == null) {
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id solicitud/estado es requerido"));
        }
        return estadoRepository.findById(idEstado)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.VALIDATION_ERROR, "Estado no existe")))
                .then(Mono.defer(() ->
                        solicitudRepository.findById(idSolicitud)
                            .switchIfEmpty(Mono.error(new BusinessException(
                                ErrorCode.VALIDATION_ERROR, "Solicitud no existe")))
                            .flatMap(s -> {
                                s.setIdEstado(idEstado);
                                return solicitudRepository.save(s);
                            })
                        )
                );
    }

    private Mono<Estado> resolverEstadoInicial(Solicitud s) {
        if (s.getIdEstado() != null) {
            return Mono.empty();
        }
        return estadoRepository.findByNombre(ESTADO_PENDIENTE_NOMBRE)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Estado inicial no configurado: " + ESTADO_PENDIENTE_NOMBRE)))
                .doOnNext(e -> s.setIdEstado(e.getIdEstado()));
    }

    public Mono<PageResponse<SolicitudDetalle>> listarPendientesRevision(
            List<String> estadosFiltro, int page, int size) {

        if (estadosFiltro == null || estadosFiltro.isEmpty()) {
            return Mono.error(new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Debe especificar al menos un estado para filtrar"
            ));
        }
        if (page < 0 || size <= 0) {
            return Mono.error(new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Parámetros de paginación inválidos"
            ));
        }

        int offset = page * size;

        Mono<Long> totalMono = solicitudRepository.countByEstados(estadosFiltro);

        Flux<SolicitudDetalle> pageContent = solicitudRepository.findSolicitudesByEstados(estadosFiltro)
                .skip(offset)
                .take(size);

        return totalMono.zipWith(pageContent.collectList(),
                (total, content) -> {
                    int totalPages = (int) Math.ceil((double) total / size);
                    return PageResponse.<SolicitudDetalle>builder()
                            .content(content)
                            .page(page)
                            .size(size)
                            .totalElements(total)
                            .totalPages(totalPages)
                            .build();
                });
    }

    // ---------- Validaciones de dominio
    private Mono<Void> validarCrear(Solicitud s) {
        if (s == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Solicitud es requerida"));

        if (s.getMonto() == null || s.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Monto debe ser > 0"));

        if (s.getPlazo() == null || s.getPlazo() <= 0)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Plazo debe ser > 0"));

        if (!isEmail(s.getEmail()))
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Correo electrónico no válido"));

        if (s.getIdTipoPrestamo() == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Tipo de préstamo es requerido"));

        // if (s.getIdEstado() == null)
        //     return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Estado inicial es requerido"));
        return Mono.empty();
    }

    private Mono<Void> validarMontoEnRango(BigDecimal monto, TipoPrestamo tp) {
        if (tp.getMontoMinimo() == null || tp.getMontoMaximo() == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Rango de monto del producto no configurado"));

        if (monto.compareTo(tp.getMontoMinimo()) < 0 || monto.compareTo(tp.getMontoMaximo()) > 0) {
            return Mono.error(new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format("El monto %.2f está fuera del rango permitido (%.2f .. %.2f)",
                            monto, tp.getMontoMinimo(), tp.getMontoMaximo())
            ));
        }
        return Mono.empty();
    }

    private boolean isEmail(String s) {
        return s != null && EMAIL_REGEX.matcher(s).matches();
    }

}
