#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | Aplica TODA la capa de service mesh en orden.
# Uso:  ./apply-all.sh            (aplica)
#       ./apply-all.sh --dry-run  (solo valida contra el API server)
# ============================================================================
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
DRY=""
[ "${1:-}" = "--dry-run" ] && DRY="--dry-run=server"

echo ">> (0) Namespace + inyeccion de sidecar"
kubectl apply $DRY -f "$HERE/00-namespace.yaml"

echo ">> (0b) Re-aplicando microservicios (ahora con CPU requests + replicas=2, necesarios para el HPA)"
kubectl apply $DRY -f "$HERE/../voltera-services.yaml"

echo ">> (1) mTLS STRICT"
kubectl apply $DRY -f "$HERE/01-mtls-peerauth.yaml"

echo ">> (2) Circuit breaker (DestinationRules)"
kubectl apply $DRY -f "$HERE/02-destinationrules-circuitbreaker.yaml"

echo ">> (3) Telemetry (tracing + access logs + metricas)"
kubectl apply $DRY -f "$HERE/03-telemetry-tracing.yaml"

echo ">> (4) HPA (escalabilidad)"
kubectl apply $DRY -f "$HERE/04-hpa.yaml"

echo ">> (5) PodDisruptionBudget (disponibilidad)"
kubectl apply $DRY -f "$HERE/05-availability-pdb.yaml"

if [ -z "$DRY" ]; then
  echo ">> Reiniciando SOLO los microservicios + registry para inyectar el sidecar"
  echo "   (WSO2 se deja fuera de la malla a proposito, ver k8s-wso2.yaml)."
  for d in service-registry facturacion-service liquidacion-mensual-service \
           liquidacion-p2p-service volumetria-service tarifas-service pagos-service; do
    kubectl -n voltera-app rollout restart deploy "$d" 2>/dev/null || true
  done
  echo ">> Verifica que los pods de los micros queden 2/2 (app + istio-proxy),"
  echo "   y que wso2am siga 1/1 (sin sidecar):"
  echo "   kubectl -n voltera-app get pods"
fi
echo ">> Hecho."
