# BITÁCORA — Entrega 3 (EDA) · Voltera · Backend + Frontend

> Registro de contexto persistente de las sesiones de trabajo sobre la arquitectura
> orientada a eventos (Módulo 3). Si se pierde la sesión, **empieza leyendo este archivo**
> para saber qué se hizo, qué quedó pendiente y dónde está cada cosa.

Repositorio remoto destino: https://github.com/vimapeco24/arquitecturaVoltera

---

## Estado global (al cierre de la última sesión)

Backend y frontend alineados con el caso de negocio (láminas 01–04 del Módulo 3) y con
los dos diagramas UML de la lámina 04. Los 4 aspectos del enunciado del experimento
(EDA, KEDA, CQRS, Idempotencia/transferencia de estado) están cubiertos con evidencia
ejecutable en ambos flujos EDA (Telemetría y Seguros).

### Resumen de lo realizado (en orden cronológico de la sesión)

1. **Validación del experimento (lámina 06/20)** — se añadió el experimento **KEDA**
   que faltaba en `experimentos-telemetria.component.ts` (frontend). El componente de
   siniestros (`experimentos.component.ts`) ya tenía los 4 aspectos.

2. **Idempotencia simétrica (backend)** — `reaseguro-service` tenía idempotencia débil
   (solo por repo de escritura). Se le añadió una **tabla INBOX dedicada** igual a la de
   `tarifa-eventos`: `MensajeInbox`, `InboxPort`, `InMemoryInbox`, endpoint
   `GET /api/v1/cesiones/inbox`. El frontend muestra el conteo de duplicados.

3. **Store-and-forward (backend)** — `siniestros-service` era best-effort (podía perder
   eventos si el broker caía). Se le añadió el mismo patrón de `telemetria-service`:
   `BufferOfflinePort`, `DrenarBufferUseCase`, `InMemoryBufferOffline`, publisher
   confirmado (ack con timeout → boolean), `@EnableScheduling` + `DrenadoProgramado`,
   endpoint `POST /api/v1/siniestros/buffer/drenar`. Frontend: `drenarBuffer()` + exp. 1.

4. **Alineación de topics al diagrama (Opción B)** — se renombraron los **13 topics**
   Kafka del estilo guion (`ami-lecturas-crudas`) al estilo del diagrama con punto
   (`topic.telemetria.medidores`), de forma atómica en: constantes `TOPIC`,
   `@KafkaListener`, claves de inbox (`CONSUMIDOR#topic...`), ScaledObjects de KEDA,
   Javadoc, HTML del frontend y docs. Se creó `k8s/voltera-topics.yaml` (declaración
   central con `rpk`).

   **Mapa de topics (guion viejo → nombre actual):**
   | Viejo | Actual |
   |---|---|
   | ami-lecturas-crudas | topic.telemetria.medidores |
   | telemetria-lecturas-validadas | topic.telemetria.validadas |
   | consumo-registrado | topic.telemetria.consumo |
   | medidores-habilitacion | topic.prosumidor.estado (COMPACTADO) |
   | ordenes-instalacion | topic.prosumidor.instalacion |
   | telemetria-consumo-intervalos | topic.tarifas.calculo |
   | tarifas-eventos | topic.tarifas.asignada |
   | telemetria-alertas | topic.notificaciones |
   | cliente-notificado | topic.notificaciones.cliente |
   | notificacion-liquidada | topic.notificaciones.liquidada |
   | liquidaciones-calculadas | topic.facturacion.liquidacion |
   | facturas-emitidas | topic.facturacion.emitida |
   | siniestro-aprobado | topic.siniestros.aprobado |
   | (nuevo) | topic.cobros.pago-confirmado |

5. **Alineación de servicios al diagrama (decisión de arquitectura)** — NO se renombraron
   las carpetas de servicio (habría roto imágenes Docker, Eureka, gateway, Postman, y los
   nombres actuales son mejores en DDD). En su lugar: **anotaciones k8s de trazabilidad**
   `voltera.io/diagrama-04` + `voltera.io/rol` en los deployments de negocio, y se
   **activó el CQRS doble-PostgreSQL** (perfil `cqrs-jdbc`) en los manifiestos de
   `tarifas`, `habilitacion` y `notificaciones` (BD write/read separadas; las 6 bases ya
   están en `db-init/create-inbox-db.sql`).

6. **BC Cobros (backend NUEVO)** — la lámina 02 del enunciado exige el "Contexto Cobros →
   agregado Movimiento con Prima y Recibo; consume PagoConfirmado". Estaba AUSENTE. Se
   creó `cobros-service` completo (puerto 8100, hexagonal): agregado `Movimiento` (raíz)
   con VO `Prima` y entidad `Recibo`, consume `PagoConfirmado` (idempotente por inbox),
   emite el recibo al conciliar. Endpoints `/api/v1/movimientos`, `/simular-pago`,
   `/inbox`. Manifiesto `k8s/voltera-cobros.yaml` (Deployment+Service+HTTPRoute+KEDA).
   También se compactó `topic.prosumidor.estado` (ECST, lámina 03).

7. **Frontend alineado con TODO el backend** — se añadió la página/servicio/modelo de
   **Cobros** (ruta `/cobros`, nav `💵 Cobros`, `SERVICIOS.cobros`), con registro de
   movimiento, simulación de `PagoConfirmado` e inbox. Se completó
   `health.service.ts` para listar los **17 microservicios** con sus puertos reales
   (antes solo 6). El dashboard los chequea dinámicamente.

---

## Inventario de servicios del backend (ServiciosDeNegocio/)

| Servicio | Puerto | Rol | Topic emite → consume |
|---|---|---|---|
| habilitacion-service | 8095 | Mediador del alta (BC Habilitación) | emite topic.prosumidor.estado; consume topic.prosumidor.instalacion, topic.telemetria.validadas, topic.tarifas.asignada |
| integracion-ami-service | 8096 | ACL head-end AMI | emite topic.telemetria.medidores; consume topic.prosumidor.estado |
| ingesta-service | 8097 | Valida/deduplica (BC Telemetría·Ingesta) | emite topic.telemetria.validadas + topic.notificaciones; consume topic.telemetria.medidores, topic.prosumidor.estado |
| telemetria-core-service | 8098 | CQRS core (TSDB+Redis) | emite topic.tarifas.calculo + topic.notificaciones; consume topic.telemetria.validadas, topic.prosumidor.estado |
| tarifas-service | 8084 | BC Tarifas (E2) | emite topic.tarifas.asignada; consume topic.tarifas.calculo, topic.prosumidor.estado |
| notificaciones-service | 8099 | BC Notificaciones (E2) | emite topic.notificaciones.cliente; consume topic.notificaciones, topic.prosumidor.estado, topic.facturacion.emitida |
| liquidacion-mensual-service | 8082 | Liquidación mensual | emite topic.facturacion.liquidacion; consume topic.tarifas.calculo |
| facturacion-service | 8088 | Facturación | emite topic.facturacion.emitida; consume topic.facturacion.liquidacion |
| telemetria-service | 8093 | Emisor IoT demo (store-and-forward) | emite topic.telemetria.consumo |
| tarifa-eventos-service | 8094 | Consumidor CQRS demo | consume topic.telemetria.consumo; emite topic.notificaciones.liquidada |
| siniestros-service | 8091 | BC Siniestros (seguros) | emite topic.siniestros.aprobado (store-and-forward) |
| reaseguro-service | 8092 | BC Reaseguro (seguros) | consume topic.siniestros.aprobado (INBOX) |
| **cobros-service** | **8100** | **BC Cobros (seguros, NUEVO)** | **consume topic.cobros.pago-confirmado (INBOX)** |
| liquidacion-p2p-service | 8081 | Liquidación P2P | solo REST |
| volumetria-service | 8083 | Volumetría | solo REST |
| pagos-service | 8085 | Pagos | solo REST |

## Frontend (voltera-frontend/)

- Páginas en `src/app/pages/` — una por servicio + experimentos + observabilidad.
- Servicios HTTP en `src/app/services/` (incluye `cobros.service.ts` nuevo).
- `core/gateway.ts` → `SERVICIOS` (incluye `cobros`). `core/models.ts` → tipos (incluye Movimiento/Prima/Recibo/PagoConfirmado).
- `app.routes.ts` + `app.component.ts` (nav) incluyen `/cobros`.
- `services/health.service.ts` lista los 17 servicios con puertos.

## Experimentos (los 4 aspectos del enunciado, con evidencia ejecutable)

- **Flujo Telemetría** → `pages/experimentos-telemetria/`: EDA end-to-end, KEDA
  (pico de ingestas), CQRS (retraso escritura→lectura), Idempotencia, Notificación.
- **Flujo Seguros** → `pages/experimentos/`: EDA (no se pierden eventos + drenaje buffer),
  KEDA (pico de siniestros), CQRS, Idempotencia (muestra conteo de la inbox).
- **Cobros** → `pages/cobros/`: su propia demo de idempotencia (simular PagoConfirmado + inbox).

---

## Cómo verificar (comandos)

```bash
# Backend — un servicio (desde su carpeta):
cd Microservicios/ServiciosDeNegocio/<servicio> && ./mvnw -o test

# Frontend (desde Microservicios/voltera-frontend):
node_modules/.bin/tsc --noEmit -p tsconfig.app.json   # chequeo de tipos
node_modules/.bin/ng build --configuration development # build completo
```

Estado de verificación al cierre: cobros 4/4 tests, siniestros 6/6, reaseguro 4/4,
tarifa-eventos 11/11, tarifas 6/6, habilitacion 4/4, notificaciones 7/7. Frontend build OK.

---

## Pendientes / gaps conocidos (NO bloqueantes, documentados)

1. **Amazon MQ / AWS SQS** (lámina 04 diagrama): salida del Adaptador a cola gestionada
   AWS — no implementada (requiere cuenta/credenciales AWS). Extensión documentada:
   adaptador `SqsPublisher` bajo perfil `aws-sqs`.
2. **VO como clases dedicadas**: `CredencialDispositivo`, `VentanaDeDuplicados`,
   `LoteDeLecturas`, `ProveedorAMI` existen como funcionalidad (Set/List/String) pero no
   como clase con nombre. Refinamiento de modelado, no cambia comportamiento.
3. **Peritaje/Liquidación del siniestro** como entidades (hoy Peritaje es un estado).
4. **Process manager del alta duplicado** en `habilitacion-service` y
   `tarifa-eventos-service` (ambos emiten MedidorHabilitado). Consolidar es refactor mayor.
5. **Persistencia**: por defecto in-memory; JDBC/TSDB/Redis solo bajo perfiles. El CQRS
   doble-Postgres ya está activado en los manifiestos (perfil cqrs-jdbc).

## Documentos de referencia (fuente de verdad)

- `Microservicios/VALIDACION_DIAGRAMAS.md` — mapeo diagrama↔código, decisiones, validación láminas 01-04.
- `Microservicios/ARQUITECTURA_ACTUAL.md` — grafo de eventos EDA + C4.
- `Microservicios/EVENT_MESH_KEDA.md` — tabla KEDA (topics/umbral/min/max).
- `Microservicios/k8s/` — manifiestos (voltera-eda, voltera-negocio-eda, voltera-topics,
  voltera-cobros, voltera-keda-lamina04, voltera-services).
