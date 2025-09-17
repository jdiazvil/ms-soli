package pe.crediya.solicitudes.r2dbc;

import org.springframework.web.reactive.function.client.WebClient;
import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.model.solicitud.SolicitudDetalle;
import pe.crediya.solicitudes.model.solicitud.gateways.SolicitudRepository;
import pe.crediya.solicitudes.r2dbc.dto.SolicitudDetalleEntity;
import pe.crediya.solicitudes.r2dbc.dto.UsuarioResponse;
import pe.crediya.solicitudes.r2dbc.entity.SolicitudEntity;
import pe.crediya.solicitudes.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class SolicitudReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Solicitud,
        SolicitudEntity,
        Long,
        SolicitudReactiveRepository
> implements SolicitudRepository {
    private final WebClient authWebClient;
    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository,
                                              ObjectMapper mapper,
                                              WebClient authWebClient) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Solicitud.class));
        this.authWebClient = authWebClient;
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

    @Override
    public Flux<SolicitudDetalle> findSolicitudesByEstados(List<String> estados) {
        return repository.findDetallesByEstados(estados)
                .collectList()
                .flatMapMany(detalles -> {
                    if (detalles.isEmpty()) return Flux.empty();

                    Set<String> emails = detalles.stream()
                            .map(SolicitudDetalleEntity::getEmail)
                            .collect(Collectors.toSet());

                    return authWebClient.post()
                            .uri("/usuarios/bulk")
                            .bodyValue(Map.of("emails", emails))
                            .retrieve()
                            .bodyToFlux(UsuarioResponse.class)
                            .collectMap(UsuarioResponse::getEmail)
                            .flatMapMany(usuariosMap ->
                                    Flux.fromIterable(detalles)
                                            .map(d -> {
                                                UsuarioResponse usuario = usuariosMap.get(d.getEmail());

                                                return SolicitudDetalle.builder()
                                                        .idSolicitud(d.getIdSolicitud())
                                                        .monto(d.getMonto())
                                                        .plazo(d.getPlazo())
                                                        .email(d.getEmail())
                                                        .nombreCliente(usuario != null
                                                                ? usuario.getNombre() + " " + usuario.getApellido()
                                                                : "N/D")
                                                        .tipoPrestamo(d.getTipoPrestamo())
                                                        .tasaInteres(d.getTasaInteres())
                                                        .estadoSolicitud(d.getEstadoSolicitud())
                                                        .salarioBase(usuario != null ? usuario.getSalarioBase() : null)
                                                        .montoMensualSolicitud(
                                                                d.getMonto().divide(new BigDecimal(d.getPlazo()), 2, RoundingMode.HALF_UP)
                                                        )
                                                        .build();
                                            })
                            );
                });
    }

    @Override
    public Mono<Long> countByEstados(List<String> estados) {
        return repository.countByEstados(estados);
    }
}
