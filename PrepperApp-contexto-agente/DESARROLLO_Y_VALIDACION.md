# Desarrollo y validación

## Preparación

Abrir el proyecto raíz en Android Studio:

`/home/seba/AndroidStudioProjects/PrepperApp`

Antes de trabajar:

1. revisar `git status --short --branch`;
2. leer esta carpeta;
3. inspeccionar los archivos afectados desde los buffers del IDE;
4. preservar cambios no relacionados del usuario.

## Comandos de verificación

Verificación completa actual:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Para una iteración rápida puede ejecutarse primero la tarea más específica, pero antes del commit funcional debe correrse el conjunto completo cuando el cambio afecta producción.

## Pruebas existentes

`PrepperCalculationsTest.kt` cubre ocho casos de lógica, entre ellos objetivos, autonomía, lotes, fechas y tareas. Al cambiar reglas de negocio, actualizar o ampliar estas pruebas.

Faltan:

- pruebas de repositorio con base real;
- pruebas Compose de onboarding y navegación;
- pruebas automáticas de intents y permisos;
- round-trip de exportación/restauración;
- migraciones de esquema;
- WorkManager en condiciones simuladas.

## Validación manual mínima

Para cambios de UI o flujos:

1. desplegar en el emulador activo;
2. comprobar onboarding con una instalación limpia cuando corresponda;
3. verificar Inicio, Despensa, Tareas y Ajustes;
4. probar entradas inválidas y cancelaciones;
5. revisar que el teclado y barras del sistema no oculten acciones;
6. comprobar Logcat por `AndroidRuntime`.

Para emergencias, verificar que el marcador reciba el número, pero no pulsar el botón de llamada durante pruebas.

Para notificaciones, distinguir:

- permiso concedido/denegado;
- canal habilitado/deshabilitado;
- prueba inmediata;
- trabajo periódico;
- Doze/hibernación;
- detención forzada.

## Backup

Probar por separado:

- cancelar el selector de exportación;
- guardar correctamente y actualizar fecha;
- restaurar JSON v2 válido;
- rechazar versión o estructura inválida;
- mantener datos previos si la importación falla;
- confirmar antes de reemplazar.

## Git y entregas

El usuario pidió que cada cambio funcional válido tenga un commit.

Secuencia recomendada:

1. validar build/pruebas/lint;
2. revisar `git diff --check`;
3. incluir solo archivos del cambio;
4. crear un mensaje descriptivo;
5. subir a `origin/main` cuando el usuario haya autorizado ese flujo.

No incluir automáticamente:

- cambios de `.idea` generados por sincronización;
- APK, `build/` o cachés;
- presentaciones/PDF grandes sin decisión explícita de versionarlos.

## Última validación conocida

Commit `780ed8e`:

- build, pruebas y lint exitosos;
- 8 pruebas unitarias aprobadas;
- desplegado en `emulator-5554`;
- Inicio y emergencias inspeccionados visualmente;
- marcador abierto con 100;
- sin errores AndroidRuntime observados.
