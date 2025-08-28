package pe.crediya.solicitudes.r2dbc;

import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.model.solicitud.gateways.SolicitudRepository;
import pe.crediya.solicitudes.r2dbc.entity.SolicitudEntity;
import pe.crediya.solicitudes.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class SolicitudReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Solicitud,
        SolicitudEntity,
        Long,
        SolicitudReactiveRepository
> implements SolicitudRepository {
    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Solicitud.class));
    }

    @Override
    public Mono<Solicitud> save(Solicitud entity) {
        return super.save(entity);
    }

    @Override
    public Mono<Solicitud> findById(Long id) {
        return super.findById(id);
    }

    @Override
    public Flux<Solicitud> findAll() {
        return super.findAll();
    }

    @Override
    public Mono<Void> deleteById(Long idSolicitud) {
        return repository.deleteById(idSolicitud).then();
    }
}
