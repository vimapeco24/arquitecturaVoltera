#!/usr/bin/env bash
# ============================================================================
# VOLTERA — Despliegue COMPLETO de la capa EDA (laminas 02/03/04)
# ----------------------------------------------------------------------------
# Orquesta, EN ORDEN, todo lo necesario para la arquitectura orientada a eventos
# con persistencia real y autoescalado KEDA:
#
#   (1) Persistencia      -> Redis + TimescaleDB (voltera-persistence.yaml)
#   (2) Broker + base EDA -> Redpanda + servicios siniestros/reaseguro/etc.
#   (3) Servicios negocio -> habilitacion/ingesta/telemetria-core/notif/ami
#   (4) KEDA              -> ScaledObjects por lag + Adaptador (Prometheus)
#   (5) Event mesh        -> (opcional) multi-region
#
# NO construye imagenes: para eso usa k8s/build-push-all.sh antes.
#
# Uso:
#   ./deploy-eda-completo.sh --dry-run     # valida contra el API server
#   ./deploy-eda-completo.sh               # aplica todo
#   ./deploy-eda-completo.sh --with-mesh   # aplica tambien el event mesh
#
# Requiere: kubectl con contexto apuntando al cluster (namespace voltera-app),
# imagenes publicadas en Harbor y, para KEDA, el operador instalado:
#   helm install keda kedacore/keda -n keda --create-namespace
# ============================================================================
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
NS="voltera-app"

# ⛔ PASO 0 OBLIGATORIO (todos los servicios, sin excepcion): leer y tener aplicado
#    Microservicios/voltera-k8s-qa-sync.patch (replicas 1, min=1/max=1, JVM tuning,
#    requests 256Mi, affinity worker, topics KEDA). Verificar con:
#      git apply --check Microservicios/voltera-k8s-qa-sync.patch
# ⚠️ ANTES DE DESPLEGAR: leer DESPLIEGUE-SEGURO.md (este directorio).
#    1) kubectl get pods -n voltera-app -o wide   -> saber en que nodo esta cada servicio
#    2) kubectl top nodes                         -> master debe estar < 85% mem
#    Desplegar de uno en uno y verificar 2/2 Ready entre cada uno (no saturar el master 6GB).

DRY=""
WITH_MESH=0
for arg in "$@"; do
  case "$arg" in
    --dry-run)   DRY="--dry-run=server" ;;
    --with-mesh) WITH_MESH=1 ;;
    *) echo "Uso: $0 [--dry-run] [--with-mesh]"; exit 1 ;;
  esac
done

apply() { echo ">> kubectl apply $DRY -f $1"; kubectl apply $DRY -f "$1"; }

echo "============================================================"
echo " VOLTERA — Deploy EDA completo  (dry-run='${DRY:-no}')"
echo "============================================================"

echo ">> (1) Persistencia: Redis + TimescaleDB"
apply "$HERE/voltera-persistence.yaml"
if [ -z "$DRY" ]; then
  echo "   esperando a TimescaleDB..."
  kubectl -n "$NS" rollout status deployment/timescaledb --timeout=180s || true
  echo "   creando base del inbox (si no existe)..."
  kubectl -n "$NS" exec deploy/timescaledb -- \
    psql -U voltera -d voltera_tsdb -tc "SELECT 1 FROM pg_database WHERE datname='voltera_inbox'" \
    | grep -q 1 || \
  kubectl -n "$NS" exec deploy/timescaledb -- \
    psql -U voltera -d voltera_tsdb -c "CREATE DATABASE voltera_inbox OWNER voltera;" || true
fi

echo ">> (2) Broker Redpanda + servicios EDA base (siniestros/reaseguro/telemetria/tarifa-eventos)"
apply "$HERE/voltera-eda.yaml"

echo ">> (3) Servicios de negocio de la cadena EDA (habilitacion/integracion-ami/ingesta/telemetria-core/notificaciones)"
apply "$HERE/voltera-negocio-eda.yaml"

echo ">> (3b) Perfil inbox-jdbc para los servicios EDA ya existentes (tarifas/facturacion/liquidacion-mensual)"
if [ -z "$DRY" ]; then
  for s in tarifas-service facturacion-service liquidacion-mensual-service; do
    kubectl -n "$NS" set env deployment/"$s" \
      SPRING_PROFILES_ACTIVE=inbox-jdbc \
      INBOX_URL=jdbc:postgresql://timescaledb.voltera-app:5432/voltera_inbox \
      INBOX_USER=voltera INBOX_PASSWORD=voltera 2>/dev/null \
      && echo "   env OK $s" || echo "   (omitido $s: no desplegado aun)"
  done
fi

echo ">> (4) KEDA: ScaledObjects AJUSTADOS AL HARDWARE (autoescalado por lag, max acotado)"
echo "   (si el CRD keda.sh no existe, estos recursos se omiten; instala KEDA primero)"
# Se usa voltera-keda-ajustado.yaml (max acotado a 4-6) en lugar de la lamina
# (max 48), para no saturar el cluster de 14GB y evitar OOM del nodo.
# El integracion-ami usa scaler Kafka (no Prometheus) porque no hay gateway MQTT real.
apply "$HERE/voltera-keda-ajustado.yaml" || echo "   KEDA no instalado: ScaledObjects omitidos."

echo ">> (4b) Reparto de carga + topes de memoria (estabilidad: evita OOM del master)"
if [ -z "$DRY" ]; then
  bash "$HERE/aplicar-scheduling.sh" || echo "   (scheduling: revisar manualmente)"
fi

if [ "$WITH_MESH" = "1" ]; then
  echo ">> (5) Event mesh multi-region"
  apply "$HERE/voltera-event-mesh.yaml"
else
  echo ">> (5) Event mesh OMITIDO (usa --with-mesh para aplicarlo)"
fi

echo "============================================================"
if [ -z "$DRY" ]; then
  echo " Verificacion rapida:"
  echo "   kubectl -n $NS get pods"
  echo "   kubectl -n $NS get scaledobject,hpa"
  echo "   kubectl -n $NS exec deploy/timescaledb -- psql -U voltera -d voltera_inbox -c '\\dt'"
fi
echo " Hecho."
