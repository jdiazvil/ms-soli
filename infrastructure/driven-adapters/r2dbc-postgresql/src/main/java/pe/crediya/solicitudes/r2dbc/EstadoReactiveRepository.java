package pe.crediya.solicitudes.r2dbc;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.crediya.solicitudes.r2dbc.entity.EstadoEntity;
import reactor.core.publisher.Mono;

// TODO: This file is just an example, you should delete or modify it
public interface EstadoReactiveRepository extends ReactiveCrudRepository<EstadoEntity, Long>, ReactiveQueryByExampleExecutor<EstadoEntity> {
    Mono<EstadoEntity> findByNombre(String nombre);
}
