#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | DEMO 5: Disponibilidad / Failover (activo-activo)
# ----------------------------------------------------------------------------
# Con 2+ replicas, generamos trafico continuo y MATAMOS 1 pod. El mesh reenvia
# a la replica sana (retries + outlierDetection). Medimos cuantas peticiones
# fallan (idealmente 0) y el tiempo hasta que se recrea el pod.
#
# Evidencia: resumen fortio (Code 200 vs 5xx) + timestamps de kill/recreacion.
# ============================================================================
set -euo pipefail
NS="${NS:-voltera-app}"
APP="${APP:-facturacion-service}"
PORT="${PORT:-8088}"
DUR="${DUR:-60s}"

echo ">> Replicas actuales de ${APP}:"
kubectl -n "$NS" get pods -l app="$APP" -o wide

echo
echo ">> Iniciando trafico continuo (${DUR}) con fortio..."
kubectl -n "$NS" run fortio --image=fortio/fortio --restart=Never -- sleep 7200 >/dev/null 2>&1 || true
kubectl -n "$NS" wait --for=condition=Ready pod/fortio --timeout=90s
kubectl -n "$NS" exec fortio -c fortio -- \
  fortio load -c 8 -t "$DUR" -qps 50 "http://${APP}:${PORT}/actuator/health" >/tmp/fortio-ha.log 2>&1 &
LOAD_PID=$!

sleep 10
VICTIM="$(kubectl -n "$NS" get pod -l app="$APP" -o jsonpath='{.items[0].metadata.name}')"
echo ">> [$(date +%T)] MATANDO pod: $VICTIM"
kubectl -n "$NS" delete pod "$VICTIM" --grace-period=0 --force

echo ">> Observando recuperacion (el Deployment recrea la replica):"
kubectl -n "$NS" get pods -l app="$APP" -w &
WATCH_PID=$!
sleep 40
kill "$WATCH_PID" 2>/dev/null || true

echo
echo ">> Esperando fin de la carga y resumen (mira 'Code 200' vs '5xx'):"
wait "$LOAD_PID" 2>/dev/null || true
grep -E "Code |All done|target 50%|Sockets" /tmp/fortio-ha.log || cat /tmp/fortio-ha.log

echo
echo ">> Interpretacion: 100% Code 200 (o casi) = failover sin downtime perceptible."
echo ">> Limpieza: kubectl -n $NS delete pod fortio"
