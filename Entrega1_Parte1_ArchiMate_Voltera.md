# PARTE 1 — ArchiMate: Cinco Vistas de Voltera

> Un solo modelo, cinco recortes. Cada vista responde una pregunta distinta y se apoya en la anterior.

---

## VISTA 01 · Motivación — ¿Por qué existe esta arquitectura?

**Propósito:** Encadenar stakeholder → driver → meta → outcome, con requisitos y restricciones.  
**Audiencia:** Junta directiva, CEO, inversionistas.

### Stakeholders

| Elemento (tipo) | Descripción |
|---|---|
| CEO / Junta directiva (stakeholder) | Crecimiento, time-to-market, continuidad del negocio |
| CTO / Arquitectura (stakeholder) | Sostenibilidad técnica, evolvabilidad, control de costos |
| CISO (stakeholder) | Seguridad de infraestructura crítica, integridad de comandos |
| Mesa de energía / Trading (stakeholder) | Precisión de precios, pronóstico y liquidación del mercado |
| Operador de red / Distribuidora (stakeholder) | Disponibilidad de telemetría y gestión de dispositivos |
| Regulador — CREG (stakeholder) | Cumplimiento de AMI, generación distribuida, comunidades energéticas |
| Prosumidores y hogares (stakeholder) | Visibilidad en tiempo real, transparencia, venta de excedentes |
| Comunidades energéticas (stakeholder) | Reglas de reparto propias, operación autónoma |

### Drivers (condiciones que empujan el cambio)

| Elemento (tipo) | Descripción |
|---|---|
| Medición avanzada AMI (driver) | Resolución CREG 101-001 de 2022 obliga medidores inteligentes bidireccionales |
| Generación distribuida (driver) | Prosumidores producen energía con paneles y baterías |
| Comunidades energéticas habilitadas (driver) | Ley 2294/2023 y Decreto 2236/2023 permiten asociarse para comercializar |
| Captación agresiva de clientes (driver) | De 0 a 2M de medidores en 24 meses; +80.000 altas/mes |
| Operación sobre infraestructura crítica (driver) | Un fallo puede dejar sin energía a una comunidad o enviar comando peligroso |
| Expansión regional (driver) | Cuatro países en 36 meses sobre la misma plataforma |

### Assessments (análisis del estado frente a los drivers)

| Elemento (tipo) | Descripción |
|---|---|
| El modelo eléctrico unidireccional es obsoleto (assessment) | Facturación mensual y medición manual no soportan prosumidores ni P2P |
| No existe plataforma de datos y control en tiempo real (assessment) | Voltera es greenfield: no hay sistemas legados pero tampoco capacidad instalada |
| Falta un registro verificable de certificados (assessment) | Sin blockchain, los certificados de origen son duplicables y no auditables |

### Goals (metas de alto nivel)

| Elemento (tipo) | Descripción |
|---|---|
| Visibilidad del consumo en tiempo real (goal) | El prosumidor ve su energía casi al instante |
| Comercialización P2P de energía entre vecinos (goal) | Emparejar oferta y demanda de excedentes de forma continua |
| Liquidación confiable y auditable (goal) | Toda transacción es trazable y reconstruible al 100% |
| Operar 500 comunidades energéticas activas (goal) | Con reglas configurables de reparto por comunidad |
| Certificados de origen verificables y no duplicables (goal) | Registrar cada kWh limpio sin doble conteo |
| Seguridad Zero Trust en infraestructura crítica (goal) | Nunca confiar, siempre verificar: identidad fuerte por dispositivo |

### Outcomes (resultados medibles que concretan las metas)

| Elemento (tipo) | Descripción |
|---|---|
| 2M de medidores conectados en 24 meses (outcome) | Meta de captación principal |
| USD 180M de ingresos anualizados en 36 meses (outcome) | Crecimiento ~12% mensual |
| Retención anual ≥ 92% (outcome) | Cero incidentes graves de seguridad o facturación |
| Telemetría visible en app en p95 ≤ 500ms (outcome) | Latencia de punta a punta |
| Disponibilidad ≥ 99,99% en telemetría y facturación (outcome) | ≈ 4 min de indisponibilidad/mes |
| Emparejamiento P2P en p95 ≤ 300ms (outcome) | Liquidez del mercado entre pares |

### Requirements (requisitos)

| Elemento (tipo) | Descripción |
|---|---|
| Ingesta sostenida ≥ 2.200 lecturas/s sin pérdida (requirement) | Telemetría masiva de 2M medidores cada 15 min |
| Comandos autenticados, firmados y no repudiables (requirement) | Control seguro de dispositivos físicos |
| APIs versionadas y estables para web y móvil (requirement) | Sin romper apps ya instaladas en millones de hogares |
| Modo offline en app móvil con sincronización posterior (requirement) | Consultar última lectura sin conexión |
| Identidad fuerte por dispositivo IoT (requirement) | Credenciales rotables, revocación ≤ 5 min |

### Constraints (restricciones)

| Elemento (tipo) | Descripción |
|---|---|
| Cumplir habeas data — Ley 1581/2012 (constraint) | Dato de consumo revela hábitos del hogar |
| Cumplir PCI-DSS para pagos (constraint) | Cobro de facturas y pago de excedentes |
| Regulación CREG evolutiva (constraint) | Las reglas de AMI y comunidades siguen madurando |
| Cloud-native y contenedores (constraint) | Portabilidad, escalamiento elástico, PoC repetibles |
| Diseño incremental con PoC por módulo (constraint) | Evidencia temprana y continua, no solo documentación |

### Relaciones clave de la vista

```
CEO/Junta ──(has interest in)──► Captación agresiva (driver)
CISO ──(has interest in)──► Infraestructura crítica (driver)
Regulador CREG ──(has interest in)──► AMI + Comunidades energéticas (drivers)
Prosumidores ──(has interest in)──► Generación distribuida (driver)

Captación agresiva ──(influences)──► Visibilidad tiempo real (goal)
Infraestructura crítica ──(influences)──► Seguridad Zero Trust (goal)
Comunidades habilitadas ──(influences)──► Operar 500 comunidades (goal)

Visibilidad tiempo real ──(realized by)──► Telemetría p95 ≤ 500ms (outcome)
Comercialización P2P ──(realized by)──► Emparejamiento p95 ≤ 300ms (outcome)
Seguridad Zero Trust ──(realized by)──► Retención ≥ 92% (outcome)

Goals ──(realized by)──► Requirements
Goals ──(constrained by)──► Constraints
```

---

## VISTA 02 · Estrategia — ¿Con qué medios?

**Propósito:** Mostrar capacidades, recursos y el flujo de valor sin mencionar software.  
**Audiencia:** Operaciones, dirección de producto, planeación estratégica.

### Capabilities (de qué es capaz la empresa)

| Capability | Qué resuelve |
|---|---|
| Medición y telemetría (AMI/IoT) | Ingesta, validación y almacenamiento de lecturas de millones de medidores |
| Facturación y liquidación | Cálculo de consumo neto, tarifa dinámica, excedentes y emisión de factura |
| Mercado P2P de energía | Emparejamiento oferta/demanda entre prosumidores y liquidación de cada transacción |
| Gestión de activos distribuidos (DER) | Registro, monitoreo y control de paneles, baterías y cargadores EV |
| Pronóstico de demanda y generación | Predicción de consumo y generación solar para operar el sistema y fijar precios |
| Respuesta a la demanda (flexibilidad) | Señales y comandos para desplazar consumo o descargar baterías en momentos críticos |
| Certificados de origen renovable | Emisión, transferencia y verificación de certificados de energía limpia |
| Comunidades energéticas | Alta y operación de comunidades con reglas de reparto configurables |
| Cumplimiento y reportes al mercado | Reportes al regulador, trazabilidad y auditoría |

### Resources (activos que sostienen las capabilities)

| Resource | Descripción |
|---|---|
| Plataforma cloud-native multi-región | Infraestructura elástica, multi-zona, contenedores |
| Equipo de ciencia de datos y energía | Pronóstico, optimización, modelos de IA |
| Flota de medidores inteligentes | 2M de dispositivos AMI con comunicación bidireccional |
| Dispositivos DER (baterías, inversores, cargadores) | Activos distribuidos controlables |
| Ledger compartido (blockchain) | Registro verificable para certificados y liquidación P2P |
| API Gateway y malla de servicios | Exposición y gobierno de APIs |

### Course of Action (estrategia elegida)

| Course of action | Descripción |
|---|---|
| Plataforma de datos, control y liquidación en tiempo real | Voltera apuesta a que el ganador es quien tenga la plataforma más rápida, confiable y segura |
| Energía distribuida con mercado P2P y certificados verificables | El diferenciador: que los excedentes de un techo solar alimenten al vecino con registro auditable |
| Seguro embebido de seguridad Zero Trust | Cada componente, dispositivo y agente se autentica con mínimo privilegio |

### Value Stream (flujo de valor de punta a punta)

```
┌──────────┐    ┌──────────────┐    ┌────────────┐    ┌──────────────┐    ┌───────────────┐    ┌──────────────┐
│  Medir   │───►│  Facturar y  │───►│ Comerciar  │───►│  Controlar   │───►│  Certificar   │───►│   Operar     │
│(telemetr)│    │  Liquidar    │    │    P2P     │    │ dispositivos │    │    origen     │    │  comunidad   │
└──────────┘    └──────────────┘    └────────────┘    └──────────────┘    └───────────────┘    └──────────────┘
     │                 │                  │                  │                    │                    │
     ▼                 ▼                  ▼                  ▼                    ▼                    ▼
Medición y         Facturación        Mercado P2P       Gestión DER         Certificados        Comunidades
telemetría         y liquidación      de energía        + Respuesta         de origen           energéticas
(capability)       (capability)       (capability)      demanda (cap.)      (capability)        (capability)
```

### Relaciones clave

```
Cada etapa del value stream ──(served by)──► una o más capabilities
Cada capability ──(realized by)──► resources
Course of action ──(realizes)──► Goals (de la vista de motivación)
```

---

## VISTA 03 · Negocio — ¿Qué hace la empresa?

**Propósito:** Servicios de negocio, procesos, roles y objetos — sin referencia a tecnología.  
**Audiencia:** Producto, operaciones, negocio.  
**Prueba:** Si mañana cambiara todo el software, esta vista seguiría siendo válida.

### Business Services (expuestos al cliente/usuario)

| Business Service | A quién sirve |
|---|---|
| Monitoreo de consumo y generación en tiempo real | Prosumidor (hogar/pyme) |
| Facturación y cobro de energía | Prosumidor, empresa |
| Venta de excedentes al vecino (mercado P2P) | Prosumidor con generación solar |
| Control de dispositivos (batería, cargador EV) | Prosumidor |
| Operación de comunidad energética | Gestor de comunidad, miembros |
| Emisión de certificado de origen renovable | Prosumidor, certificadora, empresa |
| Respuesta a la demanda (flexibilidad) | Agregador de flexibilidad, operador de red |
| Alta y autoservicio del prosumidor | Prosumidor nuevo |
| Reporte regulatorio y de mercado | Regulador CREG, operador del mercado |

### Business Processes (cadenas que realizan los servicios)

| Proceso | Secuencia |
|---|---|
| Ingesta y validación de telemetría | Recibir lectura → Validar integridad → Almacenar → Notificar |
| Facturación mensual | Calcular consumo neto → Aplicar tarifa → Valorar excedentes → Emitir factura → Cobrar |
| Emparejamiento P2P | Publicar excedente → Emparejar oferta/demanda → Liquidar transacción → Registrar |
| Control de dispositivo | Detectar señal/evento → Evaluar regla → Enviar comando → Confirmar ejecución |
| Alta de comunidad | Registrar comunidad → Definir reglas de reparto → Asociar miembros → Activar operación |
| Emisión de certificado | Verificar generación limpia → Emitir certificado → Registrar en ledger |
| Respuesta a la demanda | Detectar pico/señal de precio → Seleccionar dispositivos → Enviar comando → Compensar usuario |
| Gestión de siniestro de red | Detectar corte → Notificar afectados → Encolar lecturas → Reconciliar al restaurar |

### Business Roles y Actors

| Rol / Actor | Responsabilidad |
|---|---|
| Prosumidor (actor) | Consume, produce y vende energía |
| Gestor de comunidad (rol) | Administra reglas y miembros de la comunidad |
| Mesa de energía / Trading (rol) | Opera el mercado, fija precios, gestiona riesgo |
| Operaciones de red y campo (rol) | Monitorea telemetría, gestiona dispositivos, detecta fallas |
| Operador de red / Distribuidora (actor externo) | Opera la red física, aporta topología y calidad |
| Regulador CREG (actor externo) | Fija reglas, exige reportes |
| Agregador de flexibilidad (actor externo) | Consume capacidad de respuesta a la demanda |
| Certificadora de origen (actor externo) | Valida y reconoce certificados de energía limpia |

### Business Objects (conceptos del dominio)

| Business Object | Descripción |
|---|---|
| Lectura de medidor | Dato de consumo/generación con timestamp, validación y origen |
| Factura | Liquidación del período con consumo neto, tarifa, excedentes |
| Transacción P2P | Intercambio de energía entre pares con precio y cantidad |
| Certificado de origen | Registro verificable de kWh generado de forma limpia |
| Comunidad energética | Agrupación con reglas de reparto y miembros asociados |
| Dispositivo DER | Panel, batería o cargador con estado y comandos |
| Comando de control | Instrucción firmada y autenticada enviada a un dispositivo |
| Contrato de servicio | Acuerdo entre Voltera y el prosumidor/empresa |
| Tarifa | Esquema de precio (puede ser dinámica por horas) |
| Excedente | Energía producida y no consumida, disponible para venta |

### Relaciones clave

```
Monitoreo tiempo real (service) ──(realized by)──► Ingesta y validación de telemetría (process)
Ingesta de telemetría (process) ──(assigned to)──► Operaciones de red (rol)
Ingesta de telemetría (process) ──(accesses)──► Lectura de medidor (object)

Venta excedentes P2P (service) ──(realized by)──► Emparejamiento P2P (process)
Emparejamiento P2P (process) ──(accesses)──► Transacción P2P (object)
Emparejamiento P2P (process) ──(assigned to)──► Prosumidor (actor)
```

---

## VISTA 04 · Aplicación — ¿Qué software lo apoya?

**Propósito:** Componentes de software, servicios de aplicación, objetos de datos y la cadena de realización hacia el negocio.  
**Audiencia:** CTO, equipos de desarrollo, arquitectos de software.

### Application Components

| Componente | Responsabilidad |
|---|---|
| Servicio de Telemetría (Ingesta) | Recibe, valida y almacena lecturas de medidores a alta velocidad |
| Motor de Facturación | Calcula consumo neto, aplica tarifa, emite factura |
| Motor de Mercado P2P | Empareja ofertas y demandas, liquida transacciones |
| Servicio de Control de Dispositivos | Envía y confirma comandos a baterías, inversores y cargadores |
| Servicio de Pronóstico | Predice demanda y generación solar con modelos de ML/IA |
| Servicio de Comunidades | Gestiona alta, reglas de reparto y operación de comunidades |
| Servicio de Certificados de Origen | Emite, transfiere y verifica certificados en el ledger |
| Servicio de Respuesta a la Demanda | Orquesta señales de flexibilidad y compensaciones |
| Servicio de Identidad y Autenticación | Identidad de usuarios y dispositivos, credenciales, revocación |
| API Gateway | Punto de entrada unificado, rate limiting, versionado |
| BFF Web | Back-end for frontend de la experiencia web (operación, gestión) |
| BFF Móvil | Back-end for frontend de la app del prosumidor (streaming, push) |
| Servicio de Notificaciones | Push, alertas de precio, eventos de red |
| Servicio de Reportes Regulatorios | Genera reportes para CREG y operador del mercado |
| Copiloto de Energía (IA) | Asistente que explica facturas, recomienda consumo, optimiza batería |

### Application Services (APIs/comportamientos expuestos)

| Servicio de aplicación | Expuesto por |
|---|---|
| API de Telemetría (ingesta streaming) | Servicio de Telemetría |
| API de Facturación | Motor de Facturación |
| API de Mercado P2P (publicar/emparejar) | Motor de Mercado P2P |
| API de Control de Dispositivos (comandos) | Servicio de Control |
| API de Pronóstico | Servicio de Pronóstico |
| API de Comunidades | Servicio de Comunidades |
| API de Certificados | Servicio de Certificados |
| API de Flexibilidad | Servicio de Respuesta a la Demanda |
| API de Identidad (OAuth2/OIDC) | Servicio de Identidad |
| API del Copiloto (chat/recomendaciones) | Copiloto de Energía |

### Application Interfaces

| Interfaz | Canal |
|---|---|
| Endpoint REST/gRPC del Gateway | Todos los consumidores |
| WebSocket de telemetría en tiempo real | App móvil y web |
| Interfaz MQTT/AMQP para medidores | Dispositivos IoT |
| Webhook de eventos de red | Operador de red |

### Data Objects

| Objeto de datos | Componente que lo accede |
|---|---|
| Registro de lectura (time-series) | Servicio de Telemetría |
| Registro de factura | Motor de Facturación |
| Orden de mercado P2P | Motor de Mercado P2P |
| Perfil de dispositivo DER | Servicio de Control |
| Modelo de pronóstico (artefacto ML) | Servicio de Pronóstico |
| Token de certificado de origen | Servicio de Certificados |
| Perfil de usuario/dispositivo | Servicio de Identidad |
| Regla de reparto de comunidad | Servicio de Comunidades |

### Cadena de realización (Aplicación → Negocio)

```
Servicio de Telemetría ──(realizes)──► Monitoreo de consumo en tiempo real (business service)
Motor de Facturación ──(realizes)──► Facturación y cobro de energía (business service)
Motor de Mercado P2P ──(realizes)──► Venta de excedentes al vecino (business service)
Servicio de Control ──(realizes)──► Control de dispositivos (business service)
Servicio de Comunidades ──(realizes)──► Operación de comunidad energética (business service)
Servicio de Certificados ──(realizes)──► Emisión de certificado de origen (business service)
Servicio de Resp. Demanda ──(realizes)──► Respuesta a la demanda (business service)
Servicio de Reportes ──(realizes)──► Reporte regulatorio (business service)
```

---

## VISTA 05 · Integración — ¿De quién dependemos?

**Propósito:** Terceros externos, las interfaces que exponen, quién las consume internamente y qué se degrada cuando fallan.  
**Audiencia:** CTO, CISO, operaciones, SRE.  
**Lectura:** De abajo (terceros) hacia arriba (lo nuestro).

### Mapa de dependencias externas

| Tercero (business actor externo) | Interfaz que expone | Componente propio que la consume | Qué se degrada si falla |
|---|---|---|---|
| Medidores inteligentes (AMI/IoT) — fabricantes diversos | Protocolo MQTT/DLMS/COSEM (technology interface) | Servicio de Telemetría | **Visibilidad en tiempo real** — se pierde la lectura y el monitoreo del prosumidor queda desactualizado |
| Dispositivos DER (baterías, inversores, cargadores) | API propietaria / Modbus / OCPP (technology interface) | Servicio de Control de Dispositivos | **Control de dispositivos y respuesta a demanda** — el dispositivo debe quedar en estado seguro (fail-safe) |
| Operador de red / Distribuidora | API de topología, cortes y calidad (application interface) | Servicio de Telemetría + Servicio de Reportes | **Detección de fallas y conciliación** — telemetría sigue midiendo pero no se detectan cortes de red |
| Operador del mercado mayorista | API de precios, despacho y liquidación (application interface) | Motor de Facturación + Motor de Mercado P2P | **Liquidación y fijación de precios** — se encola y se reconcilia al restaurar; la facturación se demora |
| Datos abiertos (clima y radiación solar) | API REST de datos meteorológicos (application interface) | Servicio de Pronóstico | **Pronóstico de generación** — degradación elegante con cacheo; se usa último pronóstico válido |
| Ledger de certificados de origen (blockchain) | Smart contract / API del nodo (technology interface) | Servicio de Certificados | **Emisión y transferencia de certificados** — se encolan emisiones hasta que el ledger responda |
| Pasarelas de pago y bancos | API de pagos PSP (application interface) | Motor de Facturación | **Cobro de facturas y pago de excedentes** — idempotencia, reintentos, conciliación posterior |
| Regulador CREG y mercado | API/portal de reportes (application interface) | Servicio de Reportes Regulatorios | **Cumplimiento regulatorio** — los reportes se generan y se envían al restablecer; riesgo de incumplimiento temporal |
| Proveedores de IA / LLM | API de modelos (GPT, embeddings) (application interface) | Copiloto de Energía | **Copiloto/asistente** — funcionalidad de IA no disponible; el resto opera normal |
| Certificadoras de origen renovable | API de validación (application interface) | Servicio de Certificados | **Reconocimiento de certificados** — emisión funciona pero validación externa queda pendiente |
| Agregadores de flexibilidad | API de eventos de flexibilidad (application interface) | Servicio de Respuesta a la Demanda | **Participación en mercado de flexibilidad** — Voltera no puede ofrecer capacidad al agregador |

### Patrón de resiliencia por dependencia

```
Para CADA tercero:
  Tercero (actor externo)
      │
      ▼
  Interfaz externa (app/tech interface)
      │  ──[serving]──►
      ▼
  Componente propio (application component)
      │
      ▼
  Servicio de negocio afectado (business service)
      │
      ▼
  Proceso de negocio degradado → Usuario afectado
```

### Estrategias de mitigación (implícitas en el diseño)

| Patrón | Aplicado a |
|---|---|
| Circuit breaker + backoff exponencial | Todas las dependencias externas |
| Encolamiento y almacenamiento local | Telemetría (medidores con buffer), pagos, reportes |
| Fail-safe (estado seguro ante fallo) | Control de dispositivos DER |
| Degradación elegante | Pronóstico (usa último válido), copiloto IA (desactiva feature) |
| Idempotencia y conciliación | Pagos, liquidación de mercado, reportes regulatorios |
| Timeout duro ≤ 800ms por dependencia | Presupuesto de latencia global |
| Cacheo con TTL | Datos de clima, topología de red, precios del mercado |

---

## Consistencia del modelo

> Las cinco vistas comparten UN SOLO modelo. El mismo "Servicio de Telemetría" aparece en:
> - Vista 4 (Aplicación) como **application component**
> - Vista 5 (Integración) como el componente que **consume** la interfaz del medidor
> - Vista 3 (Negocio) como el realizador del **business service** "Monitoreo en tiempo real"
> - Vista 2 (Estrategia) porque realiza la **capability** "Medición y telemetría"
> - Vista 1 (Motivación) porque esa capability sirve al **goal** "Visibilidad del consumo en tiempo real"
>
> Cambiar de vista es recortar el modelo, no rehacerlo.

---

## Nota para diagramar en Archi

Para construir los diagramas en la herramienta Archi:

1. **Crear todos los elementos primero** en el árbol del modelo (no en las vistas)
2. **Crear las relaciones** entre elementos en el árbol
3. **Crear 5 viewpoints** (vistas) y arrastrar a cada una solo los elementos relevantes
4. **El mismo elemento aparece en múltiples vistas** — eso garantiza consistencia
5. **Usar colores por capa**: motivación (lila), estrategia (beige), negocio (amarillo), aplicación (azul), tecnología (verde)
6. **Rotular el tipo entre paréntesis** cuando no sea evidente por el ícono


---

## CONTEXTO DE SESIÓN — Estado de trabajo actual

> Esta sección documenta el estado del trabajo realizado y pendiente para que una futura sesión continúe donde se quedó.

### Archivos en `/Modificados/`

| Archivo | Estado | Compatibilidad Lucidchart |
|---|---|---|
| `Voltera_01_Motivacion.drawio` | ✅ Ajustado con HTML labels + iconos Unicode | Parcial (sin iconos gráficos nativos) |
| `Voltera_01_Motivacion_Arbol.drawio` | ✅ Árbol jerárquico con imágenes Lucid embebidas | ✅ Funciona con imágenes Lucid |
| `Voltera_02_Estrategia.drawio` | ✅ Formato comprimido con `mxgraph.archimate3` | ⚠️ Iconos NO se ven en Lucid (sí en draw.io) |
| `Voltera_03_Negocio.drawio` | ✅ Ajustado con HTML labels + iconos Unicode | Parcial (sin iconos gráficos nativos) |

### Problema principal pendiente: Iconos en Lucidchart

**El problema:** Lucidchart NO renderiza los stencils `mxgraph.archimate3.*` al importar archivos `.drawio`. Los iconos de ArchiMate (Capability ⊞, Resource ☰, Value Stream ≫, Course of Action ⚿, etc.) solo se ven en draw.io.

**Intentos realizados (ninguno funcionó en Lucid):**
1. `mxgraph.archimate3` shapes comprimidas → Lucid muestra rectángulos sin iconos
2. SVG data URIs embebidos → Lucid no los renderiza
3. Imágenes de `images.lucid.app` como sub-elementos → Funcionan SOLO si las URLs son válidas/accesibles
4. HTML labels con Unicode (⊞, ≫, ⚿) → El texto se muestra pero no como iconos gráficos bonitos

**Lo que SÍ funciona en Lucid:**
- El compañero creó su diagrama **directamente en Lucidchart** usando los stencils nativos de ArchiMate de Lucid (no importado desde drawio)
- El archivo original del compañero en la carpeta raíz (`Voltera_02_Estrategia.drawio`) no tiene imágenes de Lucid — es un drawio puro con rectángulos y texto Unicode

**Conclusión:** Para tener iconos ArchiMate en Lucidchart, hay que crear el diagrama directamente en Lucidchart usando su biblioteca nativa "ArchiMate 3.0", no importar desde draw.io.

### Semántica ArchiMate aplicada

**Capa de Motivación (color: `#CCCCFF` violeta claro):**
- Stakeholder: `appType=role;archiType=square`
- Driver: `appType=driver;archiType=square`
- Goal: `appType=goal;archiType=square`
- Assessment: `appType=assess;archiType=square`
- Outcome: `appType=outcome;archiType=square`
- Requirement: `appType=requirement;archiType=square`
- Constraint: `appType=constraint;archiType=square`

**Capa de Estrategia (color: `#F5DEAA` naranja/durazno):**
- Resource: `appType=resource;archiType=square`
- Capability: `appType=capability;archiType=rounded` (o `appType=comp;archiType=square` para grid)
- Value Stream: `appType=proc;archiType=rounded`
- Course of Action: `appType=course;archiType=rounded`

**Capa de Negocio (color: `#ffff99` amarillo):**
- Business Actor: `appType=actor;archiType=square`
- Business Role: `appType=role;archiType=square`
- Business Process: `appType=proc;archiType=rounded`
- Business Service: `appType=serv;archiType=rounded`
- Business Object: `shape=mxgraph.archimate3.businessObject;overflow=fill`
- Contract: `shape=mxgraph.archimate3.contract`
- Product: `shape=mxgraph.archimate3.product`

### Relaciones ArchiMate usadas

| Relación | Estilo draw.io | Uso |
|---|---|---|
| Association | `endArrow=none;` (línea sólida sin flechas) | Stakeholder↔Driver |
| Influence | `endArrow=open;endFill=0;dashed=1;dashPattern=6 4;` | Driver→Goal |
| Realization | `endArrow=block;endFill=0;dashed=1;dashPattern=8 4;` | Goal→Outcome, Service→Process |
| Assignment | `endArrow=block;endFill=1;startArrow=oval;startFill=1;` | Process→Role |
| Access | `endArrow=open;endFill=0;dashed=1;dashPattern=2 3;` | Process→Object |
| Composition | `startArrow=diamondThin;startFill=1;endArrow=none;` | CoA→Capability |
| Serving | `endArrow=open;endFill=1;dashed=1;dashPattern=6 4;` | Capability→VS, Resource→VS |
| Triggering | `endArrow=block;endFill=1;` | VS secuencia |

### Documentación DDD leída (carpeta `/DDD/`)

37 PDFs del curso ARTI4208 - Universidad de los Andes. Temas cubiertos:
1. **DDD Táctico**: Entities, Value Objects, Aggregates, Root Entity, 4 reglas de agregados
2. **Estilo Microservicios**: definición, arquitectura hexagonal, ventajas/desventajas
3. **Comunicación**: síncrona/asíncrona, request-response, pub/sub, colas, WebSocket
4. **Patrones**: 6 familias (descomposición, datos, composición, comunicación, observación, despliegue)
5. **APIs y Portafolio**: ciclo de vida, API Gateway, BFF, plataformas (desarrollo, ejecución, gestión)
6. **Service Mesh**: sidecars, plano control/datos, mTLS, Zero Trust, circuit breaker, distributed tracing

El texto extraído está en: `/DDD/DDD_contenido_completo.txt` (304K caracteres, 4505 líneas)

### Archivos clave del proyecto

```
/ArquitectutaNuevaGenegeracion/
├── Modificados/                    ← Archivos ajustados por mí
│   ├── Voltera_01_Motivacion.drawio
│   ├── Voltera_01_Motivacion_Arbol.drawio
│   ├── Voltera_02_Estrategia.drawio
│   └── Voltera_03_Negocio.drawio
├── Voltera_01_Motivacion.drawio    ← Original (comprimido, sin iconos Lucid)
├── Voltera_02_Estrategia.drawio    ← Original (comprimido, sin iconos Lucid)
├── Voltera_03_Negocio.drawio       ← Original (comprimido)
├── Voltera_Archi_Model/            ← Modelos Archi (.archimate)
├── DDD/                            ← Documentación del curso
│   └── DDD_contenido_completo.txt  ← Texto extraído de 37 PDFs
├── Entrega1_Parte1_ArchiMate_Voltera.md  ← ESTE ARCHIVO
└── ARTI4208-Proyecto-Entrega-1-v202602.pptx ← Instrucciones entrega
```

### Próximos pasos sugeridos

1. **Resolver iconos en Lucid**: La única solución real es recrear los diagramas directamente en Lucidchart usando su biblioteca ArchiMate nativa, o aceptar que los importados desde drawio no tendrán iconos gráficos.
2. **Vista 04 (Aplicación)** y **Vista 05 (Integración)**: Crear los archivos drawio para estas vistas.
3. **Aplicar documentación DDD**: Usar los conceptos de DDD táctico para diseñar los bounded contexts y agregados de Voltera en una vista adicional.
