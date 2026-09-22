#!/usr/bin/env bash
# Genera los manifiestos K8s de los 8 servicios de negocio de Voltera.
# Deployment + Service + HTTPRoute (Gateway API) por cada uno.
# Ruta expuesta: http://192.168.3.220/<ruta>/...
set -euo pipefail
OUT="voltera-services.yaml"
GW_NS="istio-ingress"
GW_NAME="istio-ingress"
REGISTRY="192.168.3.13/app-voltaire"

# nombre:puerto:ruta
SERVICES=(
  "facturacion-service:8088:facturacion"
  "liquidacion-mensual-service:8082:liquidacion-mensual"
  "liquidacion-p2p-service:8081:liquidacion-p2p"
  "volumetria-service:8083:volumetria"
  "tarifas-service:8084:tarifas"
  "pagos-service:8085:pagos"
)

: > "$OUT"
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"; rest="${entry#*:}"; port="${rest%%:*}"; route="${rest##*:}"
  cat >> "$OUT" <<YAML
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${name}
  namespace: voltera-app
  labels: { app: ${name} }
spec:
  replicas: 2                                   # HA: 2 replicas base (el HPA ajusta entre min/max)
  selector: { matchLabels: { app: ${name} } }
  template:
    metadata: { labels: { app: ${name}, version: v1 } }   # version -> Kiali/telemetria
    spec:
      imagePullSecrets: [{ name: harbor-cred }]
      containers:
        - name: ${name}
          image: ${REGISTRY}/${name}:1.0.0
          ports: [{ containerPort: ${port} }]
          env:
            - { name: EUREKA_ENABLED, value: "true" }
            - { name: EUREKA_URL, value: "http://service-registry:8761/eureka/" }
          resources:
            limits: { cpu: "500m", memory: "512Mi" }        # cpu limit para acotar el pod
            requests: { cpu: "50m", memory: "160Mi" }       # cpu request -> BASE del calculo del HPA
          readinessProbe:
            httpGet: { path: /actuator/health, port: ${port} }
            initialDelaySeconds: 25
            periodSeconds: 10
            failureThreshold: 6
---
apiVersion: v1
kind: Service
metadata:
  name: ${name}
  namespace: voltera-app
spec:
  selector: { app: ${name} }
  ports: [{ port: ${port}, targetPort: ${port} }]
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: ${name}
  namespace: voltera-app
spec:
  parentRefs:
    - name: ${GW_NAME}
      namespace: ${GW_NS}
  rules:
    - matches:
        - path: { type: PathPrefix, value: /${route} }
      filters:
        - type: URLRewrite
          urlRewrite:
            path: { type: ReplacePrefixMatch, replacePrefixMatch: "" }
      backendRefs:
        - name: ${name}
          port: ${port}
YAML
done
echo "Generado $OUT con ${#SERVICES[@]} servicios."
