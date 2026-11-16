# Voltera — Módulo 5: IA Generativa y Agéntica

## Agente de Diagnóstico de Activos Distribuidos con Gobernanza Multi-Firma

**Curso:** ARTI4208 · Arquitecturas de Nueva Generación  
**Programa:** Maestría en Arquitecturas de TI (MATI) — Universidad de los Andes  
**Código del caso:** ARTI4208-2026-P  
**Grupo:** 5 · **Tema de investigación asignado:** Tema 5 — Prompt Engineering a gran escala y versionamiento  
**Fecha:** noviembre de 2026

---

## 1. Caso de negocio

### 1.1 Nombre del caso

Agente de diagnóstico de activos distribuidos con gobernanza multi-firma.

### 1.2 Problema u oportunidad

Voltera gestiona paneles solares, baterías, inversores y cargadores de vehículos eléctricos con protocolos heterogéneos y conectividad intermitente. El equipo de operaciones necesita detectar anomalías en el **estado de los dispositivos** (degradación de batería, inversores fuera de rango, temperatura anormal, códigos de error) antes de que escalen a cortes o facturación errónea, pero hoy revisan dashboards manualmente y reaccionan ante alarmas sin contexto.

> Alcance: este caso trata el estado físico de los dispositivos DER (SOC, temperatura, voltaje, potencia, códigos de error). Es distinto de la detección de lecturas anómalas de medición (consumo/generación) que ya resuelve el BC Telemetría en la Entrega 3.

Además, cuando se identifica una acción correctiva sobre un dispositivo físico (descargar batería, reiniciar inversor), no existe un mecanismo trazable ni auditable que garantice que la acción fue autorizada por las partes involucradas antes de ejecutarse.

Se propone un sistema de agentes de IA que diagnostique, recomiende acciones y someta cada acción crítica a un contrato inteligente de aprobación multi-firma (3 de 3) antes de que el comando llegue al dispositivo.

### 1.3 Quién lo usa

- **Equipo de operaciones de red y campo** — desde la consola web, reciben el diagnóstico del agente, pueden conversar con el Assistant sobre él y firman la aprobación técnica.
- **Prosumidor (dueño del activo)** — desde la app móvil, recibe la notificación de acción recomendada sobre su dispositivo, puede hacer preguntas al Assistant y otorga o rechaza su consentimiento firmado.

> El **gestor de comunidad energética no participa** en este flujo: cada medidor y cada DER pertenece a un hogar y está a cargo de un único prosumidor, por lo que las decisiones sobre esos activos no le corresponden al gestor.

### 1.4 Valor esperado

- Reducción del tiempo medio de detección de anomalía de dispositivo de 45 minutos a **menos de 5 minutos**.
- Disminución en un **40 %** de incidentes que llegan a afectar facturación.
- **100 %** de las acciones correctivas sobre dispositivos físicos quedan registradas on-chain con trazabilidad completa de quién propuso, quién aprobó y cuándo se ejecutó, habilitando auditoría regulatoria ante la CREG.

### 1.5 Qué hace la IA

#### RAG (Retrieval-Augmented Generation)

Recupera fichas técnicas del fabricante, históricos de fallos conocidos por modelo de dispositivo, procedimientos operativos de campo y regulación CREG aplicable, indexados en un vector store. El agente fundamenta cada diagnóstico en documentación verificable, no en invención.

#### AI Flows — Orquestación multiagente

Se orquesta un flujo de 4 agentes especializados:

1. **Agente Monitor** — Recibe eventos de estado de los dispositivos DER vía streaming (estado de batería, voltaje de inversor, temperatura, códigos de error) y evalúa umbrales adaptativos. Cuando detecta una anomalía, genera un evento de alerta enriquecido.

2. **Agente Diagnosticador** — Toma la alerta, consulta el RAG (fichas técnicas, fallos previos del mismo modelo) y la telemetría histórica vía MCP. Produce un diagnóstico en lenguaje natural con nivel de severidad (crítico / alto / medio / bajo) y la evidencia que lo sustenta.

3. **Agente de Acción** — Si el diagnóstico requiere intervención (comando al dispositivo u orden de trabajo de campo), genera la propuesta de acción con parámetros concretos y la registra como una transacción pendiente en el contrato inteligente multi-firma.

4. **Agente Notificador** — Envía notificaciones contextualizadas (push en app móvil, correo, alerta en consola) a los firmantes humanos (operador y prosumidor) con el resumen del diagnóstico y la acción propuesta, en lenguaje comprensible para cada audiencia.

#### Assistant — conversación acotada antes de firmar

Antes de firmar, el prosumidor (en la app) y el operador (en la consola) pueden conversar con el **Assistant** sobre el diagnóstico concreto: *"¿por qué hay que reiniciar mi inversor?"*, *"¿qué pasa si no apruebo?"*, *"¿me quedo sin energía?"*.

- Responde **solo** con el diagnóstico, la propuesta y la evidencia RAG de ese caso.
- **No tiene herramientas de acción**: no firma, no crea propuestas, no ejecuta comandos. La firma es siempre un control separado de la conversación.
- Usa prompts distintos por audiencia (prosumidor: lenguaje simple; operador: lenguaje técnico).
- Un guardrail impide que el Assistant **persuada** al prosumidor de aprobar: debe presentar de forma neutral las consecuencias de aprobar y de rechazar.

#### MCP Servers

Se despliegan dos MCP Servers como herramientas invocables por los agentes:

- **MCP Device Management** — Expone telemetría en tiempo real, historial de comandos, topología de red y ejecución de comandos sobre dispositivos DER. Cada llamada se autentica con identidad de servicio bajo Zero Trust.
- **MCP Blockchain Gateway** — Expone las operaciones del contrato inteligente: crear propuesta de acción, consultar estado de firmas, ejecutar acción aprobada. Aísla al LLM de la complejidad on-chain.

#### Blockchain — Contrato multi-firma 3/3

Un contrato inteligente gestiona el ciclo de vida de cada acción correctiva sobre dispositivos:

1. **Propuesta** — El Agente de Acción registra la acción recomendada (tipo de comando, dispositivo destino, parámetros, hash del diagnóstico que la justifica y **versión del prompt y del modelo** que lo generaron).
2. **Firma 1: Agente IA** — El agente firma automáticamente la propuesta que él mismo generó, dejando constancia on-chain de que la recomendación proviene del sistema de diagnóstico con su evidencia.
3. **Firma 2: Operador de Voltera** — Un miembro del equipo de operaciones revisa el diagnóstico y, si lo valida técnicamente, firma desde la consola web.
4. **Firma 3: Prosumidor** — El dueño del activo recibe la notificación en su app con el diagnóstico explicado en lenguaje natural y aprueba o rechaza la intervención sobre su dispositivo.
5. **Ejecución** — Solo cuando las 3 firmas están registradas, el contrato emite un evento que el MCP Blockchain Gateway captura y traduce en la ejecución del comando vía el MCP Device Management.
6. **Registro** — La transacción completada (propuesta + 3 firmas + resultado del comando) queda inmutable on-chain para auditoría.

**Timeout y escalación:** si alguna firma no llega en un plazo configurable (p. ej. 30 minutos para severidad crítica), el contrato emite un evento de escalación que el Agente Notificador recoge y reenvía con urgencia elevada **al mismo firmante pendiente** (operador o prosumidor) por canales adicionales. La escalación nunca transfiere la decisión a otra persona. Si la propuesta vence sin las 3 firmas, queda registrada on-chain como *vencida* y no se ejecuta; se puede generar una nueva propuesta. **La acción nunca se ejecuta sin las 3 firmas — no existe modo degradado 2/3.**

### 1.6 Qué decide una persona

- **El operador de Voltera** decide si el diagnóstico del agente es técnicamente correcto y si la acción propuesta es segura para la red (firma 2).
- **El prosumidor** decide si consiente la intervención sobre su propio activo — soberanía sobre su dispositivo (firma 3). Esta decisión es exclusiva del prosumidor: ni el gestor de comunidad ni ningún otro actor puede firmar en su nombre.
- **Ambos** pueden rechazar la acción, lo cual queda registrado on-chain con la razón del rechazo.
- **Nadie del lado IA** ejecuta un comando físico de forma autónoma: la firma del agente solo certifica que el diagnóstico fue generado por el sistema, pero jamás basta por sí sola para activar un dispositivo. El Assistant informa, pero no decide ni firma.

### 1.7 Integración con lo construido

#### Disparadores (eventos nuevos)

| Evento | Emisor | Tópico (Kafka / Event Mesh de la E3) |
|---|---|---|
| `EstadoDERReportado` | Inversores, baterías y cargadores (SOC, temperatura, voltaje, códigos de error) | `der.estado` |
| `AnomaliaDERDetectada` | Agente Monitor | `der.alertas` |
| `PropuestaAccionCreada` · `FirmaRegistrada` · `FirmaVencida` · `AccionAprobada` · `AccionRechazada` · `AccionEjecutada` | Contrato multi-firma, vía MCP Blockchain Gateway | `der.gobernanza` |

#### Microservicios usados como herramientas

| Herramienta (MCP) | Microservicio detrás | Origen |
|---|---|---|
| MCP Device Management | MS Control DER (BC Control DER) | BC definido en la E1; **nuevo** como microservicio |
| MCP Device Management | MS Telemetría Core (cruce con el consumo del medidor asociado) | E3, se reutiliza |
| MCP Blockchain Gateway | Gateway de cadena de la E4 + nuevo contrato `MultiFirmaAccionDER` en la misma red | E4, **modificado** |
| (Agente Notificador) | MS Notificaciones (plantillas de firma con enlace directo) | E2, **modificado** |
| (Firmantes) | BC Identidad | E1 |

#### Degradación elegante

| Falla | Comportamiento |
|---|---|
| Frontier LLM no disponible | El Model Gateway cambia a un modelo alterno; si no hay ninguno, el operador recibe una alerta clásica, sin diagnóstico. |
| El RAG no encuentra evidencia suficiente | El Diagnosticador lo declara ("sin fuente") y no propone acción; el caso escala al operador. |
| Assistant no disponible | El firmante sigue viendo el diagnóstico completo y la evidencia; solo se pierde la conversación. La firma no depende del Assistant. |
| Versión nueva de prompt degrada la calidad | Rollback inmediato a la versión anterior desde el Prompt & Model Registry (ver Tema 5). |
| Red blockchain no disponible | Las propuestas se encolan y no se ejecuta nada (fail-closed). |
| Una firma no llega a tiempo | Escalación al mismo firmante; al vencer, la propuesta queda *vencida* y no se ejecuta. |
| Riesgo físico inmediato | Actúa el fail-safe del dispositivo definido como obligatorio en la E1, por fuera del flujo agéntico; la regla 3/3 no compromete la seguridad física. |

#### Avance para la lámina 03 (nuevo / modificado / retirado)

- **Nuevos:** los 4 agentes, el Assistant, MS Control DER, los dos MCP Servers, el contrato `MultiFirmaAccionDER`, el Prompt & Model Registry y los tópicos `der.*`.
- **Modificados:** gateway de cadena de la E4, MS Notificaciones, el Event Mesh de la E3 (tópicos nuevos) y la app móvil / consola (pantalla de revisión y firma con conversación).
- **Retirados:** revisión manual de dashboards y triage manual de alarmas sin contexto.

### 1.8 Decisiones del equipo

| Decisión | Fecha |
|---|---|
| Se mantiene la propuesta de 4 agentes, contrato multi-firma 3/3 y 2 MCP Servers trabajada en clase. | 14-nov-2026 |
| La ejecución ocurre únicamente con las 3 firmas (IA + operador + prosumidor); no existe modo degradado 2/3. | 14-nov-2026 |
| El gestor de comunidad no firma ni decide sobre los DER de un hogar: cada activo está a cargo de un único prosumidor. | 14-nov-2026 |
| Se incluye el Assistant con conversación acotada al diagnóstico antes de firmar (opción B), sin herramientas de acción. | 14-nov-2026 |
| El grupo profundiza el Tema 5 (Prompt Engineering a gran escala y versionamiento): el Prompt & Model Registry y Evals & Tracing son los componentes de mayor énfasis. | 14-nov-2026 |

---

## 2. Arquitectura de referencia — selección de componentes

La arquitectura de referencia es la **plataforma de IA de Voltera**: debe servir a varias soluciones basadas en LLM (este agente de diagnóstico, y a futuro p. ej. el copiloto de factura o el optimizador de baterías), todas comportándose igual. Se distinguen tres niveles:

- **Núcleo** — componentes que la rúbrica exige (RAG, agente, MCP, contexto, prompt, monitoreo, orquestación) y que toda solución reutiliza.
- **Caso** — componentes que el caso de uso necesita y que quedan en la arquitectura.
- **Diferido / fuera** — no se abordan por ahora; cada exclusión tiene su razón.

**Énfasis Tema 5** — los componentes marcados con ★ son los que se profundizan en el punto 04.

| Elemento exigido por la plantilla | Componente que lo realiza |
|---|---|
| RAG | Knowledge Service (RAG) + Ingestion Pipeline + Vector Index |
| Agente | Process Agent (4 agentes) + Assistant |
| MCP | MCP Servers (Device Management, Blockchain Gateway) |
| Contexto | State & Memory |
| Prompt | ★ Prompt & Model Registry |
| Monitoreo | ★ Evals & Tracing + FinOps |
| Orquestación | AI Workflows |

### 2.1 Subsistema Experiencia

| Componente | Nivel | Justificación |
|---|---|---|
| **App & Portal** | Caso | El prosumidor revisa el diagnóstico, conversa con el Assistant y firma (aprueba o rechaza). Canal de la firma 3/3. |
| **Employee Apps** | Caso | El operador revisa el diagnóstico, conversa con el Assistant y firma la validación técnica. Canal de la firma 2/3. |
| **Messaging & Email** | Caso | El Agente Notificador envía alertas push, correos y mensajes a los firmantes humanos, y escala al mismo firmante si hay timeout. |
| **API Gateway** | Caso (existente E2) | Punto de entrada único para app, consola y agentes; autenticación y rate limiting. |
| **Human Review** | Caso | Pantalla de revisión y firma: ninguna acción sobre dispositivos físicos se ejecuta sin pasar por aquí. |
| **Assistant** | Caso | Conversación acotada al diagnóstico antes de firmar; sin herramientas de acción. Familia de prompts conversacionales por audiencia (★ Tema 5). |

### 2.2 Subsistema Orquestación

| Componente | Nivel | Justificación |
|---|---|---|
| **AI Workflows** | Núcleo | Flujo determinista Monitor → Diagnosticador → Acción → Notificador. Columna vertebral de la orquestación multiagente. |
| **Process Agent** | Núcleo | Cada uno de los 4 agentes, con rol, prompt versionado, herramientas MCP y contexto específicos. |
| **State & Memory** | Núcleo | Estado del flujo entre agentes (diagnóstico, propuesta, estado de firmas 0/3–3/3) y contexto de la conversación del Assistant por caso. |
| **Agent Harness** | Diferido | Lo provee el runtime del workflow (ciclo de vida, reintentos, timeouts, inyección de herramientas); no se diseña como componente aparte. |

### 2.3 Subsistema Conocimiento

| Componente | Nivel | Justificación |
|---|---|---|
| **Knowledge Service (RAG)** | Núcleo | Fundamenta diagnósticos y respuestas del Assistant en fichas técnicas, históricos de fallos y regulación CREG. |
| **Ingestion Pipeline** | Núcleo | Procesa e indexa manuales de fabricantes, resoluciones CREG y procedimientos de campo cuando hay actualizaciones. |
| **Vector Index** | Núcleo | Almacena los embeddings para búsqueda semántica. El **grafo** de conocimiento queda diferido como evolución. |

### 2.4 Subsistema Herramientas

| Componente | Nivel | Justificación |
|---|---|---|
| **MCP Servers** | Núcleo | (1) MCP Device Management — estado, historial y comandos sobre DER; (2) MCP Blockchain Gateway — crear propuesta, consultar firmas, ejecutar acción aprobada. Herramientas estandarizadas con identidad Zero Trust. |
| **Agent Registry (A2A)** | Diferido | Con 4 agentes fijos en un workflow determinista no se requiere descubrimiento dinámico; se incorpora cuando aparezcan nuevos agentes. |

### 2.5 Subsistema Gateway de Modelos

| Componente | Nivel | Justificación |
|---|---|---|
| **Model Gateway** | Caso ★ | Rutea cada llamada al modelo adecuado (Frontier LLM / SLM), habilita el fallback de la degradación elegante y **reparte tráfico entre versiones de prompt** para pruebas A/B y canary (Tema 5). |
| **Guardrails** | Caso ★ | Políticas sobre la salida de cada agente: comandos prohibidos, parámetros físicos fuera de rango, datos sensibles, y no persuasión en el Assistant. Absorbe las funciones de Safety Model y Data Classification. Sus políticas se versionan y prueban junto con los prompts. |

### 2.6 Subsistema Gobierno y Operación

| Componente | Nivel | Justificación |
|---|---|---|
| **Prompt & Model Registry** | Núcleo ★ | Componente central del Tema 5. Cada prompt es código: versión, dueño, modelo asociado, estado (borrador / en prueba / activo / retirado) y rollback. La versión activa queda referenciada en cada propuesta on-chain. |
| **Evals & Tracing** | Núcleo ★ | Pruebas automáticas de cada versión de prompt contra un conjunto de diagnósticos de referencia antes de publicarla; trazas OTLP de cada paso del flujo; métricas de calidad, groundedness y latencia por versión. |

### 2.7 Subsistema Modelos

| Componente | Nivel | Justificación |
|---|---|---|
| **Frontier LLM** | Núcleo | Agente Diagnosticador y Agente de Acción: razonamiento complejo sobre anomalías y fallos documentados. Contratado en la nube. |
| **Embedding Model** | Núcleo | Vectores para la Ingestion Pipeline y las consultas del RAG. |
| **Small Language Model** | Caso | Agente Monitor, Agente Notificador y Assistant: tareas más simples donde un SLM reduce latencia y costo. |
| **Predictive Models** | Caso | ML clásico para detección de anomalías en series de estado de los DER; el Agente Monitor los invoca para decidir si una lectura es anómala. |
| **Reranker** | Diferido | Se incorpora si el experimento muestra baja precisión en la recuperación. |
| **Safety Model** | Diferido | Su función se cubre como política de Guardrails. |
| **PII Detector** | Diferido | El estado del dispositivo no es dato personal; la minimización del consumo se cubre con una política de Guardrails. |
| **ML Platform** | Diferido | El ciclo de vida de los Predictive Models (reentrenamiento, drift) queda fuera de esta entrega. |

### 2.8 Subsistema Datos y Eventos

| Componente | Nivel | Justificación |
|---|---|---|
| **Event Bus** | Caso (existente E3) | Kafka / Event Mesh de la E3 con los nuevos tópicos `der.estado`, `der.alertas` y `der.gobernanza`. |
| **Systems of Record** | Caso | Registro maestro de dispositivos (titular, modelo, firmware, ubicación) e historial de acciones; contexto para cada diagnóstico. |
| **Data Warehouse** | Diferido | Solo alimenta el reentrenamiento de modelos, fuera del alcance. |

### 2.9 Subsistema Sistemas Existentes

| Componente | Nivel | Justificación |
|---|---|---|
| **Core Business Services** | Caso | MS Control DER (nuevo), MS Telemetría Core (E3) y MS Notificaciones (E2), expuestos como herramientas vía MCP. |
| **Rules Engine** | Caso | Umbrales de severidad por tipo de dispositivo, tiempos de espera por firma según criticidad y vigencia (TTL) de cada propuesta. Todas las acciones requieren 3/3 firmas. |
| **Process Engine** | Diferido | La escalación al mismo firmante la resuelven los eventos del contrato y el Agente Notificador. |
| **CRM** | Fuera | El flujo opera sobre dispositivos, no sobre gestión comercial del cliente. |
| **ERP · Payments** | Fuera | Un eventual costo de despacho técnico es un flujo posterior. |
| **Document Management** | Fuera | Los documentos técnicos ya están indexados en el RAG. |

### 2.10 Eje Transversal — Seguridad, Privacidad y Responsible AI

| Componente | Nivel | Justificación |
|---|---|---|
| **Identity (IAM)** | Caso | Identidad Zero Trust para cada agente, MCP Server, dispositivo y firmante humano; las firmas se vinculan a identidades verificadas. |
| **Secrets & Keys** | Caso | Llaves de firma del contrato multi-firma, tokens de los MCP Servers y credenciales de dispositivos. |
| **Audit & Retention** | Caso | La traza on-chain es el registro inmutable; junto con el tracing da a la CREG trazabilidad desde la anomalía hasta el comando, incluida la versión de prompt usada. |
| **FinOps (AI cost)** | Caso ★ | Costo por diagnóstico y por conversación, comparado entre versiones de prompt y modelos. |
| **Responsible AI** | Caso | Explicabilidad (cada diagnóstico cita su evidencia), no persuasión en el Assistant, equidad y supervisión humana vía multi-firma. Banda transversal exigida por la plantilla. |
| **Data Classification** | Diferido | Se aplica como regla dentro de Guardrails. |

### 2.11 Decisiones de arquitectura y alternativas descartadas

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| MCP como contrato entre agentes y microservicios | Que los agentes llamen directo a las APIs REST | Herramientas estandarizadas y reutilizables, con identidad Zero Trust en cada llamada. |
| Workflow con secuencia definida (Monitor → Diagnosticador → Acción → Notificador) | Agente supervisor que decide el siguiente paso | Flujo predecible y auditable; sin riesgo de ciclos entre agentes. |
| Model Gateway con varios modelos (Frontier LLM + SLM) | Un solo proveedor y modelo | Costo, fallback ante fallas, latencia y pruebas A/B de versiones. |
| RAG sobre índice vectorial | Fine-tuning de un modelo con los manuales | Los diagnósticos citan su fuente y actualizar el conocimiento es barato. |
| Guardrails antes de enviar la propuesta al contrato | Confiar solo en las firmas humanas | Defensa en profundidad: un comando inventado no llega a la firma. |
| Prompts versionados en un registro, desacoplados del código de los agentes | Prompts embebidos en el código de cada agente | Cambiar un prompt no exige redesplegar; rollback inmediato; trazabilidad de qué versión produjo cada propuesta (★ Tema 5). |
| Assistant sin herramientas de acción | Assistant que también permite firmar desde el chat | La firma es un acto explícito y separado; la conversación no puede inducir una aprobación. |

### 2.12 Resumen de la selección

| Nivel | Cantidad aprox. |
|---|---|
| Núcleo | 11 |
| Caso | 19 |
| Diferido / fuera | 12 (más el grafo de conocimiento, diferido dentro de Vector Index & Graph) |

---

## 3. Catálogo de servicios — nuevos, modificados y retirados (lámina 03)

La columna **«De qué módulo depende»** indica sobre qué entrega anterior del curso se apoya cada servicio: **M1** Modelado (ArchiMate y DDD: bounded contexts), **M2** Microservicios y APIs (service mesh, API Gateway, apps), **M3** Arquitectura orientada a eventos (Kafka / Event Mesh), **M4** Blockchain (red permisionada, contratos, gateway de cadena) y **M5** IA generativa y agéntica (esta entrega). Muestra que la IA se monta sobre lo construido y no es una aplicación aparte.

| Servicio | Nuevo / modificado / retirado | Atributo de calidad que lo justifica | De qué módulo depende |
|---|---|---|---|
| Agentes Monitor, Diagnosticador, Acción y Notificador | Nuevo | Latencia: detección de anomalías de 45 min a < 5 min | M5 · sobre eventos M3 |
| Assistant de revisión previa a la firma | Nuevo | Seguridad / Responsible AI: consentimiento informado, sin herramientas de acción | M5 · M2 |
| MS Control DER | Nuevo | Seguridad: comando autenticado y no repudiable; disponibilidad con fail-safe | M1 (BC) · M2 |
| MCP Servers (Device Management, Blockchain Gateway) | Nuevo | Facilidad de integración: herramientas estándar; Zero Trust en cada llamada | M2 · M4 |
| Contrato MultiFirmaAccionDER (3/3) | Nuevo | Seguridad: no repudio y trazabilidad del 100 % de las acciones | M4 |
| Knowledge Service (RAG) + Ingestion Pipeline | Nuevo | Explicabilidad: cada diagnóstico cita su fuente; modificación sin reentrenar | M5 |
| Prompt & Model Registry + Evals & Tracing | Nuevo | Facilidad de modificación: cambio de prompt sin redesplegar y rollback inmediato | M5 · Tema 5 |
| Gateway de cadena | Modificado | Facilidad de integración: reutiliza la red permisionada existente | M4 |
| MS Notificaciones | Modificado | Latencia de respuesta del firmante: plantillas de firma con enlace directo | M2 |
| Event Mesh / Kafka (tópicos `der.*`) | Modificado | Escalabilidad y disponibilidad: desacople por eventos | M3 |
| App móvil y consola de operaciones | Modificado | Seguridad: pantalla de revisión, conversación y firma explícita | M2 |
| Revisión manual de dashboards de dispositivos | Retirado | Latencia: la reemplaza el Agente Monitor | M3 · M5 |
| Triage manual de alarmas sin contexto | Retirado | Disponibilidad operativa: lo reemplaza el diagnóstico con evidencia | M5 |

> Las vistas ArchiMate extendidas (motivación, negocio, aplicación y tecnología) se exportan desde Archi.

---

## 4. Tema de investigación — Tema 5: Prompt Engineering a gran escala y versionamiento (lámina 04)

### 4.1 Ciclo de vida de los prompts

Cada prompt de los agentes y del Assistant se trata como código:

1. **Autoría** — el prompt vive en un repositorio con plantilla fija y cambia por pull request con revisión por pares.
2. **Evaluación** — Evals contra un conjunto de diagnósticos de referencia: ≥ 85 % de acierto, ≥ 95 % de citas válidas y 0 comandos inventados.
3. **Registro** — Prompt & Model Registry: versión semántica, modelo asociado y estado (borrador → en prueba → activa → retirada).
4. **Despliegue gradual** — Model Gateway: modo sombra → 5 % del tráfico → 100 %.
5. **Observación** — Evals & Tracing en línea y FinOps (costo) por versión.
6. **Auditoría** — la versión de prompt y modelo queda en la propuesta on-chain.

**Rollback:** si una métrica cae bajo el umbral en la observación, se vuelve de inmediato a la versión anterior del registro.

### 4.2 Control según el riesgo de cada familia de prompts

| Familia | Riesgo | Por qué | Control exigido |
|---|---|---|---|
| Agente de Acción | Alto | Impacto físico | Evaluación estricta + aprobación de Operaciones; canary obligatorio, sin A/B; Guardrails versionados con el prompt |
| Agente Diagnosticador | Alto | Fundamenta la decisión de los firmantes | Evaluación de acierto y citas; revisión experta de casos dudosos; canary antes del 100 % |
| Agente Monitor | Medio | Volumen alto, usa SLM | Evaluación de falsos positivos; costo por evento (FinOps); despliegue gradual |
| Notificador y Assistant | Bajo / medio | Dos audiencias (prosumidor y operador) | Prompts por audiencia; A/B permitido (claridad, tono); prueba de no persuasión |

### 4.3 Decisiones de diseño que impone el tema

| Decisión que impone el tema | Alternativa descartada | Trade-off: qué se gana y qué se paga |
|---|---|---|
| **1 · Prompts como código:** repositorio con plantilla fija (rol, instrucciones, esquema de salida, ejemplos); solo cambia por pull request revisado. | Prompts embebidos en el código de cada agente o editados a mano en una consola. | Gana: trazabilidad de quién cambió qué, revisión por pares y reproducibilidad. Paga: cada cambio tarda más por la revisión. |
| **2 · Registro versionado:** versión semántica (mayor, menor, corrección), modelo asociado y estado; los agentes leen la versión activa en ejecución. | Una sola versión «vigente» que se sobrescribe, o la versión atada al despliegue del agente. | Gana: rollback inmediato y cambio de prompt sin redesplegar. Paga: un componente crítico más que operar y mantener disponible. |
| **3 · Evaluación obligatoria antes de publicar:** pasa solo con ≥ 85 % de acierto, ≥ 95 % de citas válidas y 0 comandos inventados. | Pruebas manuales o validación directa en producción. | Gana: los retrocesos de calidad se detectan antes de llegar al operador o al prosumidor. Paga: construir y mantener el conjunto de referencia y el costo en tokens de cada corrida. |
| **4 · Despliegue gradual con el Model Gateway:** sombra → 5 % → 100 %; A/B solo para el Notificador y el Assistant. | Cambiar la versión para todo el tráfico de una vez. | Gana: el impacto de un error queda acotado y hay evidencia real antes de publicar. Paga: dos versiones conviviendo y un enrutamiento más complejo. |
| **5 · Control según el riesgo del agente:** el Agente de Acción exige evaluación estricta y aprobación de Operaciones; el Notificador solo la evaluación automática. | El mismo flujo de aprobación para todos los prompts. | Gana: el rigor se concentra donde hay impacto físico. Paga: cambiar el Agente de Acción es más lento. |
| **6 · Versión de prompt y modelo en cada propuesta on-chain** del contrato multi-firma. | Registrar la versión solo en los logs internos. | Gana: el regulador verifica de forma inmutable qué versión produjo cada recomendación. Paga: bytes extra por transacción y obligación de conservar las versiones retiradas. |
| **7 · Prompts del Assistant separados por audiencia,** con pruebas de no persuasión y de tono. | Un único prompt conversacional genérico. | Gana: consentimiento más informado y explicaciones comprensibles. Paga: el doble de prompts que mantener y evaluar. |
| **8 · Políticas de Guardrails versionadas junto con los prompts:** se publican y se revierten en conjunto. | Políticas fijas gestionadas aparte de los prompts. | Gana: prompt y política cambian de forma coherente. Paga: hay que evaluar las combinaciones prompt + política. |

---

## 5. Resumen de trazabilidad tecnológica

| Componente | Rol en el flujo |
|---|---|
| **RAG** | Fundamenta cada diagnóstico y cada respuesta del Assistant en documentación real (fichas técnicas, regulación CREG, históricos de fallos). |
| **AI Flows** | 4 agentes orquestados: Monitor → Diagnosticador → Acción → Notificador. |
| **Assistant** | Conversación acotada al diagnóstico antes de firmar; informa, no decide. |
| **MCP Server (Devices)** | Herramientas de estado y comandos para los agentes, con identidad Zero Trust. |
| **MCP Server (Blockchain)** | Herramientas para crear propuestas, consultar firmas y ejecutar acciones aprobadas. |
| **Prompt & Model Registry** | Versionamiento, pruebas y rollback de los prompts de todos los agentes (Tema 5). |
| **Contrato multi-firma** | Gobernanza 3/3 — ningún comando se ejecuta sin IA + operador + prosumidor. |
| **Zero Trust** | Identidad por servicio, por dispositivo y por firmante en cada paso del flujo. |
