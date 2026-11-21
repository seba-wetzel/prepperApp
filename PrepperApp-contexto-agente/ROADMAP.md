# Roadmap propuesto

Este orden es orientativo. Confirmar con el usuario antes de expandir el alcance.

## Próximo bloque: consolidación

- Probar onboarding desde instalación limpia.
- Agregar pruebas de repositorio y round-trip del backup.
- Cubrir migración de base de datos y errores de importación.
- Revisar accesibilidad: TalkBack, contraste, escalado de fuente y objetivos táctiles.
- Validar diseño en teléfono compacto y pantalla grande.
- Probar notificaciones con permiso denegado y canal deshabilitado.

## Recursos offline: análisis antes de código

1. Inventariar contenido de las clases y separar:
   - guías operativas breves;
   - listas de verificación;
   - números y fuentes locales;
   - documentos extensos;
   - contenido sensible de salud.
2. Definir fuentes, licencias, fecha de revisión y región.
3. Diseñar arquitectura de información y entrada desde la app.
4. Elegir estrategia offline y actualización.
5. Recién entonces implementar una primera colección pequeña y validada.

No usar las diapositivas completas como contenido final sin esta etapa.

## Inventario y agua

- Evaluar entidad `WaterContainer` o equivalente.
- Evitar doble conteo con el producto Agua existente.
- Registrar fecha de carga, revisión, renovación, tipo y capacidad.
- Vincular tareas sin marcar automáticamente renovación al completar una inspección.

## Avisos

- Anticipación configurable para vencimientos.
- Deduplicación persistente por evento.
- Acción de posponer sin completar.
- Reconciliación tras editar, eliminar o restaurar.
- Pantalla de diagnóstico de última ejecución y próximo evento.
- Validación de reinicio, Doze, hibernación y fabricantes.

## Calidad de producto

- Completado: pantallas separadas por funcionalidad y componentes compartidos en `ui/components/`.
- Considerar Room solo con plan de migración y pruebas.
- Añadir pruebas UI críticas.
- Revisar modo oscuro específico para la nueva identidad.
- Preparar iconografía/ilustraciones propias si el usuario aporta assets extraídos de las diapositivas.
- Revisar package name y firma antes de una publicación real.
