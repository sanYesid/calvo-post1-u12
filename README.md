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

### Decisión de Diseño 3: Arquitectura en capas simples vs. Hexagonal en persistencia y notificación (ADR-001)

* **Decisión sobre Persistencia:** `SolicitudReembolsoService` mantiene su dependencia directa hacia `RepositorioSolicitudesJpa` (arquitectura en capas simples) en lugar de introducir una interfaz de puerto `RepositorioSolicitudes` con un adaptador JPA. Dado que hoy no existe ningún plan de cambiar el motor de base de datos, crear un puerto intermedio y mappers adicionales sería una abstracción especulativa que vulnera el principio **YAGNI**.
* **Decisión sobre Notificación:** No se requiere ninguna capa ni interfaz adicional en `procesamiento.notificacion` para soportar el anuncio de Contabilidad de integrar un sistema externo el próximo trimestre. La interfaz `ObservadorCambioEstado` (patrón **Observer**) ya actúa como el *seam* (punto de desacoplamiento) necesario: `NotificadorContabilidad` podrá reemplazar su `System.out.println` por una llamada a la API externa de pagos (y Finanzas podrá agregar un `NotificadorPanelMetricas`) sin tocar una sola línea de `SolicitudReembolsoService` ni de los otros notificadores.
* **Referencia completa:** Ver el documento arquitectónico detallado en [`docs/adr/ADR-001.md`](docs/adr/ADR-001.md).

### Decisión de Diseño 4: Alcance de la Regla 1 de ArchUnit sobre `org.springframework` en el paquete `procesamiento`

* **Decisión:** La regla `procesamientoSinPersistenciaNiWeb` prohíbe explícitamente que el paquete `..procesamiento..` dependa de `..infraestructura..`, `..adaptadores..`, `org.springframework.data..` y `jakarta.persistence..`, pero permite el uso de anotaciones de estereotipo e inyección de Spring (`org.springframework.stereotype.Component`).
* **Justificación técnica:** Es fundamental distinguir entre **acoplamiento a mecanismos de entrega/persistencia** y **metadatos declarativos del contenedor de inversión de control (IoC)**:
  1. **Por qué son aceptables las anotaciones de estereotipo (`@Component`):** `@Component` es únicamente un metadato pasivo de cableado (*wiring*) que le permite a Spring descubrir y registrar automáticamente los eslabones de `CadenaValidadores`, las estrategias en `SelectorCalculadora` (`List<CalculadoraReembolso>`) y los suscriptores en `List<ObservadorCambioEstado>` sin necesidad de escribir clases de configuración manual repetitivas. Además, no impide instanciar las clases con `new` en pruebas unitarias puras (como se demuestra en los 6 tests unitarios de validadores y calculadoras).
  2. **Por qué deben prohibirse las clases de acceso a datos y capa web (`org.springframework.data..`, `jakarta.persistence..`, `..infraestructura..`, `..adaptadores..`):** Permitir que un validador, calculadora o notificador importe repositorios JPA, `EntityManager` o clases HTTP acoplaría las reglas de negocio a una base de datos o protocolo de transporte específico, rompiendo la separación de responsabilidades y haciendo imposible probar la lógica de negocio en memoria sin levantar infraestructura. Prohibir todo `org.springframework..` de forma indiscriminada obligaría a eliminar `@Component` y crear fábricas/configuraciones manuales sin ganar aislamiento real de negocio.

  ## Qué se descartó y por qué (Análisis de Impacto y Prevención de Sobreingeniería)

Durante el diseño y la implementación de las Partes 1 y 2 se evaluaron alternativas más elaboradas que finalmente se descartaron aplicando los principios **YAGNI**, **KISS**, **Rule of Three** y el **test de explicabilidad**:

### 1. Spring `ApplicationEventPublisher` en vez de una lista de observadores inyectada (Referencia del proyecto)
* **Qué se consideró:** Publicar un evento de dominio (por ejemplo, `SolicitudEstadoCambiadoEvent`) mediante `ApplicationEventPublisher` y anotar los suscriptores con `@EventListener`, en lugar de inyectar directamente `List<ObservadorCambioEstado>` en `SolicitudReembolsoService`.
* **Por qué se descartó:** Con tres observadores conocidos en tiempo de compilación dentro del mismo monolito (`NotificadorEmpleado`, `NotificadorContabilidad` y `NotificadorAuditoria`), introducir un bus de eventos genérico añade una capa de indirección que no aporta desacoplamiento adicional al que ya provee la interfaz `ObservadorCambioEstado` (**KISS** / **test de explicabilidad**), y en cambio dificulta rastrear estáticamente en el IDE el flujo de ejecución y el orden de reacción durante la depuración.
* **Qué señal futura haría reconsiderar la decisión:** Si el sistema requiere procesamiento asíncrono transaccional (`@TransactionalEventListener(phase = AFTER_COMMIT)`) para que un fallo en la API externa de Contabilidad no revierta el cambio de estado en la base de datos, o si múltiples módulos independientes del sistema empiezan a emitir y escuchar eventos de dominio cruzados.

### 2. Patrón `Visitor` o `Abstract Factory` para exportar reportes en varios formatos (Referencia del proyecto)
* **Qué se consideró:** Anticipar que el área de Contabilidad o Auditoría pediría exportar el historial trimestral de solicitudes en PDF y Excel además de CSV, diseñando desde ahora una jerarquía de exportadores con `Abstract Factory` o `Visitor`.
* **Por qué se descartó:** Hoy no existe un segundo ni tercer formato de exportación requerido en la especificación; construir una familia de abstracciones de exportación para un único formato viola directamente la **Rule of Three** y el principio **YAGNI**, resolviendo un problema hipotético que aún no existe.
* **Qué señal futura haría reconsiderar la decisión:** Cuando exista un requerimiento formal y confirmado de al menos dos o tres formatos distintos de exportación con estructuras de documento incompatibles entre sí (cumpliendo la **Rule of Three**).

### 3. Jerarquía de excepciones específicas por cada validador en lugar de `SolicitudInvalidaException` (Propuesta propia 1)
* **Qué se consideró:** Crear una jerarquía de excepciones de dominio con una subclase por cada eslabón de la cadena (`DocumentosSoporteFaltantesException`, `MontoMaximoExcedidoException`, `SolicitudDuplicadaException` extendiendo de `SolicitudInvalidaException`) en lugar de representar el resultado con el `record ResultadoValidacion(boolean valida, String motivoRechazo)` y lanzar una única `SolicitudInvalidaException` con el motivo como texto.
* **Por qué se descartó:** Actualmente el controlador REST y el servicio de aplicación tratan cualquier rechazo de validación exactamente de la misma manera: abortar la creación y devolver el mensaje explicativo al solicitante (`HTTP 400 Bad Request`). Crear tres o más clases de excepción adicionales cuando ningún consumidor ejecuta un `catch` diferenciado por tipo viola **YAGNI** y **KISS**, multiplicando el número de clases cada vez que Finanzas agregue un nuevo validador sin aportar comportamiento distinto.
* **Qué señal futura haría reconsiderar la decisión:** Si el cliente REST o un flujo automatizado necesitara reaccionar programáticamente de forma distinta según el tipo de fallo (por ejemplo, devolver `HTTP 409 Conflict` con el ID de la solicitud previa cuando falla `ValidadorDuplicados`, `HTTP 422 Unprocessable Entity` cuando excede el tope con opción de escalamiento a gerencia, y `HTTP 400` cuando faltan adjuntos).

### 4. Patrón `State` para modelar las transiciones de `EstadoSolicitud` (Propuesta propia 2)
* **Qué se consideró:** Reemplazar el `enum EstadoSolicitud` (`PENDIENTE`, `APROBADA`, `RECHAZADA`, `REQUIERE_CORRECCION`) por el patrón **State**, creando una interfaz `EstadoSolicitudState` y cuatro clases concretas (`EstadoPendiente`, `EstadoAprobada`, `EstadoRechazada`, `EstadoRequiereCorreccion`) para controlar qué transiciones están permitidas y qué comportamiento ejecuta la solicitud en cada estado.
* **Por qué se descartó:** En los requisitos actuales del sistema, los estados actúan como etiquetas de ciclo de vida cuya transición dispara notificaciones a los observadores, sin que exista todavía una matriz compleja de reglas de transición ni métodos polimórficos propios de cada estado dentro de `SolicitudReembolso`. Aplicar el patrón **State** hoy no supera el **test de explicabilidad** y vulnera **KISS**, añadiendo cuatro clases extra para un comportamiento que un `enum` resuelve con total claridad.
* **Qué señal futura haría reconsiderar la decisión:** Si Finanzas define reglas estrictas de máquina de estados donde cada estado tenga comportamientos exclusivos y transiciones prohibidas (por ejemplo, que una solicitud `RECHAZADA` sea terminal e inmutable, que `REQUIERE_CORRECCION` permita recalcular el monto y revalidar la cadena, y que `APROBADA` bloquee ediciones pero habilite reversión contable).