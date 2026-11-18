# Arquitectura técnica

## Plataforma y dependencias

- Kotlin `2.2.10`
- Android Gradle Plugin `9.5.1`
- `compileSdk` y `targetSdk`: 37
- `minSdk`: 31
- Jetpack Compose + Material 3
- Compose BOM `2026.02.01`
- Lifecycle/ViewModel + StateFlow
- WorkManager `2.12.0`
- SQLite mediante `SQLiteOpenHelper`
- JUnit 4 para pruebas de lógica

No hay Room, Hilt, Retrofit, Firebase, DataStore ni servicios de red.

## Archivos principales

| Archivo | Responsabilidad |
| --- | --- |
| `MainActivity.kt` | Integración Android: permisos, intents, compartir, SAF, marcador y composición raíz |
| `PrepperScreen.kt` | Toda la UI Compose y navegación interna de cuatro secciones |
| `PrepperModels.kt` | Modelos, cálculos, fechas y formateo |
| `PrepperViewModel.kt` | Estado observable, operaciones asíncronas y mensajes de UI |
| `PrepperRepository.kt` | SQLite, semillas, mutaciones y backup JSON |
| `ReviewWorker.kt` | Trabajo periódico, canal y notificaciones |
| `ui/theme/*` | paleta, tipografía, formas y tema Material |
| `PrepperCalculationsTest.kt` | pruebas unitarias de reglas principales |

## Flujo de datos

```text
Compose -> PrepperViewModel -> PrepperRepository -> SQLite
   ^              |
   |              +-> StateFlow<PrepperState>
   +--------------+
```

Las operaciones del ViewModel se ejecutan en `Dispatchers.IO`. Después de cada mutación, el repositorio vuelve a cargar un `PrepperState` completo.

## Esquema SQLite

Base: `prepper_app.db`, versión 2.

- `settings`: una fila con personas, semanas, kcal, agua diaria, último backup, onboarding y fecha inicial del cronograma.
- `product`: catálogo base y productos personalizados.
- `lot`: existencias por producto; cantidad en milésimas, fecha opcional, tipo, ubicación y disponibilidad.
- `task`: definición, período y estado activo.
- `task_completion`: historial con fecha local e instante.

Las claves foráneas están habilitadas. Al eliminar un producto o tarea, sus relaciones usan borrado en cascada.

## Unidades e invariantes

- Las cantidades se guardan como `Long` en milésimas para evitar errores de coma flotante.
- No se permiten lotes negativos.
- Un lote sin fecha es válido.
- `DateKind.EXPIRATION` vencido no cuenta para autonomía utilizable.
- `DateKind.BEST_BEFORE` no se excluye automáticamente.
- El stock total es la suma de lotes.
- El consumo recorre lotes disponibles ordenados por usabilidad, fecha conocida más próxima e ID.
- Los productos base no se eliminan; solo los personalizados.
- Ajustes válidos: 1–30 personas, 1–52 semanas, 1000–5000 kcal y 0,5–20 litros diarios por persona.

## Cálculos

- Meta de días: `semanas × 7`.
- Objetivo de producto: base semanal para cinco personas, escalada por integrantes y semanas.
- Unidades contables se redondean hacia arriba a enteros.
- Kg y litros se redondean hacia arriba a décimas.
- Días de comida: kcal utilizables / consumo diario del hogar.
- Días de agua: mililitros utilizables / agua diaria del hogar.
- Próximos vencimientos: hasta seis lotes con fecha dentro de los próximos 60 días, incluyendo vencidos.
- Tarea siguiente: último cumplimiento + período; sin historial, inicio del cronograma + período.

## Onboarding y recordatorios

`MainActivity` observa `!loading && settings.configured`:

- configurado: crea canal, programa revisión diaria y ejecuta una revisión inmediata;
- no configurado: cancela ambos trabajos y limpia la notificación.

`ReviewWorker` siempre vuelve a comprobar `settings.configured`. Esta doble defensa impide alertas antes del onboarding.

WorkManager ofrece ejecución persistente, pero Android puede retrasarla por Doze, ahorro, hibernación o políticas del fabricante. Una detención forzada bloquea trabajos hasta que el usuario abra nuevamente la app.

## Backup JSON v2

Incluye:

- versión y fecha de exportación;
- ajustes;
- productos;
- lotes;
- tareas e historial.

La importación exige versión 2, aplica límites de cantidad de elementos y reemplaza los datos en una transacción. No hay compatibilidad implementada con el JSON v1 de la demo HTML.

## Criterios para cambios futuros

- Mantener reglas y cálculos fuera de Compose.
- Toda migración de esquema debe incrementar la versión de base y conservar datos.
- No duplicar el total de agua si se agrega una entidad de recipientes.
- Las operaciones que reemplazan o eliminan datos requieren confirmación o posibilidad de deshacer.
- Evitar dependencias nuevas salvo que resuelvan una necesidad concreta.
