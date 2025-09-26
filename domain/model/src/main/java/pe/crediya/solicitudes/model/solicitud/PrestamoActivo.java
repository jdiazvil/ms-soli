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
public class PrestamoActivo {
    private BigDecimal monto;
    private Integer plazoMeses;
    private BigDecimal tasaInteresPorcentaje;
}
