# PrepperApp — contexto para agentes

Actualizado: 18 de noviembre de 2026.

Esta carpeta es la memoria operativa del desarrollo. Un agente que retome el proyecto debe empezar acá y contrastar siempre esta documentación con el código y el estado de Git.

## Lectura recomendada

1. `ESTADO_ACTUAL.md`: qué funciona hoy, qué fue validado y qué falta.
2. `ARQUITECTURA.md`: estructura técnica, persistencia, reglas e invariantes.
3. `PRODUCTO_Y_DISENO.md`: intención del producto, contenido y sistema visual.
4. `DECISIONES.md`: decisiones aceptadas y asuntos todavía abiertos.
5. `DESARROLLO_Y_VALIDACION.md`: cómo compilar, probar y entregar cambios.
6. `ROADMAP.md`: próximos pasos priorizados.
7. `REQUISITOS_ORIGINALES.md`: especificación inicial histórica. No describe por sí sola el estado actual.

## Referencias visuales y de contenido

- `kit-emergencia.html`: prototipo funcional original.
- `Clase 1.pdf` y `Clase 2.pdf`: material de consulta.
- Las dos presentaciones `.odp`: fuente editable del curso.

Los PDF y ODP fueron aportados por el usuario como material de referencia y pueden existir solo en el workspace local. No forman parte del APK ni del commit de esta memoria. No publicarlos sin revisar licencias, tamaño y estrategia offline.

## Estado de referencia

- Proyecto: `/home/seba/AndroidStudioProjects/PrepperApp`
- Rama: `main`
- Paquete: `ar.nuestramerica.prepper`
- Hito implementado y publicado: `780ed8e feat: complete prepper experience and visual identity`
- La aplicación es Android nativa con Kotlin y Jetpack Compose.
- Funciona sin cuenta, backend ni conexión para sus funciones centrales.

## Reglas de continuidad

- Comunicarse con el usuario en español, de forma breve y concreta.
- Inspeccionar el estado real de Git y los archivos antes de modificar.
- Preservar cambios del usuario y evitar reescrituras destructivas.
- Mantener la aplicación offline-first; no introducir servicios remotos sin aprobación.
- Después de cada cambio funcional: compilar, ejecutar pruebas pertinentes, validar en emulador y crear un commit si todo funciona. El usuario pidió explícitamente un commit por cambio válido.
- No afirmar que WorkManager garantiza avisos después de meses de hibernación o de una detención forzada.
- No presentar cantidades de alimentos, agua o indicaciones médicas como asesoramiento profesional personalizado.
- La sección Recursos está planificada, pero todavía requiere análisis de contenido y formato antes de implementarse.
