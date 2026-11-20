# Prepper App

Prepper App ayuda a organizar las reservas de un hogar. Permite saber cuánta agua y comida hay, qué falta comprar y qué envase conviene usar primero. Funciona sin conexión y sin cuenta. Es una app nativa de Android escrita en Kotlin con Jetpack Compose y Material 3.

[Descargar el APK preliminar de prueba](https://github.com/seba-wetzel/prepperApp/releases/tag/v1.0.0-preview.1) · [Ver Inicio](publicacion/play/01-inicio.png) · [Ver Despensa](publicacion/play/02-despensa.png)

## Cómo se usa

En el primer inicio elegís cuántas personas viven en casa y cuántas semanas querés cubrir. **Inicio** muestra la autonomía estimada, los faltantes y los próximos vencimientos. También da acceso a números de emergencia de Argentina. **Despensa** permite cargar y consumir existencias, consultar sus fechas y compartir la lista de compras. En **Tareas** registrás las revisiones periódicas. Desde **Ajustes** podés adaptar el catálogo de alimentos, cambiar las metas y exportar o restaurar una copia.

## Qué ocurre al guardar un lote

Supongamos que ya tenés una lata de atún y agregás otras dos con distinto vencimiento. La app conserva las dos cargas por separado aunque las sume en la misma tarjeta de producto.

1. El formulario envía la cantidad, la fecha opcional, el tipo de fecha y la ubicación al [ViewModel](app/src/main/java/ar/nuestramerica/prepper/PrepperViewModel.kt). Una corrutina ejecuta la operación de disco en `Dispatchers.IO`.
2. El [repositorio](app/src/main/java/ar/nuestramerica/prepper/PrepperRepository.kt) usa `SQLiteOpenHelper` para insertar una fila en la tabla `lot`, vinculada al producto. Guarda la cantidad como entero en milésimas (0,5 kg se guarda como 500) y genera un código como `LA02` para que puedas anotarlo en el envase.
3. Después de escribir, el repositorio vuelve a leer ajustes, productos, lotes, tareas e historial. El ViewModel publica ese conjunto como `StateFlow<PrepperState>`. La [interfaz Compose](app/src/main/java/ar/nuestramerica/prepper/PrepperScreen.kt) observa el estado y actualiza la tarjeta, el total de la despensa y la lista de compras.
4. Al consumir, la app sugiere el lote utilizable con la fecha más cercana y deja los lotes sin fecha al final. Podés elegir otro. El repositorio verifica la cantidad disponible y descuenta del ID elegido. Si completás una fecha desde **Ver existencias → Editar**, actualiza esa misma fila y conserva el código.

## Cómo se calculan los indicadores

Las [reglas del dominio](app/src/main/java/ar/nuestramerica/prepper/PrepperModels.kt) calculan estos valores a partir de los productos visibles y los ajustes del hogar.

| Indicador | Cálculo |
| --- | --- |
| Meta por producto | Cantidad semanal de referencia para cinco personas × integrantes ÷ 5 × semanas. Se redondea hacia arriba a unidades enteras para envases y a décimas para kg y litros. |
| Para comprar | Diferencia entre la meta y la cantidad total en despensa, con mínimo cero. |
| Días de comida | Suma de las kcal del stock utilizable dividida por las kcal diarias configuradas para todo el hogar. |
| Días de agua | Litros utilizables divididos por el consumo diario de agua del hogar. |

Esta distinción explica por qué un producto puede alcanzar su meta de compra y aportar menos días de autonomía. Un lote vencido sigue físicamente en la despensa, pero deja de contar como utilizable para ese cálculo. Lo mismo ocurre con el agua pasada de su fecha de rotación. Una fecha de consumo preferente pasada por sí sola no excluye el lote. Al cargar agua, el formulario propone una rotación a tres meses y permite cambiarla.

## Cómo funcionan los recordatorios

Al completar la configuración inicial, la [actividad principal](app/src/main/java/ar/nuestramerica/prepper/MainActivity.kt) programa con WorkManager una revisión inmediata y otra periódica diaria. El [worker](app/src/main/java/ar/nuestramerica/prepper/ReviewWorker.kt) lee SQLite aunque la interfaz esté cerrada. Busca lotes vencidos o con fecha dentro de los próximos 60 días y tareas activas cuya fecha ya llegó. Para una tarea semanal, por ejemplo, cuenta una semana desde el último cumplimiento o, si es la primera vez, desde la configuración inicial. Android puede diferir la ejecución del trabajo.

Con los avisos habilitados, cada notificación identifica el lote o la tarea. Al tocarla, la actividad recibe un identificador y consulta el estado actual para abrir el elemento correcto o indicar que el aviso ya está resuelto. Inicio muestra los primeros seis lotes ordenados por fecha y permite abrirlos desde la fila correspondiente.

## Datos y copias

La base SQLite vive en el almacenamiento privado de la app. El selector de archivos de Android permite exportar una copia JSON con ajustes, catálogo, lotes y tareas con su historial. Al restaurar, la app pide confirmación, valida el formato y reemplaza los datos en una transacción. Si la importación falla, la transacción evita que quede una mezcla de la copia y los datos anteriores. [Política de privacidad](publicacion/play/privacidad.html)

## Compilar y probar

Abrí el proyecto en Android Studio, sincronizá Gradle y ejecutá la configuración **app** en un dispositivo o emulador con Android 12 (API 31) o posterior. El proyecto usa el SDK de compilación 37 y el identificador `ar.nuestramerica.prepper`. [Configuración del módulo](app/build.gradle.kts)

Desde la raíz del repositorio:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

El APK de prueba queda en `app/build/outputs/apk/debug/app-debug.apk`. Las [pruebas del dominio](app/src/test/java/ar/nuestramerica/prepper/PrepperCalculationsTest.kt) cubren, entre otras reglas, el redondeo de metas, la selección de lotes y la identificación de avisos. Para retomar el desarrollo, consultá la [memoria del proyecto](PrepperApp-contexto-agente/README.md).
