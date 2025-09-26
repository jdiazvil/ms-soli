package pe.crediya.solicitudes.r2dbc.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String documentoIdentidad;
    private BigDecimal salarioBase;
    private Long idRol;
}
