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
public class SolicitudDetalle {
    private Long idSolicitud;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    private String nombreCliente;
    private String tipoPrestamo;
    private BigDecimal tasaInteres;
    private String estadoSolicitud;
    private BigDecimal salarioBase;
    private BigDecimal montoMensualSolicitud;
}
