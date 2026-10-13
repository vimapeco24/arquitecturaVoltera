#!/usr/bin/env bash
# ============================================================================
# Voltera · Importa los dashboards de Grafana vía la API HTTP.
# ----------------------------------------------------------------------------
# Deja PROVISIONADOS en Grafana los dos dashboards de Voltera:
#   - grafana-voltera-overview.json   (overview mesh + EDA, 17 servicios)
#   - grafana-telemetria-dashboard.json (caso Telemetría EDA)
#
# Uso:
#   GRAFANA_URL=http://192.168.3.11:32421 \
#   GRAFANA_USER=admin GRAFANA_PASS=admin \
#   ./grafana-import-dashboards.sh
#
# Alternativa con token (service account de Grafana):
#   GRAFANA_URL=... GRAFANA_TOKEN=glsa_xxx ./grafana-import-dashboards.sh
#
# Nota: el JSON ya viene envuelto en {"dashboard": {...}, "overwrite": true},
# que es exactamente el cuerpo que espera POST /api/dashboards/db.
# ============================================================================
set -euo pipefail

GRAFANA_URL="${GRAFANA_URL:-http://192.168.3.11:32421}"
DIR="$(cd "$(dirname "$0")" && pwd)"
DASHBOARDS=(
  "grafana-voltera-overview.json"
  "grafana-telemetria-dashboard.json"
)

# Cabecera de autenticación: token (Bearer) o basic user:pass.
if [[ -n "${GRAFANA_TOKEN:-}" ]]; then
  AUTH=(-H "Authorization: Bearer ${GRAFANA_TOKEN}")
else
  AUTH=(-u "${GRAFANA_USER:-admin}:${GRAFANA_PASS:-admin}")
fi

echo ">> Grafana: ${GRAFANA_URL}"
for f in "${DASHBOARDS[@]}"; do
  path="${DIR}/${f}"
  if [[ ! -f "$path" ]]; then echo "  !! no existe ${f}, se omite"; continue; fi
  echo ">> Importando ${f} ..."
  code=$(curl -sS -o /tmp/graf-resp.json -w "%{http_code}" \
    "${AUTH[@]}" \
    -H "Content-Type: application/json" \
    -X POST "${GRAFANA_URL%/}/api/dashboards/db" \
    --data-binary "@${path}" || echo "000")
  if [[ "$code" == "200" ]]; then
    url=$(grep -o '"url":"[^"]*"' /tmp/graf-resp.json | head -1 | cut -d'"' -f4)
    echo "   OK (${code}) -> ${GRAFANA_URL%/}${url}"
  else
    echo "   FALLO (${code}): $(cat /tmp/graf-resp.json)"
  fi
done

cat <<EOF

>> Hecho. Abre en Grafana:
   - Voltera — Plataforma (Overview Mesh + EDA)   uid: voltera-overview
   - Voltera — Telemetría EDA                     uid: voltera-telemetria-eda

   Requisitos de métricas:
   - istio_requests_total / istio_request_duration_milliseconds_bucket (addon Istio Prometheus)
   - kube_pod_status_ready / kube_pod_container_status_restarts_total (kube-state-metrics)
EOF
