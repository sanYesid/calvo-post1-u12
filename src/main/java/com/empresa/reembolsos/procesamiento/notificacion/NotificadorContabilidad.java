package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

/**
 * Observador concreto para el área de Contabilidad: actúa únicamente cuando el nuevo estado es APROBADA
 * para programar el pago del monto aprobado.
 */
@Component
public class NotificadorContabilidad implements ObservadorCambioEstado {

    @Override
    public void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior) {
        if (solicitud.getEstado() == EstadoSolicitud.APROBADA) {
            System.out.println("Contabilidad: programar pago de " + solicitud.getMontoAprobado()
                    + " para " + solicitud.getEmpleadoId());
        }
    }
}