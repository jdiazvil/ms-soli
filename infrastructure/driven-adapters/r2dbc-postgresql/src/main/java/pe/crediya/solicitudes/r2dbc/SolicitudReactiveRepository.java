package pe.crediya.solicitudes.r2dbc;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.crediya.solicitudes.r2dbc.entity.SolicitudEntity;

// TODO: This file is just an example, you should delete or modify it
public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, Long>, ReactiveQueryByExampleExecutor<SolicitudEntity> {


}
