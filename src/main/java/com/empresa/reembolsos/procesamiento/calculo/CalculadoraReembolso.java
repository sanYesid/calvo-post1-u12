package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.math.BigDecimal;

/**
 * Interfaz del patrón Strategy que define el contrato para calcular el monto aprobado
 * según la categoría de gasto de una solicitud de reembolso.
 */
public interface CalculadoraReembolso {

    /**
     * Categoría de gasto que atiende esta estrategia; {@link SelectorCalculadora} la usa como clave del mapa.
     *
     * @return la categoría soportada por esta calculadora
     */
    CategoriaGasto getCategoria();

    /**
     * Calcula el monto aprobado aplicando la fórmula propia de la categoría.
     *
     * @param solicitud solicitud que ya superó la cadena de validación
     * @return monto aprobado, nunca mayor que el monto solicitado
     */
    BigDecimal calcular(SolicitudReembolso solicitud);
}