package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Ensambla y expone la cadena de validación de solicitudes en orden de menor a mayor costo computacional.
 */
@Component
public class CadenaValidadores {

    private final ValidadorSolicitud primero;

    public CadenaValidadores(ValidadorDocumentosSoporte documentos,
                             ValidadorMontoMaximo monto,
                             ValidadorDuplicados duplicados) {
        documentos.siguiente(monto).siguiente(duplicados);
        this.primero = documentos;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        return primero.validar(solicitud, pendientes);
    }
}