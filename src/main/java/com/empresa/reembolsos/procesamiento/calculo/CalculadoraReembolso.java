package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;

/**
 * Interfaz del patrón Strategy que define el contrato para calcular el monto aprobado
 * según la categoría de gasto de una solicitud de reembolso.
 */
public interface CalculadoraReembolso {

    CategoriaGasto getCategoria();

    BigDecimal calcular(SolicitudReembolso solicitud);
}