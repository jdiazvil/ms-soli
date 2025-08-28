package pe.crediya.solicitudes.usecase.estado;

import lombok.RequiredArgsConstructor;
import pe.crediya.solicitudes.model.common.ErrorCode;
import pe.crediya.solicitudes.model.estado.Estado;
import pe.crediya.solicitudes.model.estado.gateways.EstadoRepository;
import pe.crediya.solicitudes.model.exception.BusinessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class EstadoUseCase {
    private final EstadoRepository estadoRepository;

    public Mono<Estado> crear(Estado e) {
        return validar(e).then(estadoRepository.save(e));
    }

    public Mono<Estado> actualizar(Estado e) {
        if (e == null || e.getIdEstado() == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id de estado es requerido"));

        return validar(e)
                .then(estadoRepository.findById(e.getIdEstado())
                        .switchIfEmpty(Mono.error(new BusinessException(
                                ErrorCode.VALIDATION_ERROR, "Estado no existe")))
                        .then(estadoRepository.save(e))
                );
    }

    public Mono<Estado> obtenerPorId(Long id) {
        if (id == null) return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id es requerido"));
        return estadoRepository.findById(id);
    }

    public Flux<Estado> listar() {
        return estadoRepository.findAll();
    }

    public Mono<Void> eliminar(Long id) {
        if (id == null) return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Id es requerido"));
        return estadoRepository.deleteById(id);
    }

    // ---------- Validaciones
    private Mono<Void> validar(Estado e) {
        if (e == null)
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Estado es requerido"));
        if (isBlank(e.getNombre()))
            return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR, "Nombre de estado es requerido"));
        // descripción opcional
        return Mono.empty();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
