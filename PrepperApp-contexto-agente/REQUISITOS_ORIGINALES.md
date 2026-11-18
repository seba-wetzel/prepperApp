# PrepperApp: requisitos originales e historial

> Documento histórico del 17 de noviembre de 2026. Describe el punto de partida anterior a la implementación y contiene afirmaciones que ya no son vigentes. Para retomar el proyecto, empezar por `README.md` y `ESTADO_ACTUAL.md`.

Fecha original: 17 de noviembre de 2026.

## 1. Punto de partida y encargo

Seba está desarrollando una aplicación Android para gestionar recursos domésticos de preparación para emergencias. Quiere inventario, reservas de agua, estimación de autonomía, lista de compras, tareas periódicas y recordatorios de vencimientos y revisiones. La primera versión debe funcionar completamente offline, sin cuentas ni servidor.

El usuario ya instaló Android Studio y creó un proyecto en su PC:

`/home/seba/AndroidStudioProjects/PrepperApp`

No se ha inspeccionado ese proyecto. La plantilla, el identificador de paquete, las versiones de Gradle/SDK y las dependencias todavía NO están confirmados. No se ha escrito código Android en esta conversación. No recrear ni sobrescribir el proyecto antes de inspeccionarlo.

El archivo adjunto `kit-emergencia.html` es la demo original y constituye la referencia funcional y visual. Está incluido íntegro, sin modificaciones. Abrirlo para entender las pantallas y consultar su código para recuperar todos los productos, tareas, textos y cálculos. No reducir el encargo a una única pantalla de recordatorios.

El usuario espera que el agente lea y modifique archivos directamente, compile y diagnostique cuando el entorno lo permita. No quiere copiar y pegar comandos o fragmentos de código para cada paso. Comunicarse en español, de manera profesional, breve y directa. Solicitar intervención solo para acciones que realmente requieran su dispositivo o una decisión no inferible.

## 2. Estado de las decisiones

**Requisitos expresados por el usuario:** Android, funcionamiento inicialmente totalmente offline, continuidad con la demo, recordatorios de alimentos y agua, atención especial a que la app puede pasar mucho tiempo sin abrirse.

**Propuesta técnica de trabajo presentada:** Kotlin, Jetpack Compose, Room/SQLite y WorkManager. Primero comprobar recordatorios con una tarea persistente y después completar el inventario y el resto de funciones. Continuar con esta base salvo incompatibilidad concreta en el proyecto existente.

**Mejoras propuestas, aún no implementadas:** inventario por lotes, agua por recipiente o grupo, tareas configurables con historial y autonomía que diferencie existencias registradas de recursos utilizables. Son criterios de diseño razonables; no presentarlos como funciones existentes ni como decisiones detalladas ya aprobadas.

**Fuera del alcance inicial:** backend, registro de usuarios, sincronización entre dispositivos, notificaciones push, servicios de pago, integración con sensores, catálogo en línea y lectura automática de etiquetas. No agregar estas dependencias para resolver los recordatorios.

## 3. Pantallas de la demo que deben conservarse

Navegación inferior de cuatro destinos: Inicio, Despensa, Tareas y Ajustes. Interfaz en español, adaptable al ancho del teléfono, con tema claro y oscuro. Tarjetas redondeadas, fondo gris claro o azul oscuro, acento naranja y estados verde/ámbar/rojo. Mantener la jerarquía visual; usar controles nativos accesibles de Compose en lugar de incrustar la web en un WebView.

### Inicio

- Dos tarjetas principales: días estimados de comida y días estimados de agua. Cada una muestra meta en días y progreso.
- Acceso a «Para comprar», con cantidad de productos por debajo de la meta; abre Despensa con ese filtro.
- Acceso a «Tareas pendientes», con contador; abre Tareas.
- Lista «Vencen pronto», ordenada por cercanía de fecha, incluyendo vencidos. La demo muestra hasta seis entradas dentro de una ventana de 60 días.
- Aviso de copia de seguridad inexistente o antigua.
- Estado de almacenamiento local. Evitar un indicador de sincronización si no existe sincronización.

### Despensa

- Filtros: Todo, Para comprar y Vence pronto.
- Productos agrupados por categoría.
- Cada tarjeta muestra nombre, unidad, nota, cantidad disponible, cantidad objetivo, faltante y estado.
- Controles para aumentar/disminuir cantidad y editarla directamente.
- Fecha y estado de vencimiento. En la app real, mostrar el vencimiento más próximo y permitir abrir los lotes.
- Agregar producto y compartir lista de compras mediante el selector de compartir de Android.
- El formulario de la demo incluye nombre, unidad, consumo semanal para cinco personas, kcal por unidad e incremento del contador. Se puede mejorar la redacción o normalizar a consumo por persona, preservando la equivalencia de cálculo.
- En la demo solo se eliminan productos personalizados. Definir una acción reversible o confirmación para operaciones destructivas; no eliminar existencias accidentalmente al cambiar el catálogo.

**Cambio central:** un producto puede tener varios lotes. Ejemplo: atún, tres latas que vencen en marzo y cinco en septiembre. Al reponer, crear o incrementar el lote adecuado; al consumir, proponer primero el que vence antes y permitir elegir otro. No conservar una única fecha para todo el stock.

### Tareas

- Agrupación por frecuencia, contador de tareas al día y estado de cada tarea.
- Mostrar última realización, próxima revisión y atraso cuando corresponda.
- Acción «Marcar como realizada»; conservar historial y permitir deshacer un registro accidental.
- Permitir crear y editar tareas y frecuencia. Las tareas iniciales de la demo sirven como plantilla editable.
- Posponer un aviso no significa completar la tarea ni cambiar su última realización.
- La propuesta inicial calcula la próxima fecha desde la realización efectiva. Si se implementan fechas fijas, distinguir explícitamente ese modo.
- Para frecuencias mensuales/anuales, usar períodos de calendario; la demo aproxima con 30/91/182/365 días y no debe tomarse como obligación del modelo final.

### Ajustes

- Integrantes del hogar, autonomía objetivo en semanas y kcal diarias promedio por persona.
- La demo inicia con cinco personas, dos semanas y 1900 kcal; son valores de referencia, no recomendaciones personalizadas.
- Consumo de agua: la demo usa cuatro litros por persona por día. Mantener como supuesto visible y preferentemente configurable.
- Exportar copia de seguridad y restaurarla con validación y confirmación de reemplazo.
- Borrar datos con confirmación explícita.
- Agregar ajustes de avisos: permiso de notificación, anticipación, estado de restricciones detectables, acceso a configuración del sistema y prueba de notificación.

## 4. Contenido inicial de la demo

El HTML contiene 25 productos con unidades, categoría, cantidades semanales, kcal, incrementos y notas. Importar esos valores directamente de `BASE_ITEMS`, sin reescribirlos por memoria: agua; arroz; fideos; polenta; lentejas/porotos; avena; harina; legumbres en lata; atún/caballa; carne/picadillo; leche en polvo; mantequilla de maní; aceite; frutos secos; galletitas; barras de cereal; fruta en almíbar; tomate; choclo/arvejas; azúcar; dulce/mermelada; sal; caldo; yerba; café/té.

`TASKS` incluye 19 tareas distribuidas en semanal, mensual, trimestral, semestral y anual. Cubren carga de equipos, combustible, pronóstico, linternas/radio/anafe, pilas, botiquín, mochilas, rotación de alimentos, revisión de bidones, práctica familiar, renovación del agua, ropa, documentos, efectivo, gas y revisión del plan.

Estos contenidos y sus intervalos son ejemplos de la demo. No presentar sus indicaciones de alimentación, tratamiento del agua o reemplazo de equipos como asesoramiento técnico validado. Para una publicación real deberán revisarse con fuentes pertinentes. Conservar la posibilidad de editarlos o desactivarlos.

## 5. Datos y reglas de negocio propuestas

Modelo mínimo orientativo, adaptable al proyecto:

| Entidad | Datos principales |
| --- | --- |
| Producto | ID estable, nombre, categoría, unidad, kcal por unidad, consumo objetivo, incremento, notas |
| Lote | ID, producto, cantidad restante, fecha opcional, tipo de fecha, ubicación opcional, estado de disponibilidad |
| Reserva de agua | ID, nombre de recipiente/grupo, litros actuales, tipo de agua, fecha de carga, última revisión y renovación |
| Tarea | ID, título, frecuencia, próxima fecha, vínculo opcional con reserva/recurso, activa |
| Realización | ID, tarea, fecha/hora, nota opcional |
| Aviso | Identificador deduplicable, recurso/tarea, evento y anticipación, último aviso, posposición |
| Preferencias | Personas, semanas, kcal, litros diarios, preferencias de avisos y fecha de backup confirmado |

- Usar IDs estables y relaciones; no acoplar reglas a nombres traducidos.
- Evitar errores de coma flotante en cantidades: definir precisión o unidades menores.
- No aceptar cantidades negativas. Mantener coherencia entre unidades y kcal.
- Distinguir fecha desconocida, vencimiento y consumo preferente; no inventar fechas.
- Guardar fechas de calendario como fechas locales y eventos como instantes; manejar zona horaria y cambios de fecha sin restar milisegundos como si todos los días duraran lo mismo.
- El stock de un producto es la suma de sus lotes. Si el agua se gestiona por recipientes, el total de Agua debe derivarse de esas reservas para no contabilizarla dos veces.
- Un vencimiento o revisión pendiente debe verse aunque el usuario no haya permitido notificaciones.

### Cálculos de referencia

Meta de días = semanas × 7.

Objetivo de producto = cantidad semanal base × personas / 5 × semanas. La demo redondea unidades contables hacia arriba y kg/litros a décimas; documentar la política de redondeo elegida.

Días de comida = suma(cantidad utilizable × kcal por unidad) / (personas × kcal diarias).

Días de agua = litros utilizables / (personas × litros diarios).

Son estimaciones. Las kcal no representan por sí solas una dieta completa. No contar silenciosamente recursos vencidos o no disponibles como reserva utilizable; definir una regla visible según estado y tipo de fecha, sin afirmar automáticamente que cualquier consumo preferente vencido vuelve peligroso un producto.

## 6. Recordatorios offline y límites de Android

La preocupación principal del usuario es perder avisos si no abre la app durante mucho tiempo. No prometer entrega garantizada en cualquier estado del teléfono.

- Cierre normal del proceso: no requiere mantener un servicio permanente. WorkManager puede ejecutar trabajo persistente aunque la interfaz no esté abierta.
- Doze y ahorro de batería: la ejecución periódica puede retrasarse. Un chequeo diario sirve para fechas de revisión, no garantiza una hora exacta.
- Hibernación por meses sin interacción: puede impedir trabajos, alarmas y notificaciones. Ejecutar trabajo en segundo plano no cuenta por sí solo como uso.
- Solicitar permiso de notificación cuando corresponda. Ofrecer acceso a la opción del sistema para evitar restricciones por desuso, explicando su propósito. La app no puede concederse esta excepción sola.
- Detectar los estados accesibles mediante APIs públicas; no declarar que se conocen todas las restricciones específicas del fabricante.
- Verificar el comportamiento de forzar detención, reinicio y restricciones del fabricante durante desarrollo; distinguirlos de retirar la app de recientes.
- AlarmManager tampoco elimina por sí solo el problema de la hibernación. No introducir permisos de alarma exacta o servicios permanentes sin una necesidad concreta.

Implementación propuesta: trabajo periódico único sin restricción de red, lectura de fechas desde la base local, notificación agrupada y deduplicación persistente. Revisar también pendientes al abrir la app. Reconciliar avisos al editar/completar/eliminar recursos y después de restaurar un backup.

Anticipación configurable (por ejemplo, 60/30/7 días como propuesta, no requisito cerrado). Acciones: abrir el recurso, posponer y completar una revisión. Completar una revisión de agua no debe marcar automáticamente como renovada el agua ni borrar un vencimiento de alimentos.

Pantalla de estado: permiso de notificación, canal habilitado cuando sea comprobable, restricciones por desuso detectables, última comprobación ejecutada y próximo evento previsto. Una notificación de prueba inmediata solo verifica la visualización, no la entrega futura bajo suspensión.

Referencias oficiales consultadas durante la conversación; verificar la documentación vigente al implementar:

- https://developer.android.com/develop/background-work/background-tasks/persistent
- https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest
- https://developer.android.com/topic/performance/app-hibernation
- https://developer.android.com/develop/background-work/background-tasks/bg-work-restrictions
- https://developer.android.com/training/monitoring-device-state/doze-standby
- https://developer.android.com/develop/background-work/services/alarms

## 7. Arquitectura y persistencia

Kotlin + Compose para UI; Room para datos relacionales; ViewModel y StateFlow para estado de pantalla; coroutines para operaciones asíncronas; WorkManager para comprobaciones persistentes. DataStore puede utilizarse para preferencias simples si conviene. Mantener capas claras sin introducir arquitectura excesiva ni dependencias innecesarias.

Inspeccionar versiones existentes antes de elegir librerías. Usar versiones estables compatibles, no inventarlas ni reemplazar el catálogo Gradle sin motivo. Mantener lógica de fechas, cantidades y disponibilidad independiente de Compose para poder probarla.

Backup portable con versión de esquema. Validar integridad, fechas, cantidades y relaciones antes de sustituir datos; restaurar en una transacción. Actualizar «último backup» únicamente después de una exportación exitosa, no al abrir o cancelar el selector. Usar el selector de archivos del sistema sin permisos amplios de almacenamiento.

La demo exporta JSON v1 con `settings`, `stock`, `custom` y `tasks`. Es deseable poder importarlo, pero no consta que el usuario tenga datos reales allí. Si se agrega compatibilidad: convertir cada stock en un lote, preservar fechas y última realización; no inventar historial inexistente. Si se difiere, indicarlo expresamente.

## 8. Orden de implementación y criterios de aceptación

### Paso 1 — Inspección

Leer instrucciones locales del repositorio, revisar estado de Git y archivos existentes. Confirmar plantilla, paquete y configuración. Compilar la base si SDK y dependencias están disponibles. No borrar cambios del usuario ni reinstalar herramientas que ya funcionen.

### Paso 2 — Primera entrega funcional

Conservar desde el comienzo la estructura de navegación de las cuatro pantallas, aunque algunas funciones se implementen después. Crear una tarea «Revisar bidones», persistirla y mostrar próxima fecha. Implementar completar, historial mínimo, notificación local y comprobación programada, permiso y acceso a configuración pertinente. No confundir botones o pantallas de diagnóstico con funcionalidades finales.

Esta entrega comprueba el riesgo técnico principal; no sustituye el objetivo de reproducir las cuatro pantallas y completar la app.

### Paso 3 — Inventario y agua

Catálogo inicial, lotes, alta/reposición/consumo, filtros, objetivos, reservas por recipiente y tareas asociadas. Mantener consistencia tras cierres/reaperturas. Probar dos lotes del mismo producto con vencimientos diferentes.

### Paso 4 — Completar Inicio y Ajustes

Autonomía, faltantes, próximos vencimientos, tareas configurables, lista compartible, copias y restauración. Aplicar apariencia de la demo y accesibilidad: tamaños de toque adecuados, etiquetas y estados que no dependan solo del color.

### Paso 5 — Validación

- Compilar y ejecutar comprobaciones apropiadas, registrando qué se hizo y qué quedó pendiente.
- Probar cálculos, fechas límite, redondeos, completar/posponer y deduplicación con pruebas de lógica útiles.
- Comprobar funcionamiento sin red y persistencia después de cerrar/reabrir.
- Probar permisos denegados, canal deshabilitado, reinicio, Doze, restricciones por desuso y forzar detención por separado. Usar mecanismos de desarrollo para simular estados cuando sea posible; no alterar configuraciones generales del teléfono sin explicar su alcance y restaurarlas.
- Verificar que editar, consumir o borrar un lote no deja avisos obsoletos.
- Verificar backup/restauración y cancelación de exportación.
- Probar en el Samsung S25 Ultra del usuario si está conectado y autorizado para depuración; no asumir que ya lo está. No se ha probado la app en ningún dispositivo todavía.

No declarar que los avisos sobrevivirán meses de desuso solo porque funcionó una notificación inmediata.

## 9. Cómo retomar sin repetir la conversación

Leer este documento y el HTML adjunto, inspeccionar el proyecto real y comenzar la primera entrega. No pedir al usuario que vuelva a describir las pantallas. Resolver decisiones reversibles con criterio y comunicar brevemente los cambios. Si falta acceso a la carpeta o al SDK, indicar exactamente qué falta; no confundir permisos con ausencia de montaje del directorio.

El usuario quiere desarrollo directo sobre el proyecto local. Este paquete es documentación de continuidad y referencia; no contiene una implementación Android ni acredita compilaciones o pruebas realizadas.
