package pe.crediya.solicitudes.r2dbc.entity;

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
public class SolicitudEntity {
    @Id
    @Column("id_solicitud")
    private Long idSolicitud;

    @Column("monto")
    private BigDecimal monto;

    @Column("plazo")
    private Integer plazo;

    @Column("email")
    private String email;

    @Column("id_estado")
    private Long idEstado;

    @Column("id_tipo_prestamo")
    private Long idTipoPrestamo;
}
