#!/usr/bin/env bash
#
# Publica automaticamente los 6 microservicios Voltera como APIs gestionadas
# en WSO2 API Manager, usando la REST API del Publisher.
#
# Flujo por cada servicio:
#   1) Registrar cliente dinamico (DCR) -> client_id/secret
#   2) Obtener token OAuth2 con scopes de publisher
#   3) Crear la API (contexto + backend endpoint) con un recurso comodin /*
#   4) Publicar la API (ciclo de vida -> PUBLISHED)
#
# Resultado: cada API queda consumible via el gateway:
#   https://<wso2host>:8243/<contexto>/<version>/...
#
# Uso:
#   ./publish-apis.sh                 # usa https://localhost:9443 y backend por defecto
#   WSO2=https://192.168.3.11:9443 BACKEND=http://192.168.3.220 ./publish-apis.sh
#
set -euo pipefail

WSO2="${WSO2:-https://localhost:9443}"
BACKEND="${BACKEND:-http://192.168.3.220}"
USER="${WSO2_USER:-admin}"
PASS="${WSO2_PASS:-admin}"
CURL="curl -sk"

# servicio : contexto : ruta-backend
APIS=(
  "TarifasAPI:/tarifas:/tarifas"
  "VolumetriaAPI:/volumetria:/volumetria"
  "LiquidacionP2PAPI:/liquidacion-p2p:/liquidacion-p2p"
  "LiquidacionMensualAPI:/liquidacion-mensual:/liquidacion-mensual"
  "FacturacionAPI:/facturacion:/facturacion"
  "PagosAPI:/pagos:/pagos"
  "SiniestrosAPI:/siniestros:/siniestros"
  "ReaseguroAPI:/reaseguro:/reaseguro"
  "TelemetriaAPI:/telemetria:/telemetria"
  "TarifaEventosAPI:/tarifa-eventos:/tarifa-eventos"
)
VERSION="v1"

echo ">> Registrando cliente dinamico (DCR)..."
DCR=$($CURL -X POST "${WSO2}/client-registration/v0.17/register" \
  -H "Authorization: Basic $(printf '%s:%s' "$USER" "$PASS" | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "callbackUrl":"https://localhost",
    "clientName":"voltera_publisher_script",
    "owner":"'"$USER"'",
    "grantType":"password refresh_token",
    "saasApp":true
  }')
CID=$(echo "$DCR" | python3 -c "import sys,json;print(json.load(sys.stdin)['clientId'])")
CSEC=$(echo "$DCR" | python3 -c "import sys,json;print(json.load(sys.stdin)['clientSecret'])")

echo ">> Obteniendo token OAuth2 (scopes de publisher)..."
SCOPES="apim:api_create apim:api_publish apim:api_view"
TOKEN=$($CURL -X POST "${WSO2}/oauth2/token" \
  -H "Authorization: Basic $(printf '%s:%s' "$CID" "$CSEC" | base64)" \
  -d "grant_type=password&username=${USER}&password=${PASS}&scope=${SCOPES}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

PUB="${WSO2}/api/am/publisher/v4"

for entry in "${APIS[@]}"; do
  name="${entry%%:*}"; rest="${entry#*:}"; ctx="${rest%%:*}"; route="${rest##*:}"
  echo ">> Creando API ${name} (contexto ${ctx}) -> backend ${BACKEND}${route}"

  BODY=$(python3 - "$name" "$ctx" "$VERSION" "${BACKEND}${route}" <<'PY'
import json,sys
name,ctx,ver,backend=sys.argv[1:5]
api={
 "name":name,"context":ctx,"version":ver,
 "provider":"admin","gatewayVendor":"wso2",
 "endpointConfig":{
   "endpoint_type":"http",
   "production_endpoints":{"url":backend},
   "sandbox_endpoints":{"url":backend}
 },
 "policies":["Unlimited"],
 "operations":[
   {"target":"/*","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"},
   {"target":"/*","verb":"POST","authType":"None","throttlingPolicy":"Unlimited"},
   {"target":"/*","verb":"PUT","authType":"None","throttlingPolicy":"Unlimited"},
   {"target":"/*","verb":"DELETE","authType":"None","throttlingPolicy":"Unlimited"}
 ]
}
print(json.dumps(api))
PY
)
  CREATE=$($CURL -X POST "${PUB}/apis" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" -d "$BODY")
  APIID=$(echo "$CREATE" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
  if [ -z "$APIID" ]; then echo "   !! error creando ${name}: $CREATE"; continue; fi

  # WSO2 4.x: hay que crear una revision y DESPLEGARLA al gateway.
  echo "   creando revision..."
  REV=$($CURL -X POST "${PUB}/apis/${APIID}/revisions" \
    -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
    -d '{"description":"auto"}')
  REVID=$(echo "$REV" | python3 -c "import sys,json;print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)

  echo "   desplegando revision al gateway..."
  $CURL -X POST "${PUB}/apis/${APIID}/deploy-revision?revisionId=${REVID}" \
    -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
    -d '[{"name":"Default","vhost":"localhost","displayOnDevportal":true}]' >/dev/null

  echo "   publicando ${name} (id=${APIID})..."
  $CURL -X POST "${PUB}/apis/change-lifecycle?apiId=${APIID}&action=Publish" \
    -H "Authorization: Bearer ${TOKEN}" >/dev/null
  echo "   OK ${name} -> ${WSO2%:*}:8243${ctx}/${VERSION}/..."
done

echo ""
echo "APIs publicadas. Consumo via gateway:"
for entry in "${APIS[@]}"; do
  rest="${entry#*:}"; ctx="${rest%%:*}"
  echo "  https://<wso2host>:8243${ctx}/${VERSION}/api/v1/..."
done
