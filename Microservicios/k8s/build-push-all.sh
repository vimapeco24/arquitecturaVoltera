#!/usr/bin/env bash
# Construye las 16 imagenes de backend Voltera para linux/amd64 y las sube a Harbor.
# Estrategia documentada que SI funciona con el Harbor de cert autofirmado:
#   docker buildx build --load  (carga al daemon local, que confia en el insecure registry)
#   docker push                 (push normal desde el daemon)
set -uo pipefail
REGISTRY="192.168.3.13/app-voltaire"
BASE="/Users/vimapeco/Downloads/ArquitectutaNuevaGenegeracion/Microservicios"
TAG="1.0.0"

# nombre_imagen : ruta_del_contexto
BUILDS=(
  "service-registry:${BASE}/service-registry"
  "facturacion-service:${BASE}/ServiciosDeNegocio/facturacion-service"
  "liquidacion-mensual-service:${BASE}/ServiciosDeNegocio/liquidacion-mensual-service"
  "liquidacion-p2p-service:${BASE}/ServiciosDeNegocio/liquidacion-p2p-service"
  "volumetria-service:${BASE}/ServiciosDeNegocio/volumetria-service"
  "tarifas-service:${BASE}/ServiciosDeNegocio/tarifas-service"
  "pagos-service:${BASE}/ServiciosDeNegocio/pagos-service"
  "siniestros-service:${BASE}/ServiciosDeNegocio/siniestros-service"
  "reaseguro-service:${BASE}/ServiciosDeNegocio/reaseguro-service"
  "telemetria-service:${BASE}/ServiciosDeNegocio/telemetria-service"
  "tarifa-eventos-service:${BASE}/ServiciosDeNegocio/tarifa-eventos-service"
  # Cadena EDA de negocio (laminas 02/03): consumidores con inbox/CQRS
  "habilitacion-service:${BASE}/ServiciosDeNegocio/habilitacion-service"
  "integracion-ami-service:${BASE}/ServiciosDeNegocio/integracion-ami-service"
  "ingesta-service:${BASE}/ServiciosDeNegocio/ingesta-service"
  "telemetria-core-service:${BASE}/ServiciosDeNegocio/telemetria-core-service"
  "notificaciones-service:${BASE}/ServiciosDeNegocio/notificaciones-service"
)

FAILED=()
for entry in "${BUILDS[@]}"; do
  name="${entry%%:*}"; ctx="${entry#*:}"
  echo "=========================================================="
  echo ">> BUILD $name (amd64)  ctx=$ctx"
  echo "=========================================================="
  if ! docker buildx build --builder desktop-linux --platform linux/amd64 \
        -t "${REGISTRY}/${name}:${TAG}" \
        -t "${REGISTRY}/${name}:latest" \
        --load "$ctx" >"/tmp/bp-build-$name.log" 2>&1; then
    echo "   BUILD FALLO $name"; tail -8 "/tmp/bp-build-$name.log"; FAILED+=("$name"); continue
  fi
  arch=$(docker image inspect "${REGISTRY}/${name}:${TAG}" --format '{{.Architecture}}' 2>/dev/null)
  echo "   build OK arch=$arch -> push..."
  if ! docker push "${REGISTRY}/${name}:${TAG}" >"/tmp/bp-push-$name.log" 2>&1; then
    echo "   PUSH FALLO $name"; tail -8 "/tmp/bp-push-$name.log"; FAILED+=("$name"); continue
  fi
  docker push "${REGISTRY}/${name}:latest" >>"/tmp/bp-push-$name.log" 2>&1 || true
  echo "   OK $name"
done

echo ""
echo "=========================================================="
if [ ${#FAILED[@]} -eq 0 ]; then
  echo "TODAS las 16 imagenes amd64 subidas a Harbor ($REGISTRY)."
else
  echo "FALLARON: ${FAILED[*]}"
  exit 1
fi
