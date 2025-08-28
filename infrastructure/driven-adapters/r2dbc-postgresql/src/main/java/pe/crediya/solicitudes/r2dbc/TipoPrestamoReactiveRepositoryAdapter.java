package pe.crediya.solicitudes.r2dbc;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import pe.crediya.solicitudes.model.tipoprestamo.TipoPrestamo;
import pe.crediya.solicitudes.model.tipoprestamo.gateways.TipoPrestamoRepository;
import pe.crediya.solicitudes.r2dbc.entity.TipoPrestamoEntity;
import pe.crediya.solicitudes.r2dbc.helper.ReactiveAdapterOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class TipoPrestamoReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        TipoPrestamo/* change for domain model */,
        TipoPrestamoEntity/* change for adapter model */,
        Long,
        TipoPrestamoReactiveRepository
> implements TipoPrestamoRepository {
    public TipoPrestamoReactiveRepositoryAdapter(TipoPrestamoReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, TipoPrestamo.class));
    }

    @Override
    public Mono<TipoPrestamo> save(TipoPrestamo entity) {
        return super.save(entity);
    }

    @Override
    public Mono<TipoPrestamo> findById(Long id) {
        return super.findById(id);
    }

    @Override
    public Flux<TipoPrestamo> findAll() {
        return super.findAll();
    }

    @Override
    public Mono<Void> deleteById(Long idTipoPrestamo) {
        return repository.deleteById(idTipoPrestamo).then();
    }
}
