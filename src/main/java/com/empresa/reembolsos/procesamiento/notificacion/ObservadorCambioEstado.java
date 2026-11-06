package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Interfaz del patrón Observer que define el contrato de reacción para los interesados
 * ante cualquier transición de estado en una solicitud de reembolso.
 */
public interface ObservadorCambioEstado {

    /**
     * Reacciona a una transición de estado que ya fue persistida.
     *
     * @param solicitud      solicitud con su nuevo estado
     * @param estadoAnterior estado que tenía la solicitud antes del cambio
     */
    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}