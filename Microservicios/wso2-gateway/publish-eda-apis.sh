#!/usr/bin/env bash
# Publica SOLO los 4 servicios EDA faltantes como APIs en WSO2 API Manager.
# Backend: Istio Gateway (http://192.168.3.220/<ruta>).
set -uo pipefail
WSO2="${WSO2:-https://192.168.3.13:9443}"
BACKEND="${BACKEND:-http://192.168.3.220}"
USER="${WSO2_USER:-admin}"; PASS="${WSO2_PASS:-admin}"
CURL="curl -sk"
VERSION="v1"

APIS=(
  "SiniestrosAPI:/siniestros:/siniestros"
  "ReaseguroAPI:/reaseguro:/reaseguro"
  "TelemetriaAPI:/telemetria:/telemetria"
  "TarifaEventosAPI:/tarifa-eventos:/tarifa-eventos"
)

echo ">> DCR..."
DCR=$($CURL -X POST "${WSO2}/client-registration/v0.17/register" \
  -H "Authorization: Basic $(printf '%s:%s' "$USER" "$PASS" | base64)" \
  -H "Content-Type: application/json" \
  -d '{"callbackUrl":"https://localhost","clientName":"voltera_eda_pub","owner":"'"$USER"'","grantType":"password refresh_token","saasApp":true}')
CID=$(echo "$DCR" | python3 -c "import sys,json;print(json.load(sys.stdin)['clientId'])")
CSEC=$(echo "$DCR" | python3 -c "import sys,json;print(json.load(sys.stdin)['clientSecret'])")

echo ">> Token publisher..."
SCOPES="apim:api_create apim:api_publish apim:api_view"
TOKEN=$($CURL -X POST "${WSO2}/oauth2/token" \
  -H "Authorization: Basic $(printf '%s:%s' "$CID" "$CSEC" | base64)" \
  -d "grant_type=password&username=${USER}&password=${PASS}&scope=${SCOPES}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

PUB="${WSO2}/api/am/publisher/v4"

for entry in "${APIS[@]}"; do
  name="${entry%%:*}"; rest="${entry#*:}"; ctx="${rest%%:*}"; route="${rest##*:}"
  echo ">> ${name} (ctx ${ctx}) -> ${BACKEND}${route}"
  BODY=$(python3 - "$name" "$ctx" "$VERSION" "${BACKEND}${route}" <<'PY'
import json,sys
name,ctx,ver,backend=sys.argv[1:5]
print(json.dumps({
 "name":name,"context":ctx,"version":ver,"provider":"admin","gatewayVendor":"wso2",
 "endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":backend},"sandbox_endpoints":{"url":backend}},
 "policies":["Unlimited"],
 "operations":[{"target":"/*","verb":v,"authType":"None","throttlingPolicy":"Unlimited"} for v in ("GET","POST","PUT","DELETE")]
}))
PY
)
  CREATE=$($CURL -X POST "${PUB}/apis" -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d "$BODY")
  APIID=$(echo "$CREATE" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
  if [ -z "$APIID" ]; then echo "   !! error/ya existe ${name}: $(echo "$CREATE" | head -c 200)"; continue; fi
  REVID=$($CURL -X POST "${PUB}/apis/${APIID}/revisions" -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d '{"description":"auto"}' | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
  $CURL -X POST "${PUB}/apis/${APIID}/deploy-revision?revisionId=${REVID}" -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d '[{"name":"Default","vhost":"localhost","displayOnDevportal":true}]' >/dev/null
  $CURL -X POST "${PUB}/apis/change-lifecycle?apiId=${APIID}&action=Publish" -H "Authorization: Bearer ${TOKEN}" >/dev/null
  echo "   OK ${name} publicada"
done
echo "LISTO."
