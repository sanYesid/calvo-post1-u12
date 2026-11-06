package com.empresa.reembolsos.dominio;

/**
 * Estados posibles dentro del ciclo de vida de una solicitud de reembolso.
 */
public enum EstadoSolicitud {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    REQUIERE_CORRECCION
}