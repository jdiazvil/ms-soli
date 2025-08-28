package pe.crediya.solicitudes.model.solicitud;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {
    private Long idSolicitud;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    private Long idEstado;
    private Long idTipoPrestamo;
}
