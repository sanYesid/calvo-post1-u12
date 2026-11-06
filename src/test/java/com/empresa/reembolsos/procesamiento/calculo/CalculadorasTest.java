package com.empresa.reembolsos.procesamiento.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias puras (sin contexto de Spring) de cada estrategia de cálculo y del selector.
 */
class CalculadorasTest {

    private static SolicitudReembolso solicitud(CategoriaGasto categoria, String monto, int dias) {
        return new SolicitudReembolso("emp-1", categoria, new BigDecimal(monto), LocalDate.of(2026, 8, 10), dias, true);
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void viajeAplicaTopeDiarioPorDias() {
        assertMonto("300000", new CalculadoraViaje().calcular(solicitud(CategoriaGasto.VIAJE, "500000", 2)));
    }

    @Test
    void representacionReembolsaCompletoHastaTopeMensual() {
        var calculadora = new CalculadoraRepresentacion();
        assertMonto("1500000", calculadora.calcular(solicitud(CategoriaGasto.REPRESENTACION, "1500000", 0)));
        assertMonto("2000000", calculadora.calcular(solicitud(CategoriaGasto.REPRESENTACION, "2600000", 0)));
    }

    @Test
    void capacitacionReembolsaOchentaPorCientoHastaTopeAnual() {
        var calculadora = new CalculadoraCapacitacion();
        assertMonto("800000", calculadora.calcular(solicitud(CategoriaGasto.CAPACITACION, "1000000", 0)));
        assertMonto("3000000", calculadora.calcular(solicitud(CategoriaGasto.CAPACITACION, "5000000", 0)));
    }

    @Test
    void selectorResuelveLaEstrategiaPorCategoria() {
        var selector = new SelectorCalculadora(List.of(new CalculadoraViaje(), new CalculadoraCapacitacion()));
        assertInstanceOf(CalculadoraViaje.class, selector.obtener(CategoriaGasto.VIAJE));
        assertThrows(IllegalArgumentException.class, () -> selector.obtener(CategoriaGasto.REPRESENTACION));
    }
}