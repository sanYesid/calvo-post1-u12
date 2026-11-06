package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Eslabón de validación que verifica que el monto solicitado no supere el tope de su categoría
 * y que una solicitud de viaje indique al menos un día de viaje.
 */
@Component
public class ValidadorMontoMaximo extends ValidadorSolicitud {

    private static final Map<CategoriaGasto, BigDecimal> TOPES = Map.of(
            CategoriaGasto.VIAJE, new BigDecimal("150000"),
            CategoriaGasto.REPRESENTACION, new BigDecimal("2000000"),
            CategoriaGasto.CAPACITACION, new BigDecimal("3000000"));

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        if (solicitud.getCategoria() == CategoriaGasto.VIAJE && solicitud.getDiasViaje() <= 0) {
            return ResultadoValidacion.rechazar("Una solicitud de viaje requiere al menos 1 dia de viaje");
        }
        var tope = TOPES.get(solicitud.getCategoria());
        return solicitud.getMontoSolicitado().compareTo(tope) > 0
                ? ResultadoValidacion.rechazar("Monto excede el tope de la categoria " + solicitud.getCategoria())
                : ResultadoValidacion.ok();
    }
}