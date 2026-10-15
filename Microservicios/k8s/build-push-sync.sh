#!/usr/bin/env bash
# Reconstruye TODAS las imagenes de backend Voltera (amd64) con el codigo actual
# y las sube a Harbor usando EXACTAMENTE el tag que cada deployment usa hoy en el
# cluster, de modo que un `rollout restart` recoja el codigo nuevo.
#
# Estrategia Harbor autofirmado: buildx --load al daemon + docker push.
set -uo pipefail
REGISTRY="192.168.3.13/app-voltaire"
BASE="/Users/vimapeco/Downloads/ArquitectutaNuevaGenegeracion/Microservicios"
BUILDER="desktop-linux"

# imagen : contexto : tag_desplegado_en_cluster
BUILDS=(
  "service-registry:${BASE}/service-registry:1.0.0"
  "facturacion-service:${BASE}/ServiciosDeNegocio/facturacion-service:1.0.1"
  "liquidacion-mensual-service:${BASE}/ServiciosDeNegocio/liquidacion-mensual-service:1.0.1"
  "liquidacion-p2p-service:${BASE}/ServiciosDeNegocio/liquidacion-p2p-service:1.0.1"
  "volumetria-service:${BASE}/ServiciosDeNegocio/volumetria-service:1.0.1"
  "tarifas-service:${BASE}/ServiciosDeNegocio/tarifas-service:1.0.1"
  "pagos-service:${BASE}/ServiciosDeNegocio/pagos-service:1.0.0"
  "siniestros-service:${BASE}/ServiciosDeNegocio/siniestros-service:1.0.0"
  "reaseguro-service:${BASE}/ServiciosDeNegocio/reaseguro-service:1.0.0"
  "telemetria-service:${BASE}/ServiciosDeNegocio/telemetria-service:1.0.0"
  "tarifa-eventos-service:${BASE}/ServiciosDeNegocio/tarifa-eventos-service:1.0.0"
  "habilitacion-service:${BASE}/ServiciosDeNegocio/habilitacion-service:1.0.0"
  "integracion-ami-service:${BASE}/ServiciosDeNegocio/integracion-ami-service:1.0.0"
  "ingesta-service:${BASE}/ServiciosDeNegocio/ingesta-service:1.0.0"
  "telemetria-core-service:${BASE}/ServiciosDeNegocio/telemetria-core-service:1.0.0"
  "notificaciones-service:${BASE}/ServiciosDeNegocio/notificaciones-service:1.0.0"
  "cobros-service:${BASE}/ServiciosDeNegocio/cobros-service:1.0.0"
)

OK_LIST=(); FAILED=()
for entry in "${BUILDS[@]}"; do
  name="${entry%%:*}"; rest="${entry#*:}"; ctx="${rest%:*}"; tag="${rest##*:}"
  echo "=========================================================="
  echo ">> BUILD $name:$tag (amd64)  ctx=$ctx"
  echo "=========================================================="
  if ! docker buildx build --builder "$BUILDER" --platform linux/amd64 \
        -t "${REGISTRY}/${name}:${tag}" \
        -t "${REGISTRY}/${name}:latest" \
        --load "$ctx" >"/tmp/sync-build-$name.log" 2>&1; then
    echo "   BUILD FALLO $name"; tail -12 "/tmp/sync-build-$name.log"; FAILED+=("$name"); continue
  fi
  arch=$(docker image inspect "${REGISTRY}/${name}:${tag}" --format '{{.Architecture}}' 2>/dev/null)
  if [ "$arch" != "amd64" ]; then
    echo "   ARCH INVALIDA ($arch) $name"; FAILED+=("$name"); continue
  fi
  echo "   build OK arch=$arch -> push $tag + latest..."
  if ! docker push "${REGISTRY}/${name}:${tag}" >"/tmp/sync-push-$name.log" 2>&1; then
    echo "   PUSH FALLO $name"; tail -12 "/tmp/sync-push-$name.log"; FAILED+=("$name"); continue
  fi
  docker push "${REGISTRY}/${name}:latest" >>"/tmp/sync-push-$name.log" 2>&1 || true
  echo "   OK $name:$tag"; OK_LIST+=("$name:$tag")
done

echo ""
echo "=========================================================="
echo "OK (${#OK_LIST[@]}): ${OK_LIST[*]}"
if [ ${#FAILED[@]} -ne 0 ]; then
  echo "FALLARON (${#FAILED[@]}): ${FAILED[*]}"
  exit 1
fi
echo "TODAS las imagenes reconstruidas y subidas a Harbor."
