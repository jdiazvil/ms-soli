package pe.crediya.solicitudes.model.estadocambiadoevent;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EstadoCambiadoEvent {
    Long idSolicitud;
    String nuevoEstado;
    String emailSolicitante;
    BigDecimal monto;
    String tipoPrestamo;
    Instant fecha;
    String correlationId;
    BigDecimal cuotaPrestamoNuevo;
    List<PlanPago> planPago;
}
