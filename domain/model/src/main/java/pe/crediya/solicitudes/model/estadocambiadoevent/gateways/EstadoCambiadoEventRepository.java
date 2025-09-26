package pe.crediya.solicitudes.model.estadocambiadoevent.gateways;

import pe.crediya.solicitudes.model.estadocambiadoevent.EstadoCambiadoEvent;
import reactor.core.publisher.Mono;

public interface EstadoCambiadoEventRepository {
    Mono<Void> publicarEstadoCambiado(EstadoCambiadoEvent event);
}
