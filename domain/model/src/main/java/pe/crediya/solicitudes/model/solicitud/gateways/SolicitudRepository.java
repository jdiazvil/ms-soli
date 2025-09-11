package pe.crediya.solicitudes.model.solicitud.gateways;

import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.model.solicitud.SolicitudDetalle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud solicitud);
    Mono<Solicitud> findById(Long idSolicitud);
    Flux<Solicitud> findAll();
    Mono<Void> deleteById(Long idSolicitud);

    Flux<SolicitudDetalle> findSolicitudesByEstados(List<String> estados);
    Mono<Long> countByEstados(List<String> estados);
}
