#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | DEMO 4: Escalabilidad (HPA bajo carga)
# ----------------------------------------------------------------------------
# Genera carga sostenida contra facturacion-service para subir el CPU por encima
# del target (50%) y provocar que el HPA cree pods nuevos. Registra el instante
# de cada evento de escalado para reportar "de N a M pods en X segundos".
#
# Evidencia: la tabla de eventos con timestamps + kubectl get hpa.
# ============================================================================
set -euo pipefail
NS="${NS:-voltera-app}"
TARGET="${TARGET:-http://facturacion-service:8088/actuator/health}"
CONN="${CONN:-50}"
DUR="${DUR:-180s}"

echo ">> Estado inicial:"
kubectl -n "$NS" get hpa hpa-facturacion-service
REPL0="$(kubectl -n "$NS" get deploy facturacion-service -o jsonpath='{.status.replicas}')"
echo "Replicas iniciales: ${REPL0}   |   T0 = $(date +%T)"

echo
echo ">> Desplegando fortio y generando carga durante ${DUR} (c=${CONN})..."
kubectl -n "$NS" run fortio --image=fortio/fortio --restart=Never -- sleep 7200 >/dev/null 2>&1 || true
kubectl -n "$NS" wait --for=condition=Ready pod/fortio --timeout=90s
kubectl -n "$NS" exec fortio -c fortio -- \
  fortio load -c "$CONN" -t "$DUR" -qps 0 "$TARGET" >/tmp/fortio-load.log 2>&1 &
LOAD_PID=$!

echo
echo ">> Observando HPA y pods (Ctrl-C para cortar). Anota el T de cada nuevo pod:"
echo "   --- HPA (TARGETS/REPLICAS) cada 5s ---"
END=$(( $(date +%s) + 200 ))
while [ "$(date +%s)" -lt "$END" ]; do
  printf "%s | " "$(date +%T)"
  kubectl -n "$NS" get hpa hpa-facturacion-service \
    --no-headers -o custom-columns=NAME:.metadata.name,TARGETS:.status.currentMetrics,REPL:.status.currentReplicas 2>/dev/null \
    || kubectl -n "$NS" get hpa hpa-facturacion-service --no-headers
  sleep 5
done

echo
echo ">> Eventos de escalado (timestamps = tiempo hasta escalar):"
kubectl -n "$NS" describe hpa hpa-facturacion-service | sed -n '/Events:/,$p'

wait "$LOAD_PID" 2>/dev/null || true
echo ">> Fin. Limpieza: kubectl -n $NS delete pod fortio"
