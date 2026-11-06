package com.empresa.reembolsos.procesamiento.validacion;

/**
 * Representa el resultado inmutable de una etapa de validación sobre una solicitud de reembolso.
 *
 * @param valida        indica si la solicitud superó la verificación
 * @param motivoRechazo descripción de la causa del rechazo cuando {@code valida} es {@code false}
 */
public record ResultadoValidacion(boolean valida, String motivoRechazo) {

    public static ResultadoValidacion ok() {
        return new ResultadoValidacion(true, null);
    }

    public static ResultadoValidacion rechazar(String motivo) {
        return new ResultadoValidacion(false, motivo);
    }
}