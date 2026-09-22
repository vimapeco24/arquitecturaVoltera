# BITÁCORA DE AVANCE — Proyecto Voltera (ARTI4208 · Uniandes)

> Registro de contexto persistente. Cada vez que se trabaja en el proyecto se anota aquí para
> que cualquier sesión futura continúe donde se quedó. **No borrar entradas anteriores.**

---

## Datos del proyecto

| Campo | Valor |
|---|---|
| Curso | ARTI-4208 · Arquitecturas de Nueva Generación · Universidad de los Andes |
| Entrega | Proyecto · Entrega 1 |
| Caso | **Voltera** — plataforma de energía distribuida (medición AMI, mercado P2P, comunidades energéticas, certificados de origen) |
| Caso de ejemplo del curso | Solventa (seguros embebidos) — solo referencia, NO es nuestro caso |
| Carpeta raíz | `/Users/vimapeco/Downloads/ArquitectutaNuevaGenegeracion` |
| Carpeta de trabajo (entregables) | `/Modificados` |
| Herramienta destino | **Lucidchart** (import desde `.drawio`) |

## Qué pide la entrega (resumen del enunciado)

**PARTE 1 — ArchiMate (5 vistas del MISMO modelo):**
1. Motivación — stakeholders, drivers, assessments, goals, outcomes, requirements, constraints
2. Estrategia — capabilities, resources, value stream, course of action (SIN software)
3. Negocio — business services, processes, roles/actors, business objects
4. Aplicación — application components, services, interfaces, data objects (realizan negocio)
5. Integración — terceros externos, interfaces que exponen, quién consume, qué se degrada al fallar

**PARTE 2 — DDD (3 pasos):**
1. Dominio y subdominios con tipo (core/supporting/generic) y justificación
2. Subdominios → Bounded Contexts, con razón de cada frontera
3. Lenguaje ubicuo por contexto (incluir al menos un término que choca entre contextos)

**Rúbrica:** Motivación+Estrategia 20% · Negocio+Aplicación 20% · Integración 15% · DDD 30% · Calidad modelo 10% · Presentación 5%.
**Criterio de calidad clave:** "Rotula el tipo entre paréntesis cuando no sea evidente." → aprovechado en las etiquetas.

## Fuentes de verdad del contenido (ya desarrollado)

- `Entrega1_Parte1_ArchiMate_Voltera.md` — las 5 vistas ArchiMate completas de Voltera.
- `Entrega_DDD_Voltera.md` — dominio, 12 subdominios, 12 bounded contexts, lenguaje ubicuo.
- `Cuaderno Nueva Generacion.pptx` — plantilla de entrega (láminas "SU MODELO" vacías).
- `ARTI4208-Proyecto-Entrega-1-v202602.pptx` — enunciado y rúbrica.

---

## HALLAZGO TÉCNICO CLAVE — Compatibilidad draw.io → Lucidchart

Investigado en la documentación oficial de Lucidchart (help.lucid.co, 2026) y foros de la comunidad.

**Lucidchart SÍ importa:**
- Archivos `.drawio` o `.xml` **exportados desde draw.io** (formato `mxGraphModel`).
- Funciona tanto comprimido (base64+deflate) como sin comprimir (XML plano).
- Importa: geometría, texto, **colores**, bordes, **forma del contenedor** (rectángulo, redondeado,
  elipse, rombo, octágono) y **flechas/relaciones** con sus estilos de línea.

**Lucidchart NO importa (confirmado):**
- Stencils propietarios de draw.io `shape=mxgraph.archimate3.*` → aparecen como cajas vacías o se omiten.
- Imágenes embebidas por URL `images.lucid.app/...` → no son públicas, no cargan.
- Datos URI SVG embebidos → no se renderizan de forma fiable.

**CONCLUSIÓN / ESTRATEGIA ADOPTADA:**
Para que las 5 vistas se vean bien y profesionales en Lucidchart **al importar el `.drawio`**, se
codifica la semántica ArchiMate con primitivas que Lucid SÍ entiende:
1. **Color por capa** (motivación=lila, estrategia=durazno, negocio=amarillo, aplicación=azul, tecnología=verde).
2. **Forma del contenedor** según el tipo de elemento (redondeado para comportamiento, recto para
   estructura/pasiva, etc.).
3. **Tipo rotulado entre paréntesis** en la etiqueta — además lo pide la rúbrica.
4. **Ícono Unicode** dentro de la caja como pista visual (no depende de stencils).
5. **Relaciones ArchiMate** con estilos de flecha nativos de draw.io (que Lucid conserva):
   realización (▷ discontinua), influencia (→ punteada), asignación (● línea), acceso (punteada fina),
   serving (→ abierta), triggering (► sólida), composición (◆).

Esto garantiza que el import a Lucidchart conserve estructura, colores, texto y flechas, y que el
diagrama sea legible y correcto sin depender de bibliotecas que Lucid no soporta.

---

## REGISTRO DE SESIONES

### Sesión 2026-08-28

- Leídos: enunciado (pptx), cuaderno (pptx), `Entrega1_Parte1_ArchiMate_Voltera.md`,
  `Entrega_DDD_Voltera.md`, y los `.drawio` existentes en `/Modificados`.
- Investigada la compatibilidad real draw.io → Lucidchart (ver hallazgo técnico arriba).
- Creada esta bitácora de contexto persistente.
- Diagnóstico del estado previo en `/Modificados`:
  - `Voltera_01_Motivacion.drawio` — usaba imágenes `images.lucid.app` (NO cargan en import) + stencils.
  - `Voltera_02_Estrategia.drawio` — comprimido con `mxgraph.archimate3` (iconos NO se ven en Lucid).
  - `Voltera_03_Negocio.drawio` — mezcla similar.
  - `Voltera_DDD_01/02/03.drawio` — comprimidos.
  - Faltan vistas 04 (Aplicación) y 05 (Integración).

### Sesión 2026-08-28 (continuación) — Regeneración de los 9 .drawio

**Acción:** Se regeneraron TODOS los `.drawio` de `/Modificados` con un generador propio que
produce XML `mxGraphModel` sin comprimir y compatible con la importación en Lucidchart.

**Archivos entregables generados (9):**

| Archivo | Vista | Elementos |
|---|---|---|
| `Voltera_01_Motivacion.drawio` | ArchiMate 01 Motivación | 7 stakeholders, 7 drivers, 7 goals, 3 assessments, 7 outcomes, 5 requisitos, 5 restricciones + relaciones |
| `Voltera_01_Motivacion_Arbol.drawio` | 01 alternativa (árbol de trazabilidad) | rama stakeholder→driver→meta→outcome/requisito |
| `Voltera_02_Estrategia.drawio` | ArchiMate 02 Estrategia | 3 course of action, 9 capabilities, 6 resources, value stream de 6 etapas |
| `Voltera_03_Negocio.drawio` | ArchiMate 03 Negocio | 6 servicios, 3 cadenas de proceso, 6 roles/actores, 8 objetos |
| `Voltera_04_Aplicacion.drawio` | ArchiMate 04 Aplicación | 15 componentes, 6 servicios app, 7 objetos de datos, realización→negocio |
| `Voltera_05_Integracion.drawio` | ArchiMate 05 Integración | 11 terceros, 11 interfaces, 9 componentes propios + panel de degradación |
| `Voltera_DDD_01_Subdominios.drawio` | DDD 01 | dominio + 5 core, 3 supporting, 4 generic (con justificación) |
| `Voltera_DDD_02_BoundedContexts.drawio` | DDD 02 | 11 BC + Identidad (shared kernel) + 9 relaciones de evento |
| `Voltera_DDD_03_LenguajeUbicuo.drawio` | DDD 03 | 6 contextos con términos + término que choca («Cliente», «Excedente») |

**Verificación (ejecutada):**
- Los 9 archivos parsean como XML válido (`xml.dom.minidom`).
- Ninguno contiene `mxgraph.archimate*` ni `images.lucid.app` → import limpio en Lucidchart.
- Cada elemento lleva: icono Unicode + texto + **tipo entre paréntesis** (lo exige la rúbrica).
- Relaciones ArchiMate con estilos nativos: influencia, realización, asignación, acceso, serving,
  triggering, composición/agregación, flujo.
- Color por capa aplicado (motivación lila, estrategia durazno, negocio amarillo, aplicación azul,
  tecnología/generic verde).

**Cómo importar en Lucidchart:** New → Import documents → seleccionar el `.drawio` → Import.
(Editar el importado requiere plan de pago; ver/leer es gratis.)

---

## RECOMENDACIONES DE IMÁGENES PARA LA ENTREGA

Como Lucidchart NO renderiza los iconos ArchiMate de los stencils importados, para una entrega
con aspecto profesional se recomienda una de estas opciones (de mayor a menor calidad visual):

1. **Recrear con la biblioteca nativa de Lucid** — En Lucidchart activar la shape library
   *"ArchiMate 3"* (Shapes → Manage → buscar ArchiMate) y colocar los elementos con sus iconos
   oficiales. Es lo único que da los iconos gráficos verdaderos en Lucid. Usar estos `.drawio`
   como plano/guía de qué elemento va dónde y con qué relación.

2. **Exportar como imagen desde draw.io** — Abrir cada `.drawio` en draw.io (app.diagrams.net),
   File → Export as → PNG (escala 2x, con fondo blanco) o SVG. Pegar la imagen en la lámina
   "SU MODELO" del cuaderno `.pptx`. Los iconos Unicode y colores se ven bien así.
   Recomendado: PNG 2x, márgenes de 20px, "Include a copy of my diagram" desactivado.

3. **Capturas para la sustentación** — Para la lámina de "Presentación" (5% de la rúbrica),
   una captura de cada vista + una tabla resumen (dominio, nº de subdominios core/sup/gen, nº BC).

Formatos sugeridos por lámina del cuaderno:
- Láminas 6, 8, 10, 12, 14 (SU MODELO de cada vista ArchiMate) → PNG 2x de la vista correspondiente.
- Láminas DDD (17, 19, 21) → PNG del diagrama DDD + la tabla ya está en `Entrega_DDD_Voltera.md`.

### Sesión 2026-08-28 (3) — Reformateo Vista 01 al estilo lámina de referencia

El usuario envió una captura del formato deseado (lámina "SU MODELO" del compañero) y pidió
"que las líneas queden ordenadas". Se reconstruyó `Voltera_01_Motivacion.drawio` con:
- **Filas etiquetadas a la izquierda** en inglés: Stakeholders, Drivers, Goals, Outcomes,
  Requirements, Constraints (estilo gris, como la referencia).
- Badge "SU MODELO" y título "01 · Motivación".
- **Icono ArchiMate nativo en la esquina superior derecha** de cada caja
  (`shape=mxgraph.archimate3;appType=...`) — se ve como en la imagen de referencia en draw.io.
- **Líneas ordenadas**: 4 columnas alineadas verticalmente; relaciones con
  `edgeStyle=orthogonalEdgeStyle`, saliendo del borde inferior y entrando por el superior, sin
  cruces. Cada columna es una cadena trazable stakeholder→driver→goal→outcome/requirement→constraint.

⚠ **PENDIENTE DE ACLARAR CON EL USUARIO:** la lámina de referencia muestra iconos ArchiMate
gráficos, que solo se ven en **draw.io / Lucid EDU**, NO en Lucidchart estándar al importar
(confirmado en sesión previa). Hay que decidir el destino real:
- Si es **draw.io** (o Lucid EDU): esta versión con `mxgraph.archimate3` se ve idéntica a la referencia. ✅
- Si es **Lucidchart estándar**: los iconos de esquina no se renderizarán; conviene volver a los
  iconos Unicode + tipo entre paréntesis (versión anterior) o recrear en Lucid con su biblioteca nativa.

_(Las próximas entradas se agregan debajo, con fecha.)_

### Sesión 2026-08-28 (4) — Reformateo Vista 02 Estrategia

El usuario envió captura del formato de Estrategia. Se reconstruyó `Voltera_02_Estrategia.drawio`:
- Filas a la izquierda: Resource, Capability, Value Stream, Course of Action.
- Color durazno (#F5DEAA), badge "SU MODELO", iconos ArchiMate en esquina
  (resource/capability/valueStream/courseOfAction).
- Contenido igual a la referencia: 1 resource, 7 capabilities, 5 value streams, 1 course of action.
- Líneas ortogonales ordenadas: secuencia del value stream, capability→etapa (serving),
  resource→capability (assignment), course→flujo (realization).
- Mismo criterio de iconos que Vista 01 (visibles en draw.io/Lucid EDU; PENDIENTE confirmar
  destino Lucidchart estándar).

### Sesión 2026-08-28 (5) — Reformateo Vista 03 Negocio

Captura de referencia recibida. `Voltera_03_Negocio.drawio` reconstruido:
- Filas: Business Role, Business Process, Business Service. Color amarillo (#FCEE9C).
- Iconos ArchiMate en esquina (role/process/service). Badge "SU MODELO".
- Contenido de la referencia: 3 roles, 7 procesos en secuencia (Configurar país→Habilitar país),
  1 servicio (Operación energética regional).
- Líneas ordenadas: flujo horizontal entre procesos, asignación rol→proceso, realización
  proceso→servicio.

### Sesión 2026-08-28 (6) — Validación rigurosa DDD contra el enunciado y rehecho en formato TABLA

El usuario pidió revisar a fondo el enunciado (pptx) y validar el DDD, apuntando al sitio del curso
(que requiere login; el contenido equivalente está en los 37 PDFs de `/DDD/`, ya extraídos en
`DDD_contenido_completo.txt`).

**Hallazgo clave (rigor):** el profesor NO pide diagramas de cajas para DDD; pide **TABLAS** con
columnas exactas (verificado extrayendo las formas de las láminas 17/19/21 del pptx):
- Lámina 17 (DDD 01): `Subdominio | Tipo | Por qué es de ese tipo` + frase del dominio.
- Lámina 19 (DDD 02): `Subdominio | Bounded context(s) | Razón de la frontera`.
- Lámina 21 (DDD 03): `Bounded context | Términos propios | Término que choca con otro contexto`.
- Elementos válidos declarados: DDD01 = core/supporting/generic domain; DDD02 = bounded context,
  subdomain, lenguaje ubicuo, dueño, ritmo de cambio; DDD03 = ubiquitous language, término, sinónimo.

**Acción:** se rehicieron los 3 diagramas DDD en formato TABLA exacto + un diagrama de apoyo:
- `Voltera_DDD_01_Subdominios.drawio` — tabla 12 subdominios (5 core/3 sup/4 generic) con tipo
  codificado por color y justificación; frase del dominio destacada. Cumple "no todos son core".
- `Voltera_DDD_02_BoundedContexts.drawio` — tabla subdominio→BC con razón de frontera (lenguaje/
  dueño/ritmo) + nota de proyección no 1:1 (core de comercialización se parte en Cotización/P2P/
  Facturación) + mención de ACL con externos.
- `Voltera_DDD_02b_MapaContextos.drawio` — NUEVO diagrama de apoyo: mapa de contextos con eventos
  de dominio (pub/sub), Shared Kernel (Identidad) y ACL con 3 externos. Relaciones ortogonales ordenadas.
- `Voltera_DDD_03_LenguajeUbicuo.drawio` — tabla con términos propios y, en columna amarilla, el
  término que CHOCA por cada contexto (Excedente, Cliente, Emisión, Sesión, Dispositivo, Miembro).

**Validación conceptual contra el material del curso (PDFs /DDD/):**
- Tipos core/supporting/generic con justificación ✓ (el curso clasifica Cotización/Perfilamiento core,
  Identidad genérico).
- BC = candidato natural a microservicio ✓.
- ACL traduce el mundo externo al lenguaje ubicuo ✓ (incluido en mapa de contextos).
- Comunicación por eventos de dominio (consistencia eventual entre agregados) ✓.
- OHS / Published Language ✓.
- Nota: agregados/DDD táctico son de Módulo 2; la Entrega 1 solo pide DDD estratégico → correcto no incluirlos.

**Estado final de la carpeta (10 .drawio, todos XML válido):**
- Vistas 01/02/03: reformateadas al estilo lámina del usuario (con iconos ArchiMate nativos → se ven
  en draw.io; en Lucidchart estándar los iconos de esquina no se renderizan).
- Vistas 04/05 y los 4 DDD: 100% limpios (solo primitivas) → import perfecto en Lucidchart.
- PENDIENTE: enviar láminas de referencia de 04 y 05 para reformatearlas al mismo estilo (o confirmar
  destino draw.io vs Lucidchart estándar para decidir iconos).

### Sesión 2026-08-28 (7) — Vista 04 Aplicación en notación ArchiMate

El usuario envió el enunciado de la lámina 04 y pidió ajustar `Voltera_04_Aplicacion.drawio` para
verse bien en ArchiMate. Reconstruido con estilo lámina:
- Filas: Business Service → Application Service → Application Component → Data Object.
- Iconos ArchiMate nativos en esquina (service/component/dataObject). Colores: negocio amarillo,
  aplicación azul. Badge "SU MODELO".
- 6 columnas trazables (Telemetría, Facturación, Mercado P2P, Comunidades, Certificados, Flexibilidad).
- Relaciones ordenadas verticales cumpliendo el "LISTO CUANDO": componente → app service → business
  service (realización) y componente → data object (acceso).
- Nota: usa iconos ArchiMate nativos → visibles en draw.io/Lucid EDU (como vistas 01/02/03).

### Sesión 2026-08-28 (8) — Vista 05 Integración en notación ArchiMate + estado final

El usuario envió el enunciado de la lámina 05 y pidió ajustar Integración. Reconstruido:
- 4 filas en orden de lectura de ABAJO hacia ARRIBA (como exige el enunciado):
  Tercero (externo) → Interfaz externa → Componente propio → Qué se degrada.
- Iconos ArchiMate en esquina (actor/interface/component). Colores: externo rojo, tech verde,
  app azul, degradación amarillo. Badge "SU MODELO".
- 8 dependencias clave con serving vertical ordenado por columna (tercero→interfaz→componente→
  degradación). Cumple "LISTO CUANDO": por cada tercero se sigue la flecha y se ve qué se cae.

**ESTADO FINAL — 11 archivos .drawio en /Modificados (todos XML válido):**
1. Voltera_01_Motivacion.drawio — vista 1 (estilo lámina, iconos ArchiMate)
2. Voltera_01_Motivacion_Arbol.drawio — árbol de trazabilidad (limpio)
3. Voltera_02_Estrategia.drawio — vista 2 (estilo lámina)
4. Voltera_03_Negocio.drawio — vista 3 (estilo lámina)
5. Voltera_04_Aplicacion.drawio — vista 4 (estilo lámina ArchiMate)
6. Voltera_05_Integracion.drawio — vista 5 (estilo lámina, lectura abajo→arriba)
7. Voltera_DDD_01_Subdominios.drawio — DDD tabla (formato profesor)
8. Voltera_DDD_01_Subdominios_ArchiMate.drawio — DDD subdominios en notación ArchiMate (capabilities)
9. Voltera_DDD_02_BoundedContexts.drawio — DDD tabla subdominio→BC
10. Voltera_DDD_02b_MapaContextos.drawio — mapa de contextos (eventos, shared kernel, ACL)
11. Voltera_DDD_03_LenguajeUbicuo.drawio — DDD tabla lenguaje ubicuo

Las 5 vistas ArchiMate + DDD cubren toda la Entrega 1. Vistas usan iconos ArchiMate nativos
(visibles en draw.io/Lucid EDU). DDD en tabla + mapa son limpios para Lucidchart estándar.
