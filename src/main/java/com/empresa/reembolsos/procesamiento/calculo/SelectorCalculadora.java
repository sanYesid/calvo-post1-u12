package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Componente que indexa las estrategias de cálculo inyectadas por Spring para resolver
 * la calculadora correspondiente a cada categoría de gasto sin condicionales manuales.
 */
@Component
public class SelectorCalculadora {

    private final Map<CategoriaGasto, CalculadoraReembolso> calculadoras;

    public SelectorCalculadora(List<CalculadoraReembolso> lista) {
        this.calculadoras = lista.stream()
                .collect(Collectors.toMap(CalculadoraReembolso::getCategoria, Function.identity()));
    }

    public CalculadoraReembolso obtener(CategoriaGasto categoria) {
        return Optional.ofNullable(calculadoras.get(categoria))
                .orElseThrow(() -> new IllegalArgumentException("Categoria no soportada: " + categoria));
    }
}