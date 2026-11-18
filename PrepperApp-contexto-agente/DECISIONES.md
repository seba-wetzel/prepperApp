# Registro de decisiones

## Aceptadas

### Offline-first

La primera versión funciona completamente en el dispositivo. No hay cuenta, backend ni sincronización. Compartir y exportar usan capacidades del sistema, pero la gestión cotidiana no requiere internet.

### Android nativo con Compose

La demo HTML es referencia, no implementación. La app usa Jetpack Compose y componentes Android accesibles; no usa WebView.

### SQLite directo

La implementación actual usa `SQLiteOpenHelper` para minimizar dependencias. Una migración a Room solo se justifica si mejora pruebas, migraciones o mantenimiento sin poner en riesgo los datos.

### Inventario por existencias separadas

Un producto puede tener varias cantidades con vencimientos y ubicaciones diferentes. La UI evita el término técnico “lote” en las acciones principales: Agregar, Consumir y Ver existencias abren diálogos separados, cada uno con una sola finalidad. Al consumir se muestran todas las existencias, se preselecciona y marca como sugerida la utilizable con vencimiento o rotación más próxima, pero el usuario puede elegir otra y el descuento se aplica únicamente a la seleccionada.

### Fechas argentinas y rotación del agua

La entrada de fecha usa `DD/MM/AA` o `DD/MM/AAAA`, teclado numérico autoformateado y calendario en español argentino. Para Agua se usa `ROTATION` y se propone tres meses desde la carga, tomando el extremo conservador del intervalo de 3–6 meses indicado por la clase. Una rotación vencida no cuenta como agua utilizable hasta que se actualice la existencia.

### Onboarding antes de avisos

Las tareas iniciales no se consideran pendientes hasta que el usuario termina de configurar su hogar. WorkManager se programa únicamente después.

### Notificaciones prudentes

La app usa una revisión diaria agrupada. No promete hora exacta ni supervivencia ilimitada a hibernación/detención forzada.

### Identidad del curso

La UI usa rojo, negro, crema y tipografía editorial inspirada en las presentaciones, adaptada para legibilidad y uso cotidiano.

### Emergencias mediante marcador

Los números se guardan localmente y se abren con `ACTION_DIAL`. La app no llama por sí sola.

### Salud fuera del foco

El producto puede organizar botiquín o medicación habitual como tareas, pero no debe convertirse en una fuente de diagnóstico o tratamiento.

## Abiertas

- Modelo especializado para recipientes/reservas de agua.
- Contenido, formato y navegación de la futura sección Recursos.
- Compatibilidad de importación con backups v1 de la demo.
- Anticipaciones configurables y posposición de avisos.
- Estrategia de migración si se adopta Room.
- Alcance territorial de números de emergencia y recursos.
- Licencias y distribución de materiales del curso.
- Necesidad de modo oscuro como diseño específico; hoy deriva del tema del sistema.
