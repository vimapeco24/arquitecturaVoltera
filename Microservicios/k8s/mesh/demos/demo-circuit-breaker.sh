#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | DEMO 2: Circuit Breaker (aislamiento de fallos)
# ----------------------------------------------------------------------------
# Idea: saturamos facturacion-service con muchas conexiones concurrentes.
# La DestinationRule (connectionPool: maxConnections=5, http1MaxPendingRequests=5)
# hace que Envoy ABRA el breaker y responda 503 inmediato (flag "UO" = Upstream
# Overflow) en vez de dejar caer el servicio. El RESTO de micros sigue operando.
#
# Evidencia clave: en el resumen de fortio veras respuestas 200 y 503; las 503
# son las cortadas por el breaker. En Envoy: metrica upstream_cx_overflow.
# ============================================================================
set -euo pipefail
NS="${NS:-voltera-app}"
TARGET="${TARGET:-http://facturacion-service:8088/actuator/health}"
CONN="${CONN:-30}"        # 30 conexiones concurrentes (>> maxConnections=5)
CALLS="${CALLS:-300}"     # total de peticiones

echo ">> Desplegando cliente de carga fortio en la malla..."
kubectl -n "$NS" run fortio --image=fortio/fortio --restart=Never -- sleep 3600 >/dev/null 2>&1 || true
kubectl -n "$NS" wait --for=condition=Ready pod/fortio --timeout=90s

echo
echo ">> (baseline) 1 conexion, sin saturar -> deberia ser 100% 200 OK"
kubectl -n "$NS" exec fortio -c fortio -- \
  fortio load -c 1 -n 20 -qps 0 "$TARGET" 2>&1 | grep -E "Code |All done" || true

echo
echo ">> (breaker) $CONN conexiones concurrentes, $CALLS peticiones -> aparecen 503 (UO)"
kubectl -n "$NS" exec fortio -c fortio -- \
  fortio load -c "$CONN" -n "$CALLS" -qps 0 "$TARGET" 2>&1 | grep -E "Code |All done" || true

echo
echo ">> Contador de cortes del breaker en el sidecar (upstream_cx_overflow / pending_overflow):"
POD="$(kubectl -n "$NS" get pod -l app=facturacion-service -o jsonpath='{.items[0].metadata.name}')"
kubectl -n "$NS" exec "$POD" -c istio-proxy -- \
  pilot-agent request GET stats 2>/dev/null | grep -E "facturacion.*(overflow|ejections)" || \
  echo "(usar: istioctl proxy-config ... o revisar el dashboard de Envoy)"

echo
echo ">> Comprobando que OTRO micro (volumetria = registro de casas) SIGUE OK:"
kubectl -n "$NS" exec fortio -c fortio -- \
  fortio load -c 2 -n 20 -qps 0 http://volumetria-service:8083/actuator/health 2>&1 | grep -E "Code |All done" || true

echo
echo ">> Limpieza: kubectl -n $NS delete pod fortio"
