#!/usr/bin/env bash
# ============================================================================
# VOLTERA — Aplica el reparto de carga + topes de memoria de forma SEGURA
# ----------------------------------------------------------------------------
# Idempotente y reversible. Usa 'kubectl patch --type merge', que NO reemplaza
# la plantilla del Deployment: solo fija nodeSelector y limits.
#
# Por que: evita que el master (6 GB, con control-plane) vuelva a entrar en OOM
# y tumbe MetalLB / apiserver / Istio. Ver voltera-scheduling.yaml.
#
# Uso:
#   ./aplicar-scheduling.sh            # aplica reparto + limits
#   ./aplicar-scheduling.sh --revert   # quita el nodeSelector (vuelve a libre)
# ============================================================================
set -euo pipefail
NS="voltera-app"
WORKER_LABEL='node-role.kubernetes.io/worker'

# Servicios que se anclan al worker para descargar el master.
ANCLAR_WORKER=(volumetria-service liquidacion-p2p-service siniestros-service)

revert=0
[ "${1:-}" = "--revert" ] && revert=1

echo "== Reparto de carga (nodeSelector) =="
for svc in "${ANCLAR_WORKER[@]}"; do
  if [ "$revert" = "1" ]; then
    echo " - quitando nodeSelector de $svc"
    kubectl -n "$NS" patch deploy "$svc" --type json \
      -p '[{"op":"remove","path":"/spec/template/spec/nodeSelector"}]' 2>/dev/null \
      && echo "   ok" || echo "   (ya no tenia nodeSelector)"
  else
    echo " - anclando $svc al worker"
    kubectl -n "$NS" patch deploy "$svc" --type merge \
      -p "{\"spec\":{\"template\":{\"spec\":{\"nodeSelector\":{\"$WORKER_LABEL\":\"\"}}}}}"
  fi
done

if [ "$revert" = "1" ]; then
  echo "Revert completado."
  exit 0
fi

echo
echo "== Topes de memoria (limits) para que ningun pod dispare el OOM del nodo =="
# Se fija un limit de memoria explicito a TODOS los servicios de negocio.
# Sin este limit, un pico de un servicio podia consumir toda la RAM del nodo.
for svc in volumetria-service liquidacion-p2p-service liquidacion-mensual-service \
           siniestros-service reaseguro-service tarifas-service pagos-service \
           facturacion-service cobros-service telemetria-service tarifa-eventos-service \
           habilitacion-service integracion-ami-service ingesta-service notificaciones-service; do
  cont=$(kubectl -n "$NS" get deploy "$svc" -o jsonpath='{.spec.template.spec.containers[0].name}' 2>/dev/null || true)
  [ -z "$cont" ] && { echo " - (omitido $svc: no existe)"; continue; }
  kubectl -n "$NS" patch deploy "$svc" --type json -p "[
    {\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/resources/limits/memory\",\"value\":\"512Mi\"}
  ]" 2>/dev/null && echo " - $svc limit=512Mi" \
    || kubectl -n "$NS" patch deploy "$svc" --type merge -p \
       "{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"$cont\",\"resources\":{\"limits\":{\"memory\":\"512Mi\"}}}]}}}}" \
       && echo " - $svc limit=512Mi (merge)"
done
# telemetria-core es mas pesado (redis+tsdb): 640Mi
kubectl -n "$NS" patch deploy telemetria-core-service --type merge \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"telemetria-core-service","resources":{"limits":{"memory":"640Mi"}}}]}}}}' \
  2>/dev/null && echo " - telemetria-core-service limit=640Mi" || true

echo
echo "Hecho. Verifica:  kubectl -n $NS get pods -o wide"
