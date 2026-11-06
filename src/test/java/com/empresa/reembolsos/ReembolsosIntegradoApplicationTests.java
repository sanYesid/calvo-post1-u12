package com.empresa.reembolsos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.procesamiento.calculo.CalculadoraCapacitacion;
import com.empresa.reembolsos.procesamiento.calculo.CalculadoraRepresentacion;
import com.empresa.reembolsos.procesamiento.calculo.CalculadoraViaje;
import com.empresa.reembolsos.procesamiento.validacion.ValidadorDocumentosSoporte;
import com.empresa.reembolsos.procesamiento.validacion.ValidadorDuplicados;
import com.empresa.reembolsos.procesamiento.validacion.ValidadorMontoMaximo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReembolsosIntegradoApplicationTests {

    @Autowired
    private SolicitudReembolsoService servicio;

    // --- 3 Pruebas unitarias de Validadores (una por cada validador) ---

    @Test
    void validadorDocumentosSoporteRechazaSinDocumentos() {
        var validador = new ValidadorDocumentosSoporte();
        var solicitud = new SolicitudReembolso(
                "emp-1", CategoriaGasto.VIAJE, new BigDecimal("100000"),
                LocalDate.of(2026, 8, 10), 2, false);

        var resultado = validador.validar(solicitud, List.of());
        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }

    @Test
    void validadorMontoMaximoRechazaCuandoExcedeTope() {
        var validador = new ValidadorMontoMaximo();
        var solicitud = new SolicitudReembolso(
                "emp-1", CategoriaGasto.VIAJE, new BigDecimal("200000"),
                LocalDate.of(2026, 8, 10), 2, true);

        var resultado = validador.validar(solicitud, List.of());
        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo().contains("Monto excede el tope"));
    }

    @Test
    void validadorDuplicadosRechazaMismaCategoriaYMes() {
        var validador = new ValidadorDuplicados();
        var existente = new SolicitudReembolso(
                "emp-1", CategoriaGasto.REPRESENTACION, new BigDecimal("500000"),
                LocalDate.of(2026, 8, 5), 0, true);
        var nueva = new SolicitudReembolso(
                "emp-1", CategoriaGasto.REPRESENTACION, new BigDecimal("300000"),
                LocalDate.of(2026, 8, 20), 0, true);

        var resultado = validador.validar(nueva, List.of(existente));
        assertFalse(resultado.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo", resultado.motivoRechazo());
    }

    // --- 3 Pruebas unitarias de Calculadoras (una por cada calculadora) ---

    @Test
    void calculadoraViajeAplicaTopeDiarioPorDias() {
        var calculadora = new CalculadoraViaje();
        var solicitud = new SolicitudReembolso(
                "emp-1", CategoriaGasto.VIAJE, new BigDecimal("500000"),
                LocalDate.of(2026, 8, 10), 2, true);

        var aprobado = calculadora.calcular(solicitud);
        assertEquals(0, new BigDecimal("300000").compareTo(aprobado));
    }

    @Test
    void calculadoraRepresentacionAplicaTopeMensual() {
        var calculadora = new CalculadoraRepresentacion();
        var solicitud = new SolicitudReembolso(
                "emp-1", CategoriaGasto.REPRESENTACION, new BigDecimal("1500000"),
                LocalDate.of(2026, 8, 10), 0, true);

        var aprobado = calculadora.calcular(solicitud);
        assertEquals(0, new BigDecimal("1500000").compareTo(aprobado));
    }

    @Test
    void calculadoraCapacitacionAplicaOchentaPorCientoHastaTope() {
        var calculadora = new CalculadoraCapacitacion();
        var solicitud = new SolicitudReembolso(
                "emp-1", CategoriaGasto.CAPACITACION, new BigDecimal("1000000"),
                LocalDate.of(2026, 8, 10), 0, true);

        var aprobado = calculadora.calcular(solicitud);
        assertEquals(0, new BigDecimal("800000.0").compareTo(aprobado));
    }

    // --- 1 Prueba de integración @SpringBootTest del flujo completo ---

    @Test
    void flujoCompletoCreacionCalculoCambioEstadoYRechazo() {
        // 1. Crear y aprobar solicitud de VIAJE
        var viaje = new SolicitudReembolso(
                "emp-int-1", CategoriaGasto.VIAJE, new BigDecimal("120000"),
                LocalDate.of(2026, 8, 12), 1, true);
        var creadaViaje = servicio.crear(viaje);
        assertNotNull(creadaViaje.getId());
        assertEquals(EstadoSolicitud.PENDIENTE, creadaViaje.getEstado());
        assertEquals(0, new BigDecimal("120000").compareTo(creadaViaje.getMontoAprobado()));

        var aprobada = servicio.cambiarEstado(creadaViaje.getId(), EstadoSolicitud.APROBADA);
        assertEquals(EstadoSolicitud.APROBADA, aprobada.getEstado());

        // 2. Crear solicitud de CAPACITACION (verifica el 80%)
        var capacitacion = new SolicitudReembolso(
                "emp-int-1", CategoriaGasto.CAPACITACION, new BigDecimal("2000000"),
                LocalDate.of(2026, 8, 15), 0, true);
        var creadaCap = servicio.crear(capacitacion);
        assertEquals(0, new BigDecimal("1600000.0").compareTo(creadaCap.getMontoAprobado()));

        // 3. Rechazar solicitud sin documentos soporte
        var invalida = new SolicitudReembolso(
                "emp-int-2", CategoriaGasto.REPRESENTACION, new BigDecimal("400000"),
                LocalDate.of(2026, 8, 18), 0, false);
        var ex = assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(invalida));
        assertEquals("Faltan documentos soporte", ex.getMessage());
    }
}