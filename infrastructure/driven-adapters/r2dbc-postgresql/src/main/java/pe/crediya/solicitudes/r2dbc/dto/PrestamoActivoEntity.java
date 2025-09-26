package pe.crediya.solicitudes.r2dbc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("solicitud")
public class PrestamoActivoEntity {
    @Column("monto")
    private BigDecimal monto;

    @Column("plazo_meses")
    private Integer plazoMeses;

    @Column("tasa_interes_porcentaje")
    private BigDecimal tasaInteresPorcentaje;
}
