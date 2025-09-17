package pe.crediya.solicitudes.usecase.solicitud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pe.crediya.solicitudes.model.common.ErrorCode;
import pe.crediya.solicitudes.model.estado.Estado;
import pe.crediya.solicitudes.model.estado.gateways.EstadoRepository;
import pe.crediya.solicitudes.model.exception.BusinessException;
import pe.crediya.solicitudes.model.solicitud.Solicitud;
import pe.crediya.solicitudes.model.solicitud.SolicitudDetalle;
import pe.crediya.solicitudes.model.solicitud.gateways.SolicitudRepository;
import pe.crediya.solicitudes.model.tipoprestamo.TipoPrestamo;
import pe.crediya.solicitudes.model.tipoprestamo.gateways.TipoPrestamoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SolicitudUseCaseTest {
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private TipoPrestamoRepository tipoPrestamoRepository;
    @Mock
    private EstadoRepository estadoRepository;

    private SolicitudUseCase solicitudUseCase;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        solicitudUseCase = new SolicitudUseCase(
                solicitudRepository,tipoPrestamoRepository,estadoRepository);
    }

    // ---------------- crear()
    @Test
    @DisplayName("crear(): OK con estado explícito (no consulta estado inicial)")
    void crear_ok_conEstadoExplicito() {
        var s = solicitudValida(null, new BigDecimal("1000"), 12, "ana@demo.com", 1L, 7L);
        var tp = tpRango(new BigDecimal("500"), new BigDecimal("5000"));
        var persistida = s.toBuilder().idSolicitud(1L).build();

        when(tipoPrestamoRepository.findById(1L)).thenReturn(Mono.just(tp));
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(Mono.just(persistida));

        StepVerifier.create(solicitudUseCase.crear(s))
                .assertNext(x -> {
                    assertEquals(1L, x.getIdSolicitud());
                    assertEquals(7L, x.getIdEstado());
                })
                .verifyComplete();

        verify(tipoPrestamoRepository).findById(1L);
        verify(solicitudRepository).save(any(Solicitud.class));
        verifyNoInteractions(estadoRepository);
        verifyNoMoreInteractions(solicitudRepository, tipoPrestamoRepository);
    }

    @Test
    @DisplayName("crear(): OK sin estado → asigna 'PENDIENTE' y persiste")
    void crear_ok_sinEstado_asignaPendiente() {
        var s = solicitudValida(null, new BigDecimal("1500"), 10, "ana@demo.com", 1L, null);
        var tp = tpRango(new BigDecimal("500"), new BigDecimal("5000"));
        var estadoPendiente = estado(10L, "PENDIENTE");

        when(tipoPrestamoRepository.findById(1L)).thenReturn(Mono.just(tp));
        when(estadoRepository.findByNombre("PENDIENTE")).thenReturn(Mono.just(estadoPendiente));
        // devolvemos lo que llegue para poder inspeccionarlo
        when(solicitudRepository.save(any(Solicitud.class)))
                .thenAnswer(inv -> Mono.just((Solicitud) inv.getArgument(0)));

        StepVerifier.create(solicitudUseCase.crear(s))
                .assertNext(x -> {
                    assertNotNull(x.getIdEstado());
                    assertEquals(10L, x.getIdEstado());
                })
                .verifyComplete();

        verify(tipoPrestamoRepository).findById(1L);
        verify(estadoRepository).findByNombre("PENDIENTE");
        ArgumentCaptor<Solicitud> cap = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(cap.capture());
        assertEquals(10L, cap.getValue().getIdEstado());
        verifyNoMoreInteractions(solicitudRepository, tipoPrestamoRepository, estadoRepository);
    }

    @Test
    @DisplayName("crear(): tipo de préstamo no existe → BusinessException")
    void crear_tipoPrestamoNoExiste() {
        var s = solicitudValida(null, new BigDecimal("1000"), 12, "ana@demo.com", 999L, null);

        when(tipoPrestamoRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.crear(s))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    var be = (BusinessException) ex;
                    assertEquals(ErrorCode.VALIDATION_ERROR, be.getCode());
                    assertTrue(be.getMessage().contains("Tipo de préstamo no existe"));
                })
                .verify();

        verify(tipoPrestamoRepository).findById(999L);
        verifyNoInteractions(estadoRepository);
        verifyNoInteractions(solicitudRepository);
    }

    @Test
    @DisplayName("crear(): monto fuera de rango del producto → BusinessException")
    void crear_montoFueraDeRango() {
        var s = solicitudValida(null, new BigDecimal("10000"), 12, "ana@demo.com", 1L, null);
        var tp = tpRango(new BigDecimal("500"), new BigDecimal("5000"));

        when(tipoPrestamoRepository.findById(1L)).thenReturn(Mono.just(tp));

        StepVerifier.create(solicitudUseCase.crear(s))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    var be = (BusinessException) ex;
                    assertEquals(ErrorCode.VALIDATION_ERROR, be.getCode());
                    assertTrue(be.getMessage().toLowerCase().contains("fuera del rango"));
                })
                .verify();

        verify(tipoPrestamoRepository).findById(1L);
        verifyNoInteractions(estadoRepository);
        verifyNoInteractions(solicitudRepository);
    }

    @Test
    @DisplayName("crear(): datos inválidos en validación básica → corta antes del repo")
    void crear_datosInvalidos_basicos() {
        // plazo inválido
        var s1 = solicitudValida(null, new BigDecimal("1000"), 0, "ana@demo.com", 1L, null);
        StepVerifier.create(solicitudUseCase.crear(s1))
                .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().toLowerCase().contains("plazo")))
                .verify();

        // email inválido
        var s2 = solicitudValida(null, new BigDecimal("1000"), 12, "ana#demo.com", 1L, null);
        StepVerifier.create(solicitudUseCase.crear(s2))
                .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().toLowerCase().contains("correo")))
                .verify();

        // monto <= 0
        var s3 = solicitudValida(null, new BigDecimal("0"), 12, "ana@demo.com", 1L, null);
        StepVerifier.create(solicitudUseCase.crear(s3))
                .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().toLowerCase().contains("monto")))
                .verify();

        verifyNoInteractions(tipoPrestamoRepository, estadoRepository, solicitudRepository);
    }

    // ---------------- obtenerPorId()
    @Test
    @DisplayName("obtenerPorId(): OK")
    void obtenerPorId_ok() {
        var s = solicitudValida(1L, new BigDecimal("1500"), 12, "ana@demo.com", 1L, 7L);
        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(s));

        StepVerifier.create(solicitudUseCase.obtenerPorId(1L))
                .expectNextMatches(x -> x.getIdSolicitud().equals(1L))
                .verifyComplete();

        verify(solicitudRepository).findById(1L);
        verifyNoMoreInteractions(solicitudRepository);
        verifyNoInteractions(tipoPrestamoRepository, estadoRepository);
    }

    @Test
    @DisplayName("obtenerPorId(): id null → BusinessException")
    void obtenerPorId_idNull() {
        StepVerifier.create(solicitudUseCase.obtenerPorId(null))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    assertTrue(ex.getMessage().toLowerCase().contains("id de solicitud"));
                })
                .verify();

        verifyNoInteractions(solicitudRepository, tipoPrestamoRepository, estadoRepository);
    }

    // ---------------- listar()
    @Test
    @DisplayName("listar(): OK")
    void listar_ok() {
        var s1 = solicitudValida(1L, new BigDecimal("1000"), 12, "a@a.com", 1L, 7L);
        var s2 = solicitudValida(2L, new BigDecimal("2000"), 24, "b@b.com", 1L, 7L);
        when(solicitudRepository.findAll()).thenReturn(Flux.just(s1, s2));

        StepVerifier.create(solicitudUseCase.listar())
                .expectNext(s1)
                .expectNext(s2)
                .verifyComplete();

        verify(solicitudRepository).findAll();
        verifyNoMoreInteractions(solicitudRepository);
        verifyNoInteractions(tipoPrestamoRepository, estadoRepository);
    }

    // ---------------- eliminar()
    @Test
    @DisplayName("eliminar(): OK")
    void eliminar_ok() {
        when(solicitudRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.eliminar(1L))
                .verifyComplete();

        verify(solicitudRepository).deleteById(1L);
        verifyNoMoreInteractions(solicitudRepository);
        verifyNoInteractions(tipoPrestamoRepository, estadoRepository);
    }

    @Test
    @DisplayName("eliminar(): id null → BusinessException")
    void eliminar_idNull() {
        StepVerifier.create(solicitudUseCase.eliminar(null))
                .expectError(BusinessException.class)
                .verify();

        verifyNoInteractions(solicitudRepository, tipoPrestamoRepository, estadoRepository);
    }

    // ---------------- cambiarEstado()
    @Test
    @DisplayName("cambiarEstado(): OK")
    void cambiarEstado_ok() {
        var s = solicitudValida(1L, new BigDecimal("1200"), 12, "ana@demo.com", 1L, 7L);
        when(estadoRepository.findById(10L)).thenReturn(Mono.just(estado(10L, "APROBADA")));
        when(solicitudRepository.findById(1L)).thenReturn(Mono.just(s));
        when(solicitudRepository.save(any(Solicitud.class)))
                .thenAnswer(inv -> Mono.just((Solicitud) inv.getArgument(0)));

        StepVerifier.create(solicitudUseCase.cambiarEstado(1L, 10L))
                .assertNext(x -> assertEquals(10L, x.getIdEstado()))
                .verifyComplete();

        verify(estadoRepository).findById(10L);
        verify(solicitudRepository).findById(1L);
        verify(solicitudRepository).save(any(Solicitud.class));
        verifyNoMoreInteractions(estadoRepository, solicitudRepository);
    }

    @Test
    @DisplayName("cambiarEstado(): estado no existe → BusinessException")
    void cambiarEstado_estadoNoExiste() {
        when(estadoRepository.findById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.cambiarEstado(1L, 10L))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    assertTrue(ex.getMessage().contains("Estado no existe"));
                })
                .verify();

        verify(estadoRepository).findById(10L);
        verifyNoInteractions(solicitudRepository);
    }

    @Test
    @DisplayName("cambiarEstado(): solicitud no existe → BusinessException")
    void cambiarEstado_solicitudNoExiste() {
        when(estadoRepository.findById(10L)).thenReturn(Mono.just(estado(10L, "APROBADA")));
        when(solicitudRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.cambiarEstado(1L, 10L))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    assertTrue(ex.getMessage().contains("Solicitud no existe"));
                })
                .verify();

        verify(estadoRepository).findById(10L);
        verify(solicitudRepository).findById(1L);
        verifyNoMoreInteractions(estadoRepository);
        verifyNoMoreInteractions(solicitudRepository);
    }

    // ---------------- listarPendientesRevision()
    @Test
    @DisplayName("listarPendientesRevision(): OK con estados válidos y paginación")
    void listarPendientesRevision_ok() {
        var det1 = solicitudDetalle(1L, "a@a.com", "PENDIENTE");
        var det2 = solicitudDetalle(2L, "b@b.com", "REVISION_MANUAL");

        when(solicitudRepository.findSolicitudesByEstados(List.of("PENDIENTE","REVISION_MANUAL")))
                .thenReturn(Flux.just(det1, det2));
        when(solicitudRepository.countByEstados(List.of("PENDIENTE", "REVISION_MANUAL")))
                .thenReturn(Mono.just(2L));

        StepVerifier.create(solicitudUseCase.listarPendientesRevision(
                        List.of("PENDIENTE", "REVISION_MANUAL"), 0, 10))
                .expectNextMatches(response -> {
                    assertEquals(2, response.getContent().size());
                    assertEquals(0, response.getPage());
                    assertEquals(10, response.getSize());
                    assertEquals(2L, response.getTotalElements());
                    assertEquals(1, response.getTotalPages());
                    return true;
                })
                .verifyComplete();

        verify(solicitudRepository).findSolicitudesByEstados(List.of("PENDIENTE", "REVISION_MANUAL"));
        verify(solicitudRepository).countByEstados(List.of("PENDIENTE", "REVISION_MANUAL"));
        verifyNoMoreInteractions(solicitudRepository);
        verifyNoInteractions(tipoPrestamoRepository, estadoRepository);
    }

    @Test
    @DisplayName("listarPendientesRevision(): estadosFiltro vacío → BusinessException")
    void listarPendientesRevision_filtroVacio() {
        StepVerifier.create(solicitudUseCase.listarPendientesRevision(List.of(), 0, 10))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    var be = (BusinessException) ex;
                    assertEquals(ErrorCode.VALIDATION_ERROR, be.getCode());
                    assertTrue(be.getMessage().toLowerCase().contains("estado"));
                })
                .verify();

        verifyNoInteractions(solicitudRepository, tipoPrestamoRepository, estadoRepository);
    }

    @Test
    @DisplayName("listarPendientesRevision(): parámetros de paginación inválidos → BusinessException")
    void listarPendientesRevision_paginacionInvalida() {
        StepVerifier.create(solicitudUseCase.listarPendientesRevision(List.of("PENDIENTE", "REVISION_MANUAL"), -1, 10))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    var be = (BusinessException) ex;
                    assertEquals(ErrorCode.VALIDATION_ERROR, be.getCode());
                    assertTrue(be.getMessage().toLowerCase().contains("parametros de paginacion invalidos"));
                })
                .verify();

        StepVerifier.create(solicitudUseCase.listarPendientesRevision(List.of("PENDIENTE", "REVISION_MANUAL"), 0, 0))
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof BusinessException);
                    var be = (BusinessException) ex;
                    assertEquals(ErrorCode.VALIDATION_ERROR, be.getCode());
                    assertTrue(be.getMessage().toLowerCase().contains("parametros de paginacion invalidos"));
                })
                .verify();

        verifyNoInteractions(solicitudRepository, tipoPrestamoRepository, estadoRepository);
    }

    // ------- helpers
    private Solicitud solicitudValida(Long id, BigDecimal monto, Integer plazo, String email,
                                      Long idTipoPrestamo, Long idEstado) {
        return Solicitud.builder()
                .idSolicitud(id)
                .monto(monto)
                .plazo(plazo)
                .email(email)
                .idTipoPrestamo(idTipoPrestamo)
                .idEstado(idEstado)
                .build();
    }

    private TipoPrestamo tpRango(BigDecimal min, BigDecimal max) {
        return TipoPrestamo.builder()
                .idTipoPrestamo(100L)
                .nombre("Personal")
                .montoMinimo(min)
                .montoMaximo(max)
                .tasaInteres(new BigDecimal("0.12"))
                .validacionAutomatica(Boolean.TRUE)
                .build();
    }

    private Estado estado(Long id, String nombre) {
        return Estado.builder().idEstado(id).nombre(nombre).descripcion("desc").build();
    }

    private SolicitudDetalle solicitudDetalle(Long id, String email, String estado) {
        return SolicitudDetalle.builder()
                .idSolicitud(id)
                .monto(new BigDecimal("1000"))
                .plazo(12)
                .email(email)
                .nombreCliente("Cliente " + id)
                .tipoPrestamo("Personal")
                .tasaInteres(new BigDecimal("0.15"))
                .estadoSolicitud(estado)
                .salarioBase(new BigDecimal("2000"))
                .montoMensualSolicitud(new BigDecimal("83.33"))
                .build();
    }


}
