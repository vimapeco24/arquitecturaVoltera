#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | DEMO 3: Latencia por salto (observabilidad)
# ----------------------------------------------------------------------------
# Istio expone por cada par (source_workload -> destination_workload) la metrica
#   istio_request_duration_milliseconds_bucket
# Con ella se calcula el percentil de latencia de CADA SALTO. Este script
# consulta Prometheus (port-forward) y muestra p50/p90/p99 por arista.
#
# Tambien puedes verlo visualmente en Kiali (Graph -> Response Time) y en el
# dashboard de Grafana "Istio Workload".
# ============================================================================
set -euo pipefail
NS="${NS:-voltera-app}"
PROM_NS="${PROM_NS:-istio-system}"

echo ">> Abriendo port-forward a Prometheus (localhost:9090)..."
kubectl -n "$PROM_NS" port-forward svc/prometheus 9090:9090 >/tmp/pf-prom.log 2>&1 &
PF_PID=$!
trap 'kill $PF_PID 2>/dev/null || true' EXIT
sleep 4

q() {  # $1 = percentil (0.5/0.9/0.99)
  local p="$1"
  local expr="histogram_quantile(${p}, sum(rate(istio_request_duration_milliseconds_bucket{reporter=\"destination\",destination_service_namespace=\"${NS}\"}[5m])) by (le, source_workload, destination_workload))"
  curl -s --data-urlencode "query=${expr}" http://localhost:9090/api/v1/query
}

fmt() {  # imprime source -> destination : valor ms  (requiere jq)
  if command -v jq >/dev/null 2>&1; then
    jq -r '.data.result[] | "\(.metric.source_workload // "?") -> \(.metric.destination_workload // "?") : \(.value[1]|tonumber|floor) ms"'
  else
    cat; echo "(instala 'jq' para un formato legible)"
  fi
}

echo; echo "=== Latencia por salto  p50 (ms) ==="; q 0.5  | fmt
echo; echo "=== Latencia por salto  p90 (ms) ==="; q 0.9  | fmt
echo; echo "=== Latencia por salto  p99 (ms) ==="; q 0.99 | fmt

echo
echo ">> Nota: si no hay datos, genera trafico primero (demos/demo-scalability.sh"
echo "   o cualquier curl al gateway) y vuelve a ejecutar."
