# Microservicios de Negocio — Voltera

Los 8 microservicios de la capa de **Servicios de Negocio** del diagrama UML de Voltera,
implementados en **Java 21 + Spring Boot 3.3** con **arquitectura hexagonal**, pruebas
unitarias y un **Service Registry** (Eureka) para el descubrimiento.

Diseñados para **bajo consumo de RAM**: cada pod comparte ~5 GB con la pila de
observabilidad y el sidecar del service mesh, así que los servicios dejan la mayor
parte de la memoria libre (consumo medido en reposo: **~30 MB RSS** por servicio).

## Estructura

```
Microservicios/
├── ServiciosDeNegocio/
│   ├── facturacion-service/     (8081) Facturación y liquidación de energía
│   ├── liquidacion-service/     (8082) Liquidación P2P / Mercado
│   ├── volumetria-service/      (8083) Ingesta y validación de telemetría
│   ├── tarifas-service/         (8084) Esquemas de precio dinámico
│   ├── pagos-service/           (8085) Cobro de facturas y pago de excedentes
│   ├── notificaciones-service/  (8086) Push / email / SMS
│   ├── reportes-service/        (8087) Reportes regulatorios y de mercado
│   └── comunidades-service/     (8088) Comunidades energéticas
├── service-registry/            (8761) Eureka Server
├── docker-compose.yml           Orquestación de los 9 contenedores
├── Voltera_Postman_Collection.json   Colección con los 46 requests
└── README.md
```

Cada servicio es un **proyecto Maven independiente** (su propio `pom.xml`, código, tests
y Dockerfile), lo que permite compilarlo, probarlo, desplegarlo y escalarlo por separado.

## Arquitectura Hexagonal (Ports & Adapters)

```
domain/           Núcleo. Agregados, entidades, value objects, reglas de negocio. Sin frameworks.
  model/          Agregado raíz, entidades y value objects.
  port/in/        Puertos de ENTRADA (casos de uso).
  port/out/       Puertos de SALIDA (persistencia).
  exception/
application/      Servicios de aplicación. Orquestan el dominio. Agnósticos de framework.
infrastructure/   Adaptadores. Aquí vive Spring.
  rest/           Adaptador de entrada (controladores REST + DTOs).
  persistence/    Adaptador de salida (repositorio en memoria).
  config/         Wiring de beans.
```

Regla de dependencia: **la infraestructura depende del dominio, nunca al revés.**

## Servicios y agregados (DDD táctico)

| Servicio | Puerto | Agregado raíz | Reglas de negocio destacadas |
|---|---|---|---|
| Facturación | 8081 | `Factura` | consumo neto, tarifa, excedentes; estados EMITIDA/PAGADA/ANULADA |
| Liquidación P2P | 8082 | `TransaccionP2P` | matching, precio de casación = promedio, energía = mínimo |
| Volumetría | 8083 | `LecturaTelemetria` | validación VALIDA/SOSPECHOSA(>500 kWh)/RECHAZADA(futuro) |
| Tarifas | 8084 | `Tarifa` | franjas horarias sin solape; precio por hora |
| Pagos | 8085 | `OrdenPago` | idempotencia por referencia; estados PENDIENTE/PROCESADO/FALLIDO |
| Notificaciones | 8086 | `Notificacion` | destinatario validado por canal (email/tel/token) |
| Reportes | 8087 | `Reporte` | tipos REGULATORIO_CREG/MERCADO/CONSUMO; GENERADO→ENVIADO |
| Comunidades | 8088 | `Comunidad` | miembros sin duplicar; reparto proporcional por participación |

## Endpoints principales

Ver la colección `Voltera_Postman_Collection.json` (46 requests). Resumen:

- **Facturación:** `POST /api/v1/facturas`, `GET /{id}`, `GET ?prosumidorId`
- **Liquidación:** `POST /api/v1/transacciones/emparejar`, `GET /{id}`, `GET ?prosumidorId`
- **Volumetría:** `POST /api/v1/lecturas`, `GET /{id}`, `GET ?medidorId`
- **Tarifas:** `POST /api/v1/tarifas`, `GET /{id}`, `GET /{id}/precio?hora=X`, `GET` (listar)
- **Pagos:** `POST /api/v1/pagos`, `GET /{id}`, `GET ?prosumidorId`
- **Notificaciones:** `POST /api/v1/notificaciones`, `GET /{id}`, `GET ?prosumidorId`
- **Reportes:** `POST /api/v1/reportes`, `POST /{id}/enviar`, `GET /{id}`, `GET` (listar)
- **Comunidades:** `POST /api/v1/comunidades`, `POST /{id}/miembros`, `POST /{id}/activar`, `POST /{id}/repartir`, `GET /{id}`, `GET` (listar)

## Optimización de RAM

| Técnica | Efecto |
|---|---|
| `spring.main.lazy-initialization=true` | Beans bajo demanda → menor heap al arranque |
| `spring.threads.virtual.enabled=true` | Hilos virtuales (Java 21) → menos memoria por request |
| Tomcat 25 hilos (vs 200 por defecto) | Menos buffers y stacks |
| `-XX:+UseSerialGC` | GC de baja huella para cargas pequeñas |
| `-XX:MaxRAMPercentage=50` | Heap acotado al 50% del límite del contenedor |
| Persistencia en memoria (sin driver de BD) | Sin pool de conexiones |

**Consumo medido:** ~30 MB RSS por servicio. En `docker-compose` cada servicio tiene
límite de 512 MB (holgado), dejando la mayor parte del pod para observabilidad y el
sidecar del service mesh.

## Cómo ejecutar

### Un servicio local (Maven wrapper)
```bash
cd ServiciosDeNegocio/facturacion-service
./mvnw spring-boot:run      # arranca
./mvnw test                 # pruebas
```

### Todos juntos con Docker Compose
```bash
cd Microservicios
docker compose up --build
```

## Pruebas — 68 en total

| Servicio | Pruebas |
|---|---|
| Facturación | 12 |
| Liquidación | 12 |
| Volumetría | 5 |
| Tarifas | 6 |
| Pagos | 8 |
| Notificaciones | 8 |
| Reportes | 7 |
| Comunidades | 10 |

Framework: JUnit 5 + Mockito. Todas pasan.
