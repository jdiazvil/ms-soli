package pe.crediya.solicitudes.r2dbc;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import pe.crediya.solicitudes.model.estado.Estado;
import pe.crediya.solicitudes.model.estado.gateways.EstadoRepository;
import pe.crediya.solicitudes.r2dbc.entity.EstadoEntity;
import pe.crediya.solicitudes.r2dbc.helper.ReactiveAdapterOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class EstadoReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Estado,
        EstadoEntity,
        Long,
        EstadoReactiveRepository
> implements EstadoRepository {
    public EstadoReactiveRepositoryAdapter(EstadoReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Estado.class));
    }

    @Override
    public Mono<Estado> save(Estado entity) {
        return super.save(entity);
    }

    @Override
    public Mono<Estado> findById(Long id) {
        return super.findById(id);
    }

    @Override
    public Flux<Estado> findAll() {
        return super.findAll();
    }

    @Override
    public Mono<Estado> findByNombre(String nombre) {
        return repository.findByNombre(nombre)
                .map(e -> mapper.map(e, Estado.class));
    }

    @Override
    public Mono<Void> deleteById(Long idEstado) {
        return repository.deleteById(idEstado).then();
    }
}
