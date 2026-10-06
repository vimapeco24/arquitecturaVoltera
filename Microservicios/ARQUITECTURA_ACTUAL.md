# Voltera — Arquitectura Actual (Front + Backend)

Diagrama de la arquitectura de extremo a extremo tal como está implementada hoy:
Angular SPA → BFF serverless (Vercel) → WSO2 API Gateway → Istio Ingress →
microservicios Spring Boot (arquitectura hexagonal) sobre Kubernetes, con capa
EDA (Redpanda/Kafka + KEDA), service mesh (Istio + mTLS) y observabilidad
(Prometheus, Grafana, Kiali, Jaeger).

## Vista general (C4 - Contenedores)

```mermaid
flowchart TB
    user([Usuario / Navegador])

    %% ---------------- FRONTEND ----------------
    subgraph FE["Frontend (Vercel)"]
        direction TB
        spa["Angular SPA<br/>voltera-frontend<br/>(standalone, lazy routes)"]
        subgraph BFF["BFF Serverless (Vercel Functions)"]
            gwfn["/api/gw/[...path]<br/>proxy → WSO2"]
            tokenfn["/api/token<br/>OAuth2 client-credentials"]
        end
    end

    %% ---------------- API GATEWAY ----------------
    subgraph GW["API Gateway"]
        wso2["WSO2 API Manager 4.7.0<br/>Gateway :8243 (https)<br/>Publisher/DevPortal :9443<br/>rutas /{servicio}/v1/..."]
    end

    %% ---------------- CLUSTER K8S ----------------
    subgraph K8S["Kubernetes — namespace voltera-app"]
        direction TB
        ingress["Istio Ingress Gateway<br/>(Gateway API / HTTPRoute)<br/>192.168.3.220"]

        subgraph MESH["Service Mesh (Istio + mTLS + sidecars Envoy)"]
            direction TB
            registry["Service Registry<br/>Eureka :8761"]

            subgraph NEG["Servicios de Negocio (Spring Boot 3.3 / Java 21, hexagonal)"]
                direction LR
                tarifas["tarifas :8084"]
                volumetria["volumetria :8083"]
                liqp2p["liquidacion-p2p :8081"]
                liqmen["liquidacion-mensual :8082"]
                factura["facturacion :8088"]
                pagos["pagos :8085"]
            end

            subgraph EDA["Servicios EDA (Event-Driven)"]
                direction LR
                siniestros["siniestros :8091<br/>(productor)"]
                reaseguro["reaseguro :8092<br/>(consumidor)"]
                telemetria["telemetria :8090"]
                tarifaev["tarifa-eventos"]
            end

            broker[("Redpanda<br/>broker Kafka :9092<br/>topic: siniestro-aprobado")]
            keda["KEDA ScaledObject<br/>autoescala por lag"]
        end

        subgraph OBS["Observabilidad (istio-system)"]
            direction LR
            prom["Prometheus"]
            graf["Grafana"]
            kiali["Kiali"]
            jaeger["Jaeger"]
        end
    end

    %% ---------------- FLUJOS ----------------
    user --> spa
    spa --> gwfn
    spa --> tokenfn
    tokenfn -->|token OAuth2| wso2
    gwfn -->|server-to-server| wso2
    wso2 --> ingress
    ingress --> NEG
    ingress --> EDA

    NEG -. registro/descubrimiento .-> registry
    EDA -. registro/descubrimiento .-> registry

    siniestros -->|publica evento| broker
    broker -->|consume| reaseguro
    broker -.lag.-> keda
    keda -.escala.-> reaseguro

    MESH -.métricas/trazas.-> prom
    prom --> graf
    MESH -.grafo mTLS.-> kiali
    MESH -.trazas.-> jaeger
```

## Detalle del backend hexagonal (por microservicio)

```mermaid
flowchart LR
    subgraph svc["Microservicio (Ports & Adapters)"]
        direction TB
        rest["infrastructure/rest<br/>Controladores REST + DTOs"]
        app["application<br/>Servicios de aplicación"]
        dom["domain<br/>Agregados, VOs, reglas<br/>(sin frameworks)"]
        persist["infrastructure/persistence<br/>Repositorio en memoria"]
        cfg["infrastructure/config<br/>Wiring de beans"]
    end
    rest --> app --> dom
    app --> persist
    persist -.implementa port/out.-> dom
    rest -.implementa port/in.-> dom

    note["Regla de dependencia:<br/>infraestructura → dominio (nunca al revés)"]
```

## Componentes y puertos

### Frontend
| Componente | Tecnología | Detalle |
|---|---|---|
| SPA | Angular (standalone, rutas lazy) | Páginas: dashboard, tarifas, volumetría, liquidación P2P/mensual, facturación, pagos, siniestros, reaseguro, telemetría, tarifa-eventos, observabilidad, patrones, experimentos |
| BFF | Vercel Serverless Functions | `/api/gw/[...path]` (proxy a WSO2), `/api/token` (OAuth2). Oculta el `consumerSecret`, evita CORS y el cert autofirmado del gateway |
| Hosting | Vercel | `voltera-frontend.vercel.app` |

### API Gateway
| Componente | Puerto | Detalle |
|---|---|---|
| WSO2 API Manager 4.7.0 | 8243 (https), 8280 (http), 9443 (Publisher/DevPortal) | Publicación, ciclo de vida, throttling. Rutas `/{servicio}/v1/api/v1/...` |

### Servicios de negocio (Spring Boot 3.3 / Java 21, hexagonal)
| Servicio | Puerto | Agregado raíz |
|---|---|---|
| tarifas | 8084 | `Tarifa` |
| volumetria | 8083 | `LecturaTelemetria` |
| liquidacion-p2p | 8081 | `TransaccionP2P` |
| liquidacion-mensual | 8082 | liquidación mensual |
| facturacion | 8088 | `Factura` |
| pagos | 8085 | `OrdenPago` |

### Servicios EDA
| Servicio | Puerto | Rol |
|---|---|---|
| siniestros | 8091 | Productor del evento `SiniestroAprobado` |
| reaseguro | 8092 | Consumidor de `siniestro-aprobado` (autoescala KEDA por lag) |
| telemetria | 8090 | Telemetría / métricas |
| tarifa-eventos | — | Tarifa basada en eventos |

### Infraestructura
| Componente | Detalle |
|---|---|
| Service Registry | Eureka :8761 (descubrimiento) |
| Broker | Redpanda (compatible Kafka) :9092, single-node |
| Orquestación | Kubernetes (namespace `voltera-app`) + Docker Compose (local) |
| Service Mesh | Istio + Envoy sidecars, mTLS (PeerAuthentication), DestinationRules (circuit breaker), HPA, PodDisruptionBudget |
| Autoescalado eventos | KEDA ScaledObject |
| Ingress | Istio Ingress Gateway (Gateway API / HTTPRoute) 192.168.3.220 |
| Registry de imágenes | Harbor `192.168.3.13/app-voltaire` |
| Observabilidad | Prometheus, Grafana, Kiali, Jaeger |

## Flujo de una petición (ejemplo)

```mermaid
sequenceDiagram
    participant U as Navegador
    participant SPA as Angular SPA
    participant TK as /api/token (Vercel)
    participant GW as /api/gw (Vercel)
    participant W as WSO2 Gateway
    participant I as Istio Ingress
    participant S as tarifas-service

    U->>SPA: abre página Tarifas
    SPA->>TK: solicita token OAuth2
    TK->>W: client-credentials
    W-->>TK: access_token
    SPA->>GW: GET /api/gw/tarifas/v1/api/v1/tarifas (Bearer)
    GW->>W: proxy server-to-server
    W->>I: /tarifas/v1/...
    I->>S: HTTPRoute → pod (sidecar Envoy, mTLS)
    S-->>U: 200 JSON (vía cadena inversa)
```

## Flujo de un evento (EDA)

```mermaid
sequenceDiagram
    participant SI as siniestros-service
    participant R as Redpanda (Kafka)
    participant RE as reaseguro-service
    participant K as KEDA

    SI->>R: publica SiniestroAprobado (topic siniestro-aprobado)
    R-->>RE: entrega evento (consumer group)
    R-->>K: expone lag del consumer group
    K-->>RE: escala réplicas según lag
    RE->>RE: procesa cesión de reaseguro
```
