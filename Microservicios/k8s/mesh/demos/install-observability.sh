#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | Instala la pila de observabilidad de Istio
#   Prometheus (metricas) + Grafana (dashboards) + Kiali (grafo/latencia por
#   salto) + Jaeger (trazas distribuidas).
# ============================================================================
set -euo pipefail

# Version de Istio instalada (ajustar si difiere).
ISTIO_VER="${ISTIO_VER:-1.23}"
BASE="https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VER}/samples/addons"

echo ">> Instalando addons de observabilidad (release-${ISTIO_VER})..."
kubectl apply -f "${BASE}/prometheus.yaml"
kubectl apply -f "${BASE}/grafana.yaml"
kubectl apply -f "${BASE}/jaeger.yaml"
kubectl apply -f "${BASE}/kiali.yaml"

echo ">> Esperando a que los addons esten listos..."
kubectl -n istio-system rollout status deploy/prometheus --timeout=180s || true
kubectl -n istio-system rollout status deploy/grafana    --timeout=180s || true
kubectl -n istio-system rollout status deploy/jaeger      --timeout=180s || true
kubectl -n istio-system rollout status deploy/kiali       --timeout=180s || true

cat <<'EOF'

>> Listo. Abrir los tableros (cada uno en una terminal):

  # Kiali  -> grafo de servicios con LATENCIA POR SALTO y estado de mTLS (candado)
  istioctl dashboard kiali

  # Jaeger -> trazas distribuidas (cadena de saltos por request)
  istioctl dashboard jaeger

  # Grafana -> dashboards "Istio Service/Workload" con percentiles p50/p90/p99
  istioctl dashboard grafana

  # Prometheus -> consultas PromQL crudas
  istioctl dashboard prometheus

En Kiali: Graph -> namespace voltera-app -> Display -> "Traffic Animation" y
"Response Time" para ver los ms de cada arista (cada salto).
EOF
