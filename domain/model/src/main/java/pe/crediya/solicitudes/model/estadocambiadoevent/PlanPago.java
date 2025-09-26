package pe.crediya.solicitudes.model.estadocambiadoevent;

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
public class PlanPago {
    private Integer nro;
    private String fechaVencimiento;  // "YYYY-MM-DD"
    private BigDecimal cuota;
    private BigDecimal interes;
    private BigDecimal capital;
    private BigDecimal saldo;
}
