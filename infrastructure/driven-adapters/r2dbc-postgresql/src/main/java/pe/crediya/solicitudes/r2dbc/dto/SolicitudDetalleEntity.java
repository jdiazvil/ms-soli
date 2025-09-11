package pe.crediya.solicitudes.r2dbc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("solicitud")
public class SolicitudDetalleEntity {
    @Id
    @Column("id_solicitud")
    private Long idSolicitud;

    @Column("monto")
    private BigDecimal monto;

    @Column("plazo")
    private Integer plazo;

    @Column("email")
    private String email;

    @Column("tipo_prestamo")
    private String tipoPrestamo;

    @Column("tasa_interes")
    private BigDecimal tasaInteres;

    @Column("estado_solicitud")
    private String estadoSolicitud;
}
