package pe.crediya.solicitudes.model.tipoprestamo.gateways;

import pe.crediya.solicitudes.model.tipoprestamo.TipoPrestamo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TipoPrestamoRepository {
    Mono<TipoPrestamo> save(TipoPrestamo tipoPrestamo);
    Mono<TipoPrestamo> findById(Long idTipoPrestamo);
    Flux<TipoPrestamo> findAll();
    Mono<Void> deleteById(Long idTipoPrestamo);
}
