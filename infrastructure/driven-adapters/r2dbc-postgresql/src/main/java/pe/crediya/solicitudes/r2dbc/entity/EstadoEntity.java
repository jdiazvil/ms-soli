package pe.crediya.solicitudes.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("estados")
public class EstadoEntity {
    @Id
    @Column("id_estado")
    private Long idEstado;

    @Column("nombre")
    private String nombre;

    @Column("descripcion")
    private String descripcion;
}
