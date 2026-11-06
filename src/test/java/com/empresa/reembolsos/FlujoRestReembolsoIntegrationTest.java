package com.empresa.reembolsos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integración del flujo completo vía REST: crear, validar, calcular (3 categorías),
 * cambiar estado y verificar la reacción de los 3 observadores.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class FlujoRestReembolsoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private long crear(String json, String montoEsperado) throws Exception {
        var body = mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        Number monto = JsonPath.read(body, "$.montoAprobado");
        assertEquals(0, new BigDecimal(montoEsperado).compareTo(new BigDecimal(monto.toString())));
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void cambiarEstado(long id, String estado) throws Exception {
        mvc.perform(put("/api/solicitudes/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON).content("\"" + estado + "\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(estado));
    }

    @Test
    void flujoCompletoTresCategoriasYTresObservadores(CapturedOutput salida) throws Exception {
        long viaje = crear("""
                {"empleadoId":"rest-1","categoria":"VIAJE","montoSolicitado":120000,
                 "fechaGasto":"2026-09-01","diasViaje":1,"documentosSoporte":true}""", "120000");
        long representacion = crear("""
                {"empleadoId":"rest-1","categoria":"REPRESENTACION","montoSolicitado":1500000,
                 "fechaGasto":"2026-09-02","diasViaje":0,"documentosSoporte":true}""", "1500000");
        long capacitacion = crear("""
                {"empleadoId":"rest-1","categoria":"CAPACITACION","montoSolicitado":2000000,
                 "fechaGasto":"2026-09-03","diasViaje":0,"documentosSoporte":true}""", "1600000");

        // Validación: sin documentos soporte -> 400 con mensaje claro
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"empleadoId":"rest-2","categoria":"REPRESENTACION","montoSolicitado":400000,
                         "fechaGasto":"2026-09-04","diasViaje":0,"documentosSoporte":false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Faltan documentos soporte"));

        cambiarEstado(viaje, "APROBADA");
        cambiarEstado(representacion, "RECHAZADA");
        cambiarEstado(capacitacion, "REQUIERE_CORRECCION");

        var log = salida.getOut();
        // Empleado: notificado en los tres cambios
        assertThat(log).contains(
                "Notificando a rest-1: su solicitud paso de PENDIENTE a APROBADA",
                "Notificando a rest-1: su solicitud paso de PENDIENTE a RECHAZADA",
                "Notificando a rest-1: su solicitud paso de PENDIENTE a REQUIERE_CORRECCION");
        // Auditoria: registra todo cambio sin excepcion
        assertThat(log).contains(
                "Auditoria: solicitud " + viaje + " cambio de PENDIENTE a APROBADA",
                "Auditoria: solicitud " + representacion + " cambio de PENDIENTE a RECHAZADA",
                "Auditoria: solicitud " + capacitacion + " cambio de PENDIENTE a REQUIERE_CORRECCION");
        // Contabilidad: solo actua ante APROBADA
        assertThat(log).containsPattern("Contabilidad: programar pago de 120000(\\.0+)? para rest-1");
        assertThat(log).doesNotContain("programar pago de 1500000", "programar pago de 1600000");
    }
}