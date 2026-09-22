#!/usr/bin/env bash
# Construye las imagenes EDA (siniestros + reaseguro + telemetria + tarifa-eventos) y las sube a Harbor.
# Ejecutar desde tu Mac; las imagenes se publican en el registry del Synology.
set -euo pipefail
REGISTRY="192.168.3.13/app-voltaire"
SERVICIOS=("siniestros-service" "reaseguro-service" "telemetria-service" "tarifa-eventos-service")
TAG="1.0.0"

for svc in "${SERVICIOS[@]}"; do
  echo "=== Building $svc ==="
  docker buildx build --platform linux/amd64 \
    -t "$REGISTRY/$svc:$TAG" \
    --push \
    "../ServiciosDeNegocio/$svc"
done
echo "Done. Images pushed to $REGISTRY."
