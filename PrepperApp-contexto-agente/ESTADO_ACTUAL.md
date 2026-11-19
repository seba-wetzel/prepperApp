# Estado actual de la aplicación

Fecha de corte: 18 de noviembre de 2026. Baseline: commit `780ed8e`.

## Funcionalidad implementada

### Primer inicio

- La app muestra un onboarding antes de activar tareas y avisos.
- Permite configurar integrantes del hogar, semanas objetivo, kcal diarias y litros de agua diarios.
- Hasta finalizar esta configuración, `settings.configured` es falso, no hay tareas pendientes visibles y se cancelan los trabajos de revisión.
- Al finalizar, la fecha de inicio del cronograma se fija al día actual. Esto evita las 19 alertas inmediatas que aparecían apenas se instalaba la app.

### Inicio

- Autonomía estimada de comida y agua.
- Cantidad de productos por debajo del objetivo.
- Cantidad de tareas pendientes.
- Lotes vencidos o próximos a vencer dentro de 60 días.
- Estado básico de la última copia de seguridad.
- Acceso destacado a números de emergencia sin conexión.

### Emergencias

- Números incluidos: 911 emergencias generales, 100 Bomberos, 103 Defensa Civil y 107 emergencias médicas.
- Se aclara que la disponibilidad puede variar por localidad.
- La app usa `Intent.ACTION_DIAL`: abre el marcador con el número precargado, no inicia la llamada y no requiere permiso de llamadas.

### Despensa

- Catálogo inicial de 25 productos, configurable desde Ajustes mediante casillas individuales.
- Los productos ocultos conservan sus existencias, pero no aparecen en Despensa, Para comprar, autonomía, próximos vencimientos ni avisos.
- Alta de productos personalizados desde la misma sección de Ajustes.
- Filtros Todo, Para comprar y Vence pronto.
- Objetivos calculados según hogar y semanas.
- Acciones explícitas por producto: Agregar, Consumir y Ver existencias; ya no se crea stock sin contexto mediante botones `+ / −`.
- Cada carga crea una existencia separada con cantidad, fecha opcional, tipo de fecha, ubicación y código estable de dos letras más secuencia (`LP01`, `LA01`, etc.).
- El código se informa al guardar y se muestra al consultar o consumir para poder marcar físicamente cada envase.
- La fecha acepta `DD/MM/AA` o `DD/MM/AAAA`, se autoformatea con teclado numérico y también ofrece calendario localizado en español argentino.
- Para Agua se propone una rotación a tres meses, el extremo conservador del intervalo de 3–6 meses indicado por el material del curso.
- Al consumir se muestran todas las existencias, se sugiere la utilizable con vencimiento o rotación más próxima y el usuario puede elegir exactamente de cuál descontar.
- Eliminación de existencias y de productos personalizados.
- Lista de compras compartible mediante el selector nativo de Android.

### Tareas

- 19 tareas iniciales editables, agrupadas por frecuencia.
- Frecuencias semanales, mensuales, trimestrales, semestrales y anuales usando períodos de calendario.
- Alta, edición, activación y desactivación.
- Registro de realización e historial persistente.
- Deshacer la última realización.
- El vencimiento se calcula desde la realización más reciente o desde el inicio del cronograma del onboarding.

### Ajustes y copias

- Edición de integrantes, semanas objetivo, kcal y agua diaria.
- Estado del permiso y canal de notificaciones.
- Solicitud de permiso, acceso a ajustes del sistema y aviso de prueba.
- Exportación JSON mediante Storage Access Framework.
- Restauración con confirmación.
- Restablecimiento de datos con confirmación.
- La fecha de backup se actualiza únicamente después de guardar con éxito.

### Avisos

- `ReviewWorker` ejecuta una revisión diaria con WorkManager.
- También se revisa al abrir/configurar la app.
- Resume tareas pendientes y lotes próximos o vencidos en una notificación.
- El trabajo y la notificación se cancelan cuando la app no está configurada.
- La notificación abre la sección Tareas.

## Diseño aplicado

La interfaz dejó la estética genérica azul/naranja y adoptó la identidad del curso:

- rojo de emergencia, negro tinta y fondo papel crema;
- titulares pesados y etiquetas monoespaciadas;
- bordes marcados, geometría editorial y radios contenidos;
- iconos Material en la navegación;
- tarjeta roja de emergencias como acción primaria.

Los detalles están en `PRODUCTO_Y_DISENO.md`.

## Validación realizada

En el hito `780ed8e` se verificó:

- `:app:assembleDebug`;
- `:app:testDebugUnitTest`, 8 pruebas aprobadas;
- `:app:lintDebug`;
- instalación y ejecución en `emulator-5554`;
- visualización de Inicio y del diálogo de emergencias;
- apertura del marcador con `100` precargado;
- ausencia de excepciones `AndroidRuntime` durante esa prueba.

## Límites y deuda conocida

- La sección Recursos todavía no existe.
- El agua se modela como producto/lotes; aún no hay recipientes de agua como entidad especializada.
- La persistencia usa `SQLiteOpenHelper`, no Room.
- No existe sincronización entre dispositivos ni backend.
- Los avisos son una revisión agrupada diaria; no hay anticipaciones configurables ni acciones de posponer desde la notificación.
- No hay pruebas instrumentadas de flujos Compose ni pruebas automáticas de backup/restauración.
- Falta validar hibernación, Doze, reinicio, canal deshabilitado y detención forzada en un dispositivo físico.
- Las presentaciones y PDF son fuentes de diseño/contenido, no recursos empaquetados en la aplicación.
