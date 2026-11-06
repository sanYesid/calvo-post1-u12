package com.empresa.reembolsos.aplicacion;

/**
 * Excepción de negocio lanzada cuando una solicitud de reembolso no supera la cadena de validación.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String message) {
        super(message);
    }
}