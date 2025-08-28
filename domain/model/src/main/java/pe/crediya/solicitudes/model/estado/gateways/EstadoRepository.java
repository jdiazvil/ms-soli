package pe.crediya.solicitudes.model.estado.gateways;

import pe.crediya.solicitudes.model.estado.Estado;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EstadoRepository {
    Mono<Estado> save(Estado estado);
    Mono<Estado> findById(Long idEstado);
    Mono<Estado> findByNombre(String nombre);
    Flux<Estado> findAll();
    Mono<Void> deleteById(Long idEstado);
}
