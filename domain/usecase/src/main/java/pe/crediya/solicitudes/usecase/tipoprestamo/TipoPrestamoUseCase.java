package pe.crediya.solicitudes.usecase.tipoprestamo;

import lombok.RequiredArgsConstructor;
import pe.crediya.solicitudes.model.common.ErrorCode;
import pe.crediya.solicitudes.model.exception.BusinessException;
import pe.crediya.solicitudes.model.tipoprestamo.TipoPrestamo;
import pe.crediya.solicitudes.model.tipoprestamo.gateways.TipoPrestamoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class TipoPrestamoUseCase {
    private final TipoPrestamoRepository tipoPrestamoRepository;

    public Mono<TipoPrestamo> crear(TipoPrestamo t) {
        return validar(t)
                .then(tipoPrestamoRepository.save(t));
    }

    public Mono<TipoPrestamo> actualizar(TipoPrestamo t) {
        if (t == null || t.getIdTipoPrestamo() == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id de tipo de préstamo es requerido"));

        return validar(t)
                .then(tipoPrestamoRepository.findById(t.getIdTipoPrestamo())
                        .switchIfEmpty(Mono.error(new BusinessException(
                                ErrorCode.VALIDATION_ERROR, "Tipo de préstamo no existe")))
                        .then(tipoPrestamoRepository.save(t))
                );
    }

    public Mono<TipoPrestamo> obtenerPorId(Long id) {
        if (id == null) return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id es requerido"));
        return tipoPrestamoRepository.findById(id);
    }

    public Flux<TipoPrestamo> listar() {
        return tipoPrestamoRepository.findAll();
    }

    public Mono<Void> eliminar(Long id) {
        if (id == null) return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id es requerido"));
        return tipoPrestamoRepository.deleteById(id);
    }

    // ---------- Validaciones
    private Mono<Void> validar(TipoPrestamo t) {
        if (t == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Tipo de préstamo es requerido"));

        if (isBlank(t.getNombre()))
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Nombre es requerido"));

        if (t.getMontoMinimo() == null || t.getMontoMinimo().compareTo(BigDecimal.ZERO) < 0)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Monto mínimo debe ser >= 0"));

        if (t.getMontoMaximo() == null || t.getMontoMaximo().compareTo(t.getMontoMinimo()) < 0)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Monto máximo debe ser >= monto mínimo"));

        if (t.getTasaInteres() == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Tasa de interés es requerida"));

        //Tasa en formato de procentade del 0.00 al 100.00
        if (t.getTasaInteres().compareTo(BigDecimal.ZERO) < 0 || t.getTasaInteres().compareTo(new BigDecimal("100")) > 0)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Tasa de interés fuera de rango (0..100)"));

        //if (t.getValidacionAutomatica() == null)
        //    return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Bandera validación automática es requerida"));

        return Mono.empty();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
