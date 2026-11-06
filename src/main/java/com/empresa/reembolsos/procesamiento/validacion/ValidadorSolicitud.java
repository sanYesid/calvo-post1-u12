package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.util.List;

/**
 * Manejador base abstracto del patrón Chain of Responsibility para encadenar
 * verificaciones independientes sobre una solicitud de reembolso.
 */
public abstract class ValidadorSolicitud {

    private ValidadorSolicitud siguiente;

    /**
     * Enlaza el siguiente eslabón de la cadena.
     *
     * @param siguiente eslabón que se ejecuta si este aprueba la solicitud
     * @return el eslabón recibido, para permitir encadenamiento fluido
     */
    public ValidadorSolicitud siguiente(ValidadorSolicitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    /**
     * Ejecuta este eslabón y, si aprueba, delega al siguiente; corta en el primer rechazo.
     *
     * @param solicitud             solicitud a validar
     * @param pendientesDelEmpleado solicitudes pendientes del empleado, ya cargadas por el servicio
     * @return el primer rechazo encontrado o {@link ResultadoValidacion#ok()} si todos aprueban
     */
    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado) {
        ResultadoValidacion propio = validarPropio(solicitud, pendientesDelEmpleado);
        if (!propio.valida() || siguiente == null) {
            return propio;
        }
        return siguiente.validar(solicitud, pendientesDelEmpleado);
    }

    /**
     * Verificación propia del eslabón, sin conocer a los demás eslabones de la cadena.
     *
     * @param solicitud             solicitud a validar
     * @param pendientesDelEmpleado solicitudes pendientes del empleado
     * @return resultado de esta verificación
     */
    protected abstract ResultadoValidacion validarPropio(
            SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado);
}