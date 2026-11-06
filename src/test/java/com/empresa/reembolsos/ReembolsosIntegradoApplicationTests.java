package com.empresa.reembolsos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de integración del flujo completo a nivel de servicio de aplicación.
 */
@SpringBootTest
class ReembolsosIntegradoApplicationTests {

    @Autowired
    private SolicitudReembolsoService servicio;

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
        assertEquals(0, new BigDecimal("1600000").compareTo(creadaCap.getMontoAprobado()));

        // 3. Rechazar solicitud sin documentos soporte
        var invalida = new SolicitudReembolso(
                "emp-int-2", CategoriaGasto.REPRESENTACION, new BigDecimal("400000"),
                LocalDate.of(2026, 8, 18), 0, false);
        var ex = assertThrows(SolicitudInvalidaException.class, () -> servicio.crear(invalida));
        assertEquals("Faltan documentos soporte", ex.getMessage());
    }
}