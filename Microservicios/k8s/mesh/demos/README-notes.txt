============================================================================
 VOLTERA - CAPA DE SERVICE MESH (ISTIO)  ·  Runbook para la sustentacion
============================================================================

Que evalua el profesor  ->  Que lo cubre  ->  Como se evidencia
----------------------------------------------------------------------------

1) mTLS ENTRE MICROSERVICIOS
   Artefacto : 00-namespace.yaml (inyeccion sidecar) + 01-mtls-peerauth.yaml (STRICT)
   Evidencia : demos/verify-mtls.sh
               - pods 2/2 (app + istio-proxy)
               - PeerAuthentication mode: STRICT
               - candado mTLS en el grafo de Kiali
               - conexion en texto plano RECHAZADA

2) LATENCIA POR SALTO (observabilidad / trazabilidad)
   Artefacto : 03-telemetry-tracing.yaml + addons (install-observability.sh)
   Evidencia : demos/query-latency-per-hop.sh  -> p50/p90/p99 por arista
               Kiali (Graph -> Response Time)  -> ms en cada salto
               Jaeger                           -> traza de la cadena completa
   Metrica   : istio_request_duration_milliseconds (source -> destination)

3) CIRCUIT BREAKER (aislamiento de fallos)
   Artefacto : 02-destinationrules-circuitbreaker.yaml
               (connectionPool + outlierDetection; facturacion = breaker agresivo)
   Evidencia : demos/demo-circuit-breaker.sh
               - al saturar facturacion aparecen 503 (flag UO = Upstream Overflow)
               - contador Envoy upstream_cx_overflow
               - volumetria (registro de casas) SIGUE respondiendo 200 => aislamiento

4) ESCALABILIDAD (HPA)
   Artefacto : gen-services.sh (CPU requests) + 04-hpa.yaml
   Evidencia : demos/demo-scalability.sh
               - "de N a M pods en X s" (eventos del HPA con timestamp)
               - kubectl get hpa -w / kubectl top pods
   Requisito : metrics-server instalado (kubectl top pods debe dar datos)

5) DISPONIBILIDAD / FAILOVER (activo-activo)
   Artefacto : replicas>=2 (gen-services.sh) + 05-availability-pdb.yaml
               + outlierDetection/retries del paso 2
   Evidencia : demos/demo-availability.sh
               - se mata 1 pod bajo carga; fortio reporta ~100% Code 200
               - el Deployment recrea la replica (tiempo de recuperacion)

----------------------------------------------------------------------------
ORDEN DE EJECUCION (demo en vivo)
----------------------------------------------------------------------------
  cd Microservicios/k8s/mesh
  ./apply-all.sh --dry-run        # valida contra el API server
  ./apply-all.sh                  # aplica y reinicia deployments (inyecta sidecar)
  ./demos/install-observability.sh

  ./demos/verify-mtls.sh
  ./demos/demo-circuit-breaker.sh
  ./demos/demo-scalability.sh
  ./demos/query-latency-per-hop.sh
  ./demos/demo-availability.sh

----------------------------------------------------------------------------
COMPLETADO (antes eran limitaciones, ya resueltas)
----------------------------------------------------------------------------
A) TRAZA DISTRIBUIDA CONTINUA:  RESUELTO.
   Los 6 servicios llevan micrometer-tracing-bridge-brave + zipkin-reporter-brave
   y config management.tracing.sampling.probability=1.0 +
   management.zipkin.tracing.endpoint (env ZIPKIN_ENDPOINT, default
   http://zipkin.istio-system:9411/api/v2/spans). Propagan b3/traceparent, así
   que la traza NO se corta al pasar por la app y se ve completa en Jaeger.

B) SALTO REAL ENTRE MICROS:  RESUELTO.
   facturacion-service -> tarifas-service (arquitectura hexagonal):
   - puerto out  : domain/port/out/TarifaConsultaPort
   - adaptador   : infrastructure/client/TarifaRestClient (RestClient instrumentado)
   - uso         : al emitir factura con tarifaId, consulta el precio vigente a
                   tarifas (GET /api/v1/tarifas/{id}/precio?hora=H).
   El salto viaja con mTLS por los sidecars y su latencia se mide en Prometheus.
   El circuit breaker de tarifas ahora aplica "entre micros".

----------------------------------------------------------------------------
DEMO END-TO-END DESDE EL FRONT (dispara el salto real)
----------------------------------------------------------------------------
1) En la UI (pestaña Tarifas) crear una tarifa y copiar su ID (TAR-xxx).
2) En la UI (pestaña Facturación) emitir una factura poniendo ese Tarifa ID
   y una Hora (0-23). Eso hace: front -> WSO2 -> ingress -> facturacion
   -> (mTLS) -> tarifas.
3) En Jaeger: buscar servicio "facturacion-service" -> se ve la traza con el
   span hijo hacia "tarifas-service".
4) En Kiali (Graph): aparece la arista facturacion -> tarifas con su latencia.

DESPLIEGUE (requiere red al cluster/Harbor; ejecutar en la máquina con acceso):
   cd Microservicios
   ./k8s/rebuild-amd64.sh            # build amd64 + push a Harbor (imágenes nuevas)
   kubectl apply -f k8s/voltera-registry.yaml
   kubectl apply -f k8s/voltera-services.yaml
   ./k8s/mesh/apply-all.sh           # malla + sidecars + mTLS + CB + HPA + PDB
   ./k8s/mesh/demos/install-observability.sh
   # Frontend (Vercel):  cd voltera-frontend && npm run build && vercel deploy --prod
============================================================================

----------------------------------------------------------------------------
CONSULTAS PromQL (ejecutar en Prometheus/Grafana — no se muestran en el front)
----------------------------------------------------------------------------
# Latencia p50 por salto (ms)
histogram_quantile(0.5, sum(rate(istio_request_duration_milliseconds_bucket{reporter="destination",destination_service_namespace="voltera-app"}[5m])) by (le, source_workload, destination_workload))

# Peticiones por segundo (por servicio)
sum(rate(istio_requests_total{destination_service_namespace="voltera-app"}[1m])) by (destination_workload)

# % de trafico con mTLS
sum(rate(istio_requests_total{connection_security_policy="mutual_tls"}[5m])) / sum(rate(istio_requests_total[5m]))

# Cortes del circuit breaker (503) en facturacion
sum(rate(istio_requests_total{destination_workload="facturacion-service",response_code="503"}[1m]))

# Replicas actuales por HPA (escalabilidad)
kube_horizontalpodautoscaler_status_current_replicas{namespace="voltera-app"}

Prometheus (LAN): http://192.168.3.11:31659/graph   ·   Grafana: http://192.168.3.11:32421
============================================================================
