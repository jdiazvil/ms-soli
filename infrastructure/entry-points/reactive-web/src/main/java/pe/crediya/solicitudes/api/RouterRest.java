package pe.crediya.solicitudes.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import pe.crediya.solicitudes.model.common.PageResponse;
import pe.crediya.solicitudes.model.solicitud.Solicitud;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;


@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitudes",
                    method = RequestMethod.POST,
                    beanClass = HandlerV1.class,
                    beanMethod = "crearSolicitud",
                    operation = @Operation(
                            operationId = "crearSolicitud",
                            summary = "Registrar nueva solicitud de préstamo",
                            description = "Crea una solicitud de préstamo con datos de usuario, monto y plazo.",
                            security = { @SecurityRequirement(name = "bearer-jwt") },
                            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                    required = true,
                                    content = @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = HandlerV1.SolicitudRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Solicitud creada",
                                            content = @Content(schema = @Schema(implementation = Solicitud.class))),
                                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                                    @ApiResponse(responseCode = "401", description = "No autenticado"),
                                    @ApiResponse(responseCode = "403", description = "Sin rol adecuado"),
                                    @ApiResponse(responseCode = "409", description = "Email duplicado o tipo de préstamo no existe")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes/{id}",
                    method = RequestMethod.GET,
                    beanClass = HandlerV1.class,
                    beanMethod = "obtenerSolicitudPorId",
                    operation = @Operation(
                            operationId = "obtenerSolicitudPorId",
                            summary = "Obtener solicitud de préstamo por ID",
                            description = "Consulta una solicitud de préstamo existente utilizando su ID.",
                            security = { @SecurityRequirement(name = "bearer-jwt") },
                            parameters = {
                                    @Parameter(name = "id", in = ParameterIn.PATH, description = "ID de la solicitud", required = true)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Solicitud encontrada",
                                            content = @Content(schema = @Schema(implementation = Solicitud.class))),
                                    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes/pendientes",
                    method = RequestMethod.GET,
                    beanClass = HandlerV1.class,
                    beanMethod = "listarPendientes",
                    operation = @Operation(
                            operationId = "listarPendientes",
                            summary = "Listar solicitudes pendientes",
                            description = "Devuelve lista paginada y filtrada de solicitudes",
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Listado paginado",
                                            content = @Content(schema = @Schema(implementation = PageResponse.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(HandlerV1 handlerV1, HandlerV2 handlerV2) {
        return RouterFunctions
            .route()
            .path("/api/v1", builder -> builder
                .POST("/solicitudes", accept(APPLICATION_JSON), handlerV1::crearSolicitud)
                .GET("/solicitudes/{id}", accept(APPLICATION_JSON), handlerV1::obtenerSolicitudPorId)
                .GET("/solicitudes",accept(APPLICATION_JSON),handlerV1::listarPendientes)
            )
            /*
            .path("/api/v2", builder -> builder.GET("/usecase/path", handlerV2::listenGETUseCase).POST("/usecase/otherpath", handlerV2::listenPOSTUseCase).GET("/otherusercase/path", handlerV2::listenGETOtherUseCase))
            */
            .build();
        }
}
