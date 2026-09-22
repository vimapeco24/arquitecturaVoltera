#!/usr/bin/env bash
# Reconstruye las 9 imagenes Voltera para linux/amd64 y las sube a Harbor.
# Necesario porque el Mac es arm64 y el cluster K8s es amd64.
set -euo pipefail
REGISTRY="192.168.3.13/app-voltaire"
BASE="/Users/vimapeco/Downloads/ArquitectutaNuevaGenegeracion/Microservicios"

# nombre_imagen : ruta_del_contexto
BUILDS=(
  "service-registry:${BASE}/service-registry"
  "facturacion-service:${BASE}/ServiciosDeNegocio/facturacion-service"
  "liquidacion-mensual-service:${BASE}/ServiciosDeNegocio/liquidacion-mensual-service"
  "liquidacion-p2p-service:${BASE}/ServiciosDeNegocio/liquidacion-p2p-service"
  "volumetria-service:${BASE}/ServiciosDeNegocio/volumetria-service"
  "tarifas-service:${BASE}/ServiciosDeNegocio/tarifas-service"
  "pagos-service:${BASE}/ServiciosDeNegocio/pagos-service"
)

docker buildx use desktop-linux
for entry in "${BUILDS[@]}"; do
  name="${entry%%:*}"; ctx="${entry#*:}"
  echo "=========================================================="
  echo ">> $name (amd64)"
  echo "=========================================================="
  docker buildx build --platform linux/amd64 \
    -t "${REGISTRY}/${name}:1.0.0" \
    -t "${REGISTRY}/${name}:latest" \
    --load "$ctx" >/tmp/build-$name.log 2>&1 || { echo "BUILD FALLO $name"; tail -5 /tmp/build-$name.log; exit 1; }
  arch=$(docker image inspect "${REGISTRY}/${name}:1.0.0" --format '{{.Architecture}}')
  echo "   arch=$arch  -> push..."
  docker push "${REGISTRY}/${name}:1.0.0" >/tmp/push-$name.log 2>&1 || { echo "PUSH FALLO $name"; tail -5 /tmp/push-$name.log; exit 1; }
  docker push "${REGISTRY}/${name}:latest" >>/tmp/push-$name.log 2>&1 || true
  echo "   OK $name"
done
echo ""
echo "TODAS las imagenes amd64 subidas a Harbor."
