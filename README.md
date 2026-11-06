# Sistema de Solicitudes de Reembolso de Gastos Corporativos

**Post-contenido — Proyecto Integrador (Unidad 12 · Patrones de Diseño de Software)**

Sistema backend construido con **Java 17** y **Spring Boot 3** para gestionar el registro, la validación en múltiples etapas, el cálculo de montos aprobados según la categoría del gasto y la notificación simultánea a los interesados ante cambios de estado en solicitudes de reembolso corporativo.

## Arquitectura y Estructura de Paquetes (Parte 1)

El proyecto organiza sus responsabilidades en cinco paquetes con límites explícitos:

```text
src/main/java/com/empresa/reembolsos/
├── ReembolsosIntegradoApplication.java
├── dominio/
│   ├── SolicitudReembolso.java
│   ├── CategoriaGasto.java
│   └── EstadoSolicitud.java
├── procesamiento/
│   ├── validacion/
│   │   ├── ResultadoValidacion.java
│   │   ├── ValidadorSolicitud.java
│   │   ├── ValidadorDocumentosSoporte.java
│   │   ├── ValidadorMontoMaximo.java
│   │   ├── ValidadorDuplicados.java
│   │   └── CadenaValidadores.java
│   ├── calculo/
│   │   ├── CalculadoraReembolso.java
│   │   ├── CalculadoraViaje.java
│   │   ├── CalculadoraRepresentacion.java
│   │   ├── CalculadoraCapacitacion.java
│   │   └── SelectorCalculadora.java
│   └── notificacion/
│       ├── ObservadorCambioEstado.java
│       ├── NotificadorEmpleado.java
│       ├── NotificadorContabilidad.java
│       └── NotificadorAuditoria.java
├── aplicacion/
│   ├── SolicitudReembolsoService.java
│   └── SolicitudInvalidaException.java
├── infraestructura/
│   └── RepositorioSolicitudesJpa.java
└── adaptadores/
    └── rest/
        └── SolicitudReembolsoController.java
```

* **`dominio/`**: Entidad central (`SolicitudReembolso`) y enumeraciones (`CategoriaGasto`, `EstadoSolicitud`), completamente libres de dependencias de Spring y de JPA.
* **`procesamiento/`**: Contiene los tres patrones de diseño de comportamiento (`Chain of Responsibility` en `validacion`, `Strategy` en `calculo` y `Observer` en `notificacion`), desacoplados de la capa web y del acceso a base de datos.
* **`aplicacion/`**: Servicio coordinador (`SolicitudReembolsoService`) que orquesta la cadena de validación, la selección de la calculadora, el repositorio de persistencia y la notificación a los observadores.
* **`infraestructura/`**: Acceso a datos mediante Spring Data JPA (`RepositorioSolicitudesJpa`).
* **`adaptadores/rest/`**: Controlador HTTP (`SolicitudReembolsoController`) que expone los endpoints REST y delega toda la lógica al servicio de aplicación.

## Diseño Propuesto

Antes de escribir el código de los patrones, se analizan los tres requisitos funcionales del sistema identificando su punto de variabilidad concreto, el patrón de diseño GoF que lo resuelve y su justificación aplicando el test de explicabilidad en una oración:

| Requisito | Punto de variabilidad concreto | Patrón de diseño propuesto | Justificación (Test de explicabilidad en una oración) |
| :--- | :--- | :--- | :--- |
| **RF1 — Validación en varias etapas** | La secuencia y cantidad de reglas de verificación independientes (tope por categoría, documentos soporte, duplicados en el período y futuras reglas como el límite anual acumulado) que pueden rechazar prematuramente una solicitud antes de aprobarse. | **Chain of Responsibility** | Se elige **Chain of Responsibility** (y no un condicional monolítico ni una lista rígida) porque permite encadenar verificaciones desacopladas con cortocircuito inmediato al primer rechazo e incorporar nuevas reglas de validación en el futuro sin modificar los validadores existentes ni el servicio cliente. |
| **RF2 — Cálculo distinto por categoría de gasto** | El algoritmo matemático de cálculo del monto aprobado según la categoría del gasto (`VIAJE` por tope diario multiplicado por días, `REPRESENTACION` por tope mensual, `CAPACITACION` al 80% hasta tope anual, y futuras categorías como bienestar). | **Strategy** | Se elige **Strategy** (y no un `switch`/`if-else` en el servicio ni herencia sobre la entidad `SolicitudReembolso`) porque encapsula cada fórmula de reembolso en una clase independiente bajo una interfaz común, permitiendo seleccionar el algoritmo según la categoría y agregar nuevas categorías sin modificar las calculadoras existentes. |
| **RF3 — Notificación simultánea a varios interesados** | El conjunto de módulos suscriptores (empleado, contabilidad, auditoría interna y futuros interesados como el panel de métricas de Finanzas) y la reacción específica que cada uno ejecuta cuando una solicitud cambia de estado. | **Observer** | Se elige **Observer** (y no llamadas directas acopladas dentro del servicio de aplicación) porque permite que múltiples interesados reaccionen simultáneamente y con su propia lógica de filtrado ante un cambio de estado sin que `SolicitudReembolsoService` conozca cuántos son ni cómo implementa cada uno su notificación. |

## Decisiones de Diseño

### Decisión de Diseño 1: Orden de encadenamiento en `CadenaValidadores`

* **Orden elegido:** `ValidadorDocumentosSoporte` -> `ValidadorMontoMaximo` -> `ValidadorDuplicados`.
* **Justificación por criterio de eficiencia (Fail-Fast):** Los eslabones se ordenan de menor a mayor costo computacional. `ValidadorDocumentosSoporte` evalúa un único booleano en memoria (`O(1)`), `ValidadorMontoMaximo` realiza una búsqueda hash en un mapa estático en memoria y una comparación de `BigDecimal` (`O(1)`), mientras que `ValidadorDuplicados` es el eslabón más costoso porque debe recorrer mediante un `Stream` la colección de solicitudes pendientes del empleado (`O(n)`) comparando categorías y meses de fechas. Por lo tanto, **conviene ubicar a `ValidadorDuplicados` de último en la cadena**.
* **Qué se gana y qué se pierde con cada orden:**
  * **Ubicando el más caro al final (orden elegido):** Se gana eficiencia por cortocircuito (*short-circuit evaluation*), evitando iterar innecesariamente sobre la lista de solicitudes históricas cuando una solicitud ya es inválida por un motivo trivial (como olvidar adjuntar soportes o exceder el tope). Lo único que se "pierde" es que, si una solicitud es simultáneamente un duplicado y además carece de soportes, el usuario recibirá primero el mensaje de error sobre los documentos soporte y solo descubrirá que estaba duplicada si intenta reenviarla con soportes.
  * **Ubicando el más caro al principio (`duplicados` primero):** Se ganaría detectar de inmediato reenvíos accidentales (doble clic del empleado) informando primero la duplicidad antes de revisar los campos de la solicitud, pero se perdería eficiencia al ejecutar siempre el recorrido `O(n)` sobre la lista de pendientes incluso para solicitudes incompletas o con montos fuera de rango.

### Decisión de Diseño 2: Mecanismo de selección en `SelectorCalculadora` (Mapa inyectado por Spring vs. `switch` manual)

* **Mecanismo elegido:** Construcción de un `Map<CategoriaGasto, CalculadoraReembolso>` a partir de la `List<CalculadoraReembolso>` que Spring inyecta automáticamente con todos los `@Component` que implementan la interfaz.
* **Justificación frente al RF2:** El requisito **RF2** establece explícitamente que el área de Finanzas planea agregar nuevas categorías de gasto en los próximos semestres (por ejemplo, `"bienestar"`) con su propia fórmula **sin modificar el código de cálculo ya existente** (Principio Abierto/Cerrado — OCP).
* **¿Cuál de las dos opciones cumple ese requisito sin tocar una sola línea ya escrita?** Únicamente el **mapa construido a partir de la lista inyectada por Spring** cumple esta condición: cuando se agregue una nueva categoría al `enum CategoriaGasto` y se cree su clase `CalculadoraBienestar implements CalculadoraReembolso` anotada con `@Component`, Spring la detectará automáticamente y la incluirá en el mapa de `SelectorCalculadora` sin tocar ni una sola línea de `SelectorCalculadora`, de `SolicitudReembolsoService` ni de las calculadoras existentes. En cambio, un `switch` manual sobre `CategoriaGasto` obligaría a abrir y modificar el método de selección cada vez que nazca una categoría nueva, violando directamente el requisito RF2.