package com.empresa.reembolsos.procesamiento.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias puras (sin contexto de Spring) de cada eslabón de la cadena de validación.
 */
class ValidadoresTest {

    private static SolicitudReembolso solicitud(CategoriaGasto categoria, String monto,
                                                LocalDate fecha, int dias, boolean documentos) {
        return new SolicitudReembolso("emp-1", categoria, new BigDecimal(monto), fecha, dias, documentos);
    }

    @Test
    void documentosSoporteRechazaCuandoFaltanDocumentos() {
        var resultado = new ValidadorDocumentosSoporte().validar(
                solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 10), 1, false), List.of());
        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }

    @Test
    void montoMaximoRechazaCuandoExcedeTopeDeCategoria() {
        var resultado = new ValidadorMontoMaximo().validar(
                solicitud(CategoriaGasto.REPRESENTACION, "2500000", LocalDate.of(2026, 8, 10), 0, true), List.of());
        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo().contains("REPRESENTACION"));
    }

    @Test
    void montoMaximoRechazaViajeSinDias() {
        var resultado = new ValidadorMontoMaximo().validar(
                solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 10), 0, true), List.of());
        assertFalse(resultado.valida());
        assertEquals("Una solicitud de viaje requiere al menos 1 dia de viaje", resultado.motivoRechazo());
    }

    @Test
    void duplicadosRechazaMismaCategoriaEnMismoPeriodo() {
        var existente = solicitud(CategoriaGasto.REPRESENTACION, "500000", LocalDate.of(2026, 8, 5), 0, true);
        var nueva = solicitud(CategoriaGasto.REPRESENTACION, "300000", LocalDate.of(2026, 8, 20), 0, true);
        var resultado = new ValidadorDuplicados().validar(nueva, List.of(existente));
        assertFalse(resultado.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo", resultado.motivoRechazo());
    }

    @Test
    void duplicadosAceptaMismoMesDeOtroAnio() {
        var existente = solicitud(CategoriaGasto.REPRESENTACION, "500000", LocalDate.of(2025, 8, 5), 0, true);
        var nueva = solicitud(CategoriaGasto.REPRESENTACION, "300000", LocalDate.of(2026, 8, 20), 0, true);
        assertTrue(new ValidadorDuplicados().validar(nueva, List.of(existente)).valida());
    }

    @Test
    void cadenaCompletaAceptaSolicitudValida() {
        var cadena = new CadenaValidadores(
                new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(), new ValidadorDuplicados());
        var valida = solicitud(CategoriaGasto.CAPACITACION, "1000000", LocalDate.of(2026, 8, 10), 0, true);
        assertTrue(cadena.validar(valida, List.of()).valida());
    }
}