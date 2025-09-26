package pe.crediya.solicitudes.model.capacidad.gateways;

import pe.crediya.solicitudes.model.capacidad.Capacidad;
import reactor.core.publisher.Mono;

public interface CapacidadRepository {
    Mono<Void> enviarEvaluacion(Capacidad capacidad);
}
