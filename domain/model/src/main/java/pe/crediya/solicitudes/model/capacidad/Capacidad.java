package pe.crediya.solicitudes.model.capacidad;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Capacidad {
    Long idSolicitud;
    String documento;
    BigDecimal ingresoMensual;
    BigDecimal monto;
    BigDecimal tasaMensual;
    Integer plazoMeses;
    String tipoPrestamo;
    BigDecimal deudaMensual;
    Boolean validacionAutomatica;
    Instant fecha;
    String correlationId;
}
