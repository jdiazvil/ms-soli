package pe.crediya.solicitudes.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.crediya.solicitudes.r2dbc.dto.SolicitudDetalleEntity;
import pe.crediya.solicitudes.r2dbc.entity.SolicitudEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

// TODO: This file is just an example, you should delete or modify it
public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, Long>, ReactiveQueryByExampleExecutor<SolicitudEntity> {
    Flux<SolicitudEntity> findByIdEstadoIn(List<String> estados);

    @Query("""
    SELECT s.id_solicitud, s.monto, s.plazo, s.email,
           tp.nombre AS tipo_prestamo,
           tp.tasa_interes,
           e.nombre AS estado_solicitud
    FROM solicitud s
    JOIN tipo_prestamo tp ON s.id_tipo_prestamo = tp.id_tipo_prestamo
    JOIN estados e ON s.id_estado = e.id_estado
    WHERE e.nombre IN (:estados)""")
    Flux<SolicitudDetalleEntity> findDetallesByEstados(List<String> estados);

    @Query("""
    SELECT COUNT(*) FROM solicitud s JOIN estados e
    ON s.id_estado = e.id_estado
    WHERE e.nombre IN (:estados)""")
    Mono<Long> countByEstados(List<String> estados);
}
