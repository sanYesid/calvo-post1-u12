package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Interfaz del patrón Observer que define el contrato de reacción para los interesados
 * ante cualquier transición de estado en una solicitud de reembolso.
 */
public interface ObservadorCambioEstado {

    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}