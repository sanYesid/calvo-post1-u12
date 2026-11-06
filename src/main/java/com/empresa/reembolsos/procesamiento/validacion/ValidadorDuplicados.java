package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Eslabón de validación que verifica que no exista otra solicitud pendiente
 * del mismo empleado para la misma categoría en el mismo mes.
 */
@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && p.getFechaGasto().getMonth() == solicitud.getFechaGasto().getMonth());
        return existeDuplicado
                ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}