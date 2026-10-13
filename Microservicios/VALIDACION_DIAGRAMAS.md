# Validación de los diagramas UML (04) y "Diagrama con Patrones"

Contraste de ambos diagramas de la lámina 04 contra el código real, con el mapeo de
nombres y el estado de cada elemento (cumple / equivalente / implementado ahora /
pendiente de alcance).

## Mapeo de topics (diagrama → broker real)

> **Actualizado (Opción B aplicada):** los topics del código ahora usan el estilo
> del diagrama (`topic.<dominio>.<evento>` con punto). Ya no hay traducción
> guion→punto: los nombres del broker COINCIDEN con el diagrama. Los 5 topics
> lógicos del diagrama se materializan en 13 canales físicos para no mezclar
> payloads heterogéneos en un mismo topic (ver `k8s/voltera-topics.yaml`).

| Topic del diagrama | Topic(s) real(es) en el código | Nota |
|---|---|---|
| `topic.telemetria.medidores` | `topic.telemetria.medidores`, `topic.telemetria.validadas`, `topic.telemetria.consumo` | ingesta/telemetría |
| `topic.tarifas.calculo` | `topic.tarifas.calculo`, `topic.tarifas.asignada` | tarifas consume consumo, emite tarifa |
| `topic.notificaciones` | `topic.notificaciones`, `topic.notificaciones.cliente`, `topic.notificaciones.liquidada` | alertas + salida de notif. |
| `topic.prosumidor.estado` | `topic.prosumidor.estado`, `topic.prosumidor.instalacion` | ECST del medidor (transferencia de estado) |
| `topic.proveedores.energia` | (no hay topic; `integracion-ami` consume del head-end AMI) | el proveedor entra por el ACL, no por topic Kafka |
| (dominio seguros) | `topic.siniestros.aprobado` | flujo EDA siniestros→reaseguro |
| (facturación) | `topic.facturacion.liquidacion`, `topic.facturacion.emitida` | liquidación→facturación→notificaciones |

> Los nombres del código y del broker quedan alineados literalmente al diagrama.
> La declaración central de los 13 topics vive en `k8s/voltera-topics.yaml`.

## Elementos del diagrama vs. código

| Elemento del diagrama | Estado | Dónde |
|---|---|---|
| Prosumidores / Medidor IoT → Ingreso | Equivalente | medidor → `integracion-ami` (ACL) |
| IoT Gateway (en K8s) | Equivalente | rol cubierto por `integracion-ami-service` |
| Serv. de Ingesta Telemetría | ✔ Cumple | `ingesta-service` |
| Kafka Pub/Sub (EDA) | ✔ Cumple | Redpanda/Kafka + OUTBOX |
| Serv. Adaptador Proveedores | ✔ Cumple (ACL) | `integracion-ami-service` |
| Adaptador → Message Queue (Amazon MQ / AWS SQS) | ⏳ Pendiente de alcance | ver sección AWS |
| Serv. de Telemetría · CQRS (Command/Query + BD esc/lec) | ✔ Cumple | `telemetria-core` (TSDB escritura + Redis lectura) |
| Serv. de Tarifas · CQRS doble-BD | ✔ Activado en k8s | perfil `cqrs-jdbc` (Postgres esc/lec) |
| Serv. de Trans. de Estado · CQRS doble-BD | ✔ Activado en k8s | `habilitacion-service` perfil `cqrs-jdbc` |
| Serv. de Notificaciones · CQRS doble-BD | ✔ Activado en k8s | `notificaciones-service` perfil `cqrs-jdbc` |
| Database per Service (PostgreSQL Escritura + Lectura) | ✔ Implementado ahora | datasources `*-write` / `*-read` por servicio |
| JDBC/SSL | ✔ Config | `sslmode` configurable en la URL JDBC |
| Serv. Transversal · Facturación | ✔ Cumple | `facturacion-service` |
| Serv. Transversal · Consultas | ⏳ Pendiente de alcance | no existe como microservicio |
| Serv. Transversal · Usuarios | ⏳ Pendiente de alcance | no existe como microservicio |
| Kubernetes (Control Plane, Nodes, Pods) | ✔ Cumple | manifiestos en `k8s/` |

## CQRS · Database per Service (implementado por perfil `cqrs-jdbc`)

Cada servicio de negocio que el diagrama muestra con CQRS tiene DOS datasources
PostgreSQL independientes (Database per Service), **activados en el despliegue**
con el perfil `cqrs-jdbc` (ya cableado en los manifiestos k8s junto a `inbox-jdbc`):

- **BD Escritura** (lado Command): `*_write` — el servicio escribe el estado.
- **BD Lectura** (lado Query): `*_read` — se sirven las consultas; el command proyecta aquí.

Sin el perfil, los servicios siguen con adaptadores in-memory (no requieren BD).

| Servicio | Perfil | BD escritura | BD lectura |
|---|---|---|---|
| tarifas | `cqrs-jdbc` | `voltera_tarifas_write` | `voltera_tarifas_read` |
| notificaciones | `cqrs-jdbc` | `voltera_notif_write` | `voltera_notif_read` |
| habilitacion (Trans. de Estado) | `cqrs-jdbc` | `voltera_habilitacion_write` | `voltera_habilitacion_read` |
| telemetria-core | `tsdb` + `redis` | TimescaleDB (serie) | Redis (vistas) |

Activación (ejemplo tarifas):
```
SPRING_PROFILES_ACTIVE=cqrs-jdbc
CQRS_WRITE_URL=jdbc:postgresql://pg:5432/voltera_tarifas_write?sslmode=require
CQRS_READ_URL=jdbc:postgresql://pg:5432/voltera_tarifas_read?sslmode=require
CQRS_USER=voltera  CQRS_PASSWORD=***
```

## Pendiente de alcance (requiere infra externa / decisión)

### Adaptador → Message Queue (Amazon MQ / AWS SQS)
El diagrama envía la salida del Adaptador de Proveedores a una cola gestionada AWS.
No se implementa como código local porque requiere cuenta/credenciales AWS. Para
materializarlo: añadir un adaptador de salida `SqsPublisher` (SDK `software.amazon.awssdk:sqs`)
activado por perfil `aws-sqs`, con `AWS_REGION` y `SQS_QUEUE_URL`. Queda documentado
como extensión; el ACL hoy publica a Kafka interno.

### Servicios transversales Consultas y Usuarios
No existen como microservicios. Si entran en alcance, se crean con el mismo patrón
hexagonal (dominio + puertos + REST) — `Usuarios` para identidad/preferencias y
`Consultas` como BFF de lectura agregada. Hoy esas funciones viven parcialmente en
otros servicios (p.ej. preferencias de notificación en `notificaciones`).

## Decisión de arquitectura · alineación con el diagrama (nombres de servicio)

El diagrama 04 es una **vista lógica**: nombra los componentes por su ROL
("Servicio de Transferencia de Estado", "Adaptador de Proveedores"), no por su
nombre de implementación. Decisión tomada:

- **NO se renombran las carpetas/artefactos de servicio.** Los nombres actuales
  son más precisos en términos de DDD: `habilitacion-service` es el *bounded
  context* de Habilitación (usa el patrón ECST / transferencia de estado), e
  `integracion-ami-service` es un *Anti-Corruption Layer* del head-end AMI.
  Degradar un nombre de dominio a un nombre de patrón, y romper imágenes Docker,
  Eureka, rutas de gateway y Postman, sería un retroceso sin ganancia funcional.

- **Trazabilidad explícita por anotaciones k8s.** Cada deployment lleva
  `voltera.io/diagrama-04` (nombre del componente en el diagrama) y
  `voltera.io/rol` (responsabilidad real). Consultable con:
  ```
  kubectl get deploy -n voltera-app \
    -o custom-columns='SERVICIO:.metadata.name,DIAGRAMA-04:.metadata.annotations.voltera\.io/diagrama-04'
  ```

| Componente del diagrama 04 | Servicio real | Anotación |
|---|---|---|
| Servicio de Telemetría | `telemetria-core-service` | ✔ |
| Servicio de Tarifas | `tarifas-service` | ✔ |
| Servicio de Transferencia de Estado | `habilitacion-service` | ✔ |
| Servicio de Notificaciones | `notificaciones-service` | ✔ |
| Servicio de Ingesta Telemetría | `ingesta-service` | ✔ |
| Adaptador de Proveedores | `integracion-ami-service` | ✔ |

- **Topics ya alineados literalmente** al estilo del diagrama (`topic.<dominio>.<evento>`),
  con declaración central en `k8s/voltera-topics.yaml`.
- **CQRS Database-per-Service activado** en los manifiestos (perfil `cqrs-jdbc`):
  `tarifas`, `habilitacion` y `notificaciones` con BD escritura/lectura PostgreSQL
  separadas, tal como muestra el diagrama.

## Validación contra el caso de negocio (láminas 01–04 · Módulo 3)

Contraste del backend contra el enunciado del caso "Habilitación de un medidor
inteligente y activación del flujo de telemetría AMI".

### Lámina 01 · Flujo de eventos — ✔ COMPLETO
- Dispara `OrdenInstalacionCerrada` (externo) → `habilitacion-service`; y cada
  `LecturaCrudaRecibida` del head-end vía `integracion-ami-service` (ACL).
- MS Habilitación emite `MedidorHabilitado` con **estado completo** (ECST).
- Adaptador emite `LecturaCrudaRecibida`; Ingesta emite `LecturaValidada`.
- Reaccionan: Ingesta (abre canal → `CanalIngestaCreado`), Telemetría Core (crea
  serie), Tarifas (`TarifaAsignada`), Notificaciones.
- Si falla: evento retenido en Kafka + offset; alta "pendiente" hasta
  `CanalIngestaCreado` + `TarifaAsignada`; a los 15 min → `HabilitacionFallida`
  (`Medidor.TIMEOUT_ALTA = 15 min`, `VigilanteTimeout` cada 5 s).

### Lámina 02 · Microservicios y DDD — ✔ COMPLETO (incluye BC seguros)
Los 6 BC del caso Telemetría existen con sus agregados y eventos exactos
(Habilitación, Integración AMI, Telemetría·Ingesta, Telemetría·Core, Tarifas,
Notificaciones). Para el **dominio seguros** del enunciado:
- Contexto **Siniestros** → `siniestros-service` (agregado Siniestro, emite
  `SiniestroAprobado`); `reaseguro-service` consume.
- Contexto **Cobros** → `cobros-service` (NUEVO): agregado **Movimiento** (raíz)
  con **Prima** (VO) y **Recibo** (entidad). **Consume `PagoConfirmado`** desde
  `topic.cobros.pago-confirmado` y concilia emitiendo el recibo, de forma
  idempotente (tabla inbox por `eventId`). Puerto 8100. Manifiesto
  `k8s/voltera-cobros.yaml`.

### Lámina 03 · Patrones y CQRS — ✔ COMPLETO
- Event notification, ECST, CQRS, Idempotencia (inbox), Orquestación (mediador
  con outbox + timeout 15 min), Coreografía, Zero Trust (mTLS/ACL a nivel mesh).
- **ECST con tópico compactado**: `topic.prosumidor.estado` se crea con
  `cleanup.policy=compact` (clave = medidorId) en `k8s/voltera-topics.yaml`, de
  modo que un consumidor nuevo reconstruye su réplica leyendo el tópico.

### Lámina 04 · KEDA y event mesh — ✔ COMPLETO
- ScaledObjects por lag del consumer group para ingesta, telemetria-core,
  proyector, notificaciones, habilitación, tarifa-eventos, reaseguro y **cobros**.
- Nombres de topic alineados al estilo del diagrama (`topic.<dominio>.<evento>`).
