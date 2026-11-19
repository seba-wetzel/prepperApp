# Producto, contenido y diseño

## Propósito

PrepperApp ayuda a un hogar a organizar una reserva para emergencias: existencias, autonomía estimada, compras, vencimientos y tareas de mantenimiento. La aplicación debe ser útil sin conexión y no depende de una cuenta.

No es una aplicación médica ni garantiza seguridad. Las cifras son parámetros editables y estimaciones de planificación.

## Fuentes del producto

1. `kit-emergencia.html`: prototipo original y referencia funcional.
2. Presentaciones y PDF del curso: identidad visual y contenido educativo validado por el usuario a partir de material de expertos.
3. Código Android actual: fuente de verdad de lo que está implementado.
4. `REQUISITOS_ORIGINALES.md`: intención inicial e historial; puede contener afirmaciones obsoletas sobre el estado técnico.

Cuando haya conflicto, inspeccionar el código y consultar al usuario solo si la decisión cambia materialmente el producto.

## Principios de contenido

- Español rioplatense claro: “vos”, “cargá”, “podés”.
- Priorizar acciones concretas y lenguaje sin alarmismo.
- Diferenciar estimación, recomendación general y dato ingresado por el usuario.
- No profundizar en salud ni ofrecer indicaciones médicas personalizadas.
- Los números y servicios de emergencia pueden variar por jurisdicción; mantener la advertencia.
- El contenido del curso debe inspirar y mantener consistencia, no copiarse indiscriminadamente dentro de pantallas operativas.
- Todo contenido descargable futuro debe indicar fuente, fecha/versión y alcance geográfico.

## Sistema visual vigente

### Paleta

- Rojo de emergencia: `#E10B05`
- Rojo oscuro: `#9E1B16`
- Negro tinta: `#171614`
- Papel: `#F2E9D8`
- Papel claro: `#FFF9EE`
- Grafito: `#555049`
- Verde de estado: `#2F694B`
- Ámbar: `#A86600`

### Lenguaje gráfico

- Titulares pesados, directos y de alto contraste.
- Etiquetas en mayúsculas con estilo monoespaciado.
- Bloques negros tipo rótulo y tarjeta roja para emergencia.
- Bordes oscuros visibles y radios de 2–16 dp, evitando el aspecto de “píldora” genérico.
- Fondos cálidos tipo papel.
- Iconos Material reconocibles; no usar caracteres Unicode como iconos.
- Las texturas, recortes y tratamiento gráfico de las diapositivas deben reservarse para hero images o recursos, sin reducir legibilidad ni rendimiento.

### Ícono de la aplicación

El ícono vigente usa una mochila de emergencia negra con símbolos de agua y alimento, sobre un círculo rojo y fondo papel. La versión adaptativa separa foreground y background para respetar las máscaras del launcher y usa la paleta exacta de la app. El PNG de 512 × 512 para Play Store se conserva en `play-store-assets/icon-512.png`.

La tipografía del curso se aproxima con familias del sistema: Sans Serif pesada y Monospace. No se empaquetaron IBM Plex Mono ni Helvetica.

## Estructura de navegación

La navegación inferior conserva cuatro destinos:

- Inicio
- Despensa
- Tareas
- Ajustes

La futura sección Recursos no debe agregarse como quinto destino sin analizar primero jerarquía, frecuencia de uso y espacio. Alternativas a evaluar: acceso desde Inicio, pantalla secundaria o reemplazo de una arquitectura de navegación.

## Recursos offline: intención pendiente

El usuario quiere descargar o conservar información importante para consultarla sin conexión. Antes de implementarla hay que definir:

- qué documentos son realmente útiles durante una emergencia;
- autoría, licencias y permiso de redistribución;
- alcance regional y fecha de vigencia;
- formato: texto nativo, PDF incluido, archivo descargable o combinación;
- tamaño del APK y política de actualizaciones;
- búsqueda, favoritos y señalización de contenido ya disponible offline;
- advertencias para salud, primeros auxilios y números locales.

No implementar todavía una biblioteca vacía ni convertir automáticamente las diapositivas en “recursos”. Primero preparar inventario y criterios editoriales con el usuario.
