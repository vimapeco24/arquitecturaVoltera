#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | DEMO 1: verificar mTLS ESTRICTO entre microservicios
# ----------------------------------------------------------------------------
# Muestra 3 evidencias:
#   (A) el sidecar Envoy esta inyectado (2/2 contenedores por pod),
#   (B) la politica PeerAuthentication esta en STRICT,
#   (C) el trafico servicio->servicio va cifrado mTLS (istioctl authn/x describe),
#   (D) opcional: una conexion en TEXTO PLANO es RECHAZADA.
# ============================================================================
set -euo pipefail
NS="${NS:-voltera-app}"

echo "=== (A) Sidecars inyectados (esperar READY 2/2) ==="
kubectl -n "$NS" get pods -o wide

echo
echo "=== (B) PeerAuthentication (esperar mode: STRICT) ==="
kubectl -n "$NS" get peerauthentication -o yaml | grep -E "name:|mode:" || true

echo
echo "=== (C) Estado mTLS del trafico entrante a facturacion-service ==="
POD="$(kubectl -n "$NS" get pod -l app=facturacion-service -o jsonpath='{.items[0].metadata.name}')"
echo "Pod destino: $POD"
# istioctl x describe resume el estado de la conexion (mTLS on/off) para el pod.
istioctl x describe pod "$POD.$NS" || \
  echo "(instalar istioctl para el detalle; el candado en Kiali tambien lo evidencia)"

echo
echo "=== (D) Prueba negativa: cliente SIN sidecar en TEXTO PLANO debe fallar ==="
echo "Lanzando un pod efimero fuera de la malla (sin sidecar)..."
kubectl -n "$NS" run plaintext-test --rm -i --restart=Never \
  --annotations="sidecar.istio.io/inject=false" \
  --image=curlimages/curl -- \
  curl -s -o /dev/null -w "HTTP %{http_code}\n" --max-time 5 \
  http://facturacion-service:8088/actuator/health \
  || echo ">> Conexion en texto plano RECHAZADA (esperado con mTLS STRICT). OK."

echo
echo ">> En Kiali (Graph) los enlaces con mTLS aparecen con un CANDADO."
