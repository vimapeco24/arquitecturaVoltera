#!/usr/bin/env bash
# ============================================================================
# REANUDAR: push de las 7 imagenes ya construidas + rollout de TODO el cluster
# ----------------------------------------------------------------------------
# Se interrumpio el push por una caida de la LAN 192.168.3.x. Las 7 imagenes
# siguientes YA estan construidas en el daemon local (amd64); este script solo
# espera a que Harbor vuelva, re-logea, las pushea y luego reinicia todos los
# deployments para que tomen el codigo nuevo.
#
# Uso (credenciales por variable de entorno, NO versionadas):
#   export SSHPASS='<pass-nodo>'            # pass del nodo SSH
#   export HARBOR_PASS='<pass-harbor>'      # pass del registry Harbor
#   ./resume-push-rollout.sh
# ============================================================================
set -uo pipefail
REGISTRY="192.168.3.13/app-voltaire"
HARBOR_HOST="192.168.3.13"
HARBOR_USER="${HARBOR_USER:-voltera-user}"
HARBOR_PASS="${HARBOR_PASS:?define HARBOR_PASS en el entorno}"
NODE="${VOLTERA_NODE:-atiesia@192.168.3.11}"

# Las 7 que quedaron sin pushear (nombre:tag). Las otras 10 ya estan en Harbor.
PENDING=(
  "tarifa-eventos-service:1.0.0"
  "habilitacion-service:1.0.0"
  "integracion-ami-service:1.0.0"
  "ingesta-service:1.0.0"
  "telemetria-core-service:1.0.0"
  "notificaciones-service:1.0.0"
  "cobros-service:1.0.0"
)

# Todos los deployments de negocio a reiniciar tras el push (todos los que usan Harbor).
ALL_DEPLOYS=(
  service-registry facturacion-service liquidacion-mensual-service liquidacion-p2p-service
  volumetria-service tarifas-service pagos-service siniestros-service reaseguro-service
  telemetria-service tarifa-eventos-service habilitacion-service integracion-ami-service
  ingesta-service telemetria-core-service notificaciones-service cobros-service
)

echo ">> Esperando a que Harbor ($HARBOR_HOST:443) vuelva..."
until nc -z -G 4 "$HARBOR_HOST" 443 2>/dev/null; do printf '.'; sleep 15; done
echo " OK."

echo ">> Login a Harbor..."
echo "$HARBOR_PASS" | docker login "$HARBOR_HOST" -u "$HARBOR_USER" --password-stdin

echo ">> Push de las 7 imagenes pendientes (ya construidas)..."
FAILED=()
for nt in "${PENDING[@]}"; do
  echo "   push $nt + latest"
  name="${nt%%:*}"
  if docker push "${REGISTRY}/${nt}"; then
    docker push "${REGISTRY}/${name}:latest" || true
  else
    FAILED+=("$nt")
  fi
done
if [ ${#FAILED[@]} -ne 0 ]; then
  echo "PUSH FALLO: ${FAILED[*]} (reintenta este script)"; exit 1
fi
echo ">> Las 17 imagenes estan en Harbor."

echo ">> Rollout restart de TODOS los deployments (via SSH)..."
sshpass -e ssh -o StrictHostKeyChecking=accept-new "$NODE" "
  for d in ${ALL_DEPLOYS[*]}; do
    kubectl -n voltera-app rollout restart deployment/\$d 2>/dev/null && echo \"   restart \$d\" || echo \"   (omitido \$d)\"
  done
"
echo ">> Hecho. Verifica con:  kubectl -n voltera-app get pods"
