#!/usr/bin/env bash
#
# deploy-lamina10.sh — Despliega los cambios de la lámina 10 (INBOX idempotencia +
# ORQUESTACIÓN/outbox/timeout en tarifa-eventos-service) al clúster k8s del Synology
# y publica el frontend.
#
# REQUISITOS DE RED: la Mac debe tener ruta a la LAN 192.168.3.x y los hosts del
# clúster vivos:
#   - Synology/k8s:    ssh atiesia@192.168.3.11   (pass por ENV: SSH_PASS)
#   - Registry Harbor: 192.168.3.13/app-voltaire  (credenciales fuera del repo)
#   - Namespace: voltera-app
#
# Verifica conectividad primero:
#   nc -z -G 5 192.168.3.11 22 && echo OK || echo "clúster inaccesible"
#
# Uso:
#   ./deploy-lamina10.sh backend     # build+push+rollout del tarifa-eventos-service
#   ./deploy-lamina10.sh frontend    # ng build + deploy (Vercel con token por ENV)
#   ./deploy-lamina10.sh all
#
set -euo pipefail

REGISTRY="192.168.3.13/app-voltaire"
SVC="tarifa-eventos-service"
TAG="1.0.0"
NS="voltera-app"
SSH_HOST="atiesia@192.168.3.11"
# La contraseña NUNCA se hardcodea. Pásala por variable de entorno:
#   SSH_PASS='...' ./deploy-lamina10.sh backend
SSH_PASS="${SSH_PASS:?Define SSH_PASS en el entorno antes de ejecutar}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"

deploy_backend() {
  echo "=== [1/3] Build imagen amd64 de $SVC (carga al daemon local) ==="
  # Estrategia que SÍ funciona con el Harbor autofirmado (ver ESTADO_SESION_EDA.md):
  # build con --load (no --push), luego docker push desde el daemon que confía en el registry inseguro.
  docker buildx build --platform linux/amd64 \
    -t "$REGISTRY/$SVC:$TAG" -t "$REGISTRY/$SVC:latest" \
    --load \
    "$ROOT/ServiciosDeNegocio/$SVC"

  echo "=== [2/3] Push a Harbor ==="
  docker push "$REGISTRY/$SVC:$TAG"
  docker push "$REGISTRY/$SVC:latest"

  echo "=== [3/3] Rollout en k8s (namespace $NS) ==="
  # El manifiesto usa tag fijo 1.0.0; forzamos el restart para bajar la imagen re-publicada.
  sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "$SSH_HOST" \
    "kubectl -n $NS rollout restart deployment/$SVC && kubectl -n $NS rollout status deployment/$SVC --timeout=120s"
  echo "Backend desplegado."
}

deploy_frontend() {
  echo "=== Frontend: ng build (producción) ==="
  cd "$ROOT/voltera-frontend"
  npx ng build --configuration production
  echo "Build OK en dist/voltera-frontend."
  if [ -n "${VERCEL_TOKEN:-}" ]; then
    echo "=== Deploy a Vercel ==="
    npx vercel deploy --prod --yes --token "$VERCEL_TOKEN"
  else
    echo "VERCEL_TOKEN no definido. Para desplegar:"
    echo "  VERCEL_TOKEN=xxxx ./deploy-lamina10.sh frontend"
    echo "(usa un token NUEVO; no lo pegues en chat)."
  fi
}

case "${1:-all}" in
  backend)  deploy_backend ;;
  frontend) deploy_frontend ;;
  all)      deploy_backend; deploy_frontend ;;
  *) echo "Uso: $0 {backend|frontend|all}"; exit 1 ;;
esac
