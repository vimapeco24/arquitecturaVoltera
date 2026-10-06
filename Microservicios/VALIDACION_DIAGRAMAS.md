# Validación de los diagramas UML (04) y "Diagrama con Patrones"

Contraste de ambos diagramas de la lámina 04 contra el código real, con el mapeo de
nombres y el estado de cada elemento (cumple / equivalente / implementado ahora /
pendiente de alcance).

## Mapeo de topics (diagrama → broker real)

| Topic del diagrama | Topic(s) real(es) en el código | Nota |
|---|---|---|
| `topic.telemetria.medidores` | `ami-lecturas-crudas`, `telemetria-lecturas-validadas` | ingesta/telemetría |
| `topic.tarifas.calculo` | `telemetria-consumo-intervalos`, `tarifas-eventos` | tarifas consume consumo, emite tarifa |
| `topic.notificaciones` | `telemetria-alertas`, `cliente-notificado`, `facturas-emitidas` | alertas + salida de notif. |
| `topic.prosumidor.estado` | `medidores-habilitacion` | ECST del medidor (transferencia de estado) |
| `topic.proveedores.energia` | (no hay topic; `integracion-ami` consume del head-end AMI) | el proveedor entra por el ACL, no por topic Kafka |

> No se renombran los topics reales (romperían el EDA ya funcionando). El diagrama
> usa nombres lógicos; esta tabla es el contrato de equivalencia.

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
| Serv. de Tarifas · CQRS doble-BD | ✔ Implementado ahora | perfil `cqrs-jdbc` (Postgres esc/lec) |
| Serv. de Trans. de Estado · CQRS doble-BD | ✔ Implementado ahora | `habilitacion-service` perfil `cqrs-jdbc` |
| Serv. de Notificaciones · CQRS doble-BD | ✔ Implementado ahora | `notificaciones-service` perfil `cqrs-jdbc` |
| Database per Service (PostgreSQL Escritura + Lectura) | ✔ Implementado ahora | datasources `*-write` / `*-read` por servicio |
| JDBC/SSL | ✔ Config | `sslmode` configurable en la URL JDBC |
| Serv. Transversal · Facturación | ✔ Cumple | `facturacion-service` |
| Serv. Transversal · Consultas | ⏳ Pendiente de alcance | no existe como microservicio |
| Serv. Transversal · Usuarios | ⏳ Pendiente de alcance | no existe como microservicio |
| Kubernetes (Control Plane, Nodes, Pods) | ✔ Cumple | manifiestos en `k8s/` |

## CQRS · Database per Service (implementado por perfil `cqrs-jdbc`)

Cada servicio de negocio que el diagrama muestra con CQRS tiene DOS datasources
PostgreSQL independientes (Database per Service), activados con el perfil `cqrs-jdbc`:

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
