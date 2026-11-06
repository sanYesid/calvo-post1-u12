package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.util.List;

/**
 * Manejador base abstracto del patrón Chain of Responsibility para encadenar
 * verificaciones independientes sobre una solicitud de reembolso.
 */
public abstract class ValidadorSolicitud {

    private ValidadorSolicitud siguiente;

    public ValidadorSolicitud siguiente(ValidadorSolicitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado) {
        ResultadoValidacion propio = validarPropio(solicitud, pendientesDelEmpleado);
        if (!propio.valida() || siguiente == null) {
            return propio;
        }
        return siguiente.validar(solicitud, pendientesDelEmpleado);
    }

    protected abstract ResultadoValidacion validarPropio(
            SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado);
}