#!/usr/bin/env bash
# =============================================================================
# Voltera · Experimento reproducible — Marketplace P2P + anclaje on-chain
# =============================================================================
# Ejecuta el flujo COMPLETO de principio a fin contra la infraestructura real
# (frontend Vercel BFF → WSO2 → liquidacion-p2p  y  → blockchain-gateway Render
# → Polygon Amoy) e imprime la evidencia de cada paso (txHash, bloque, gas,
# enlace a PolygonScan). Pensado para una sustentación/entrega: determinista,
# repetible y sin depender del estado del navegador.
#
# USO:
#   ./experimento-p2p.sh                 # corre el experimento con datos demo
#   VENDEDOR=PRO-X COMPRADOR=PRO-Y ./experimento-p2p.sh
#
# REQUISITOS: bash, curl, python3 (para parsear JSON; viene en macOS).
# No requiere Docker, VPN ni acceso a la LAN: usa el BFF público de Vercel y el
# gateway público de Render.
# =============================================================================
set -euo pipefail

# ---- Configuración (se puede sobreescribir por variables de entorno) --------
FRONT="${FRONT:-https://voltera-frontend.vercel.app}"
GATEWAY="${GATEWAY:-https://voltera-blockchain-gateway.onrender.com}"
VENDEDOR="${VENDEDOR:-PRO-SOLAR-01}"
COMPRADOR="${COMPRADOR:-PRO-CASA-07}"
COMUNIDAD="${COMUNIDAD:-COMUNIDAD-SOLAR-01}"
KWH_VENTA="${KWH_VENTA:-12}"
PRECIO_VENTA="${PRECIO_VENTA:-420}"
KWH_COMPRA="${KWH_COMPRA:-10}"
PRECIO_COMPRA="${PRECIO_COMPRA:-500}"

PY="${PY:-python3}"
# Si el python3 por defecto está bloqueado (licencia Xcode en macOS), usar el de CommandLineTools.
if ! printf '{}' | "$PY" -c 'import sys,json;json.load(sys.stdin)' >/dev/null 2>&1; then
  if [ -x /Library/Developer/CommandLineTools/usr/bin/python3 ]; then
    PY=/Library/Developer/CommandLineTools/usr/bin/python3
  fi
fi
OUT_DIR="${OUT_DIR:-evidencia-experimento}"
TS="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT_DIR"
REGISTRO="$OUT_DIR/experimento-$TS.json"

# ---- Utilidades de salida ---------------------------------------------------
c_reset=$'\033[0m'; c_b=$'\033[1m'; c_g=$'\033[32m'; c_y=$'\033[33m'; c_r=$'\033[31m'; c_c=$'\033[36m'
paso()  { printf '\n%s▶ PASO %s · %s%s\n' "$c_b$c_c" "$1" "$2" "$c_reset"; }
ok()    { printf '  %s✓%s %s\n' "$c_g" "$c_reset" "$1"; }
warn()  { printf '  %s!%s %s\n' "$c_y" "$c_reset" "$1"; }
err()   { printf '  %s✗%s %s\n' "$c_r" "$c_reset" "$1"; }
# jget CLAVE → imprime d["CLAVE"] del JSON recibido por stdin (tolerante a errores)
jget()  { "$PY" -c "
import sys, json
clave = sys.argv[1]
try:
    d = json.load(sys.stdin)
    v = d.get(clave)
    print('' if v is None else v)
except Exception:
    print('')
" "$1"; }

echo "======================================================================="
echo "  VOLTERA · Experimento Marketplace P2P + anclaje on-chain"
echo "  $(date)"
echo "  Front:   $FRONT"
echo "  Gateway: $GATEWAY"
echo "======================================================================="

# =============================================================================
# PASO 0 · Pre-flight: la cadena responde (despierta el free tier de Render)
# =============================================================================
paso 0 "Verificar que el gateway y la cadena responden (cold-start Render ~60-90 s)"
RED_JSON=""
for intento in 1 2 3; do
  RED_JSON="$(curl -s --max-time 120 "$GATEWAY/api/v1/anclajes/red" || true)"
  [ -n "$RED_JSON" ] && break
  warn "sin respuesta (cold-start), reintento $intento/3…"
done
if [ -z "$RED_JSON" ]; then
  err "El gateway no respondió tras 3 intentos. Reintenta en ~60 s."; exit 1
fi
DISPONIBLE="$(printf '%s' "$RED_JSON" | jget disponible)"
CONTRATO="$(printf '%s' "$RED_JSON" | jget contrato)"
CHAINID="$(printf '%s' "$RED_JSON" | jget chainId)"
EXPLORER="$(printf '%s' "$RED_JSON" | jget explorer)"
RED_NOMBRE="$(printf '%s' "$RED_JSON" | jget red)"
ok "Red: $RED_NOMBRE · chainId $CHAINID"
ok "Contrato: $CONTRATO"
if [ "$DISPONIBLE" = "True" ] || [ "$DISPONIBLE" = "true" ]; then
  ok "Cadena conectada (disponible=true)"
else
  warn "Cadena en modo demo (disponible=false): los anclajes quedarán PENDIENTE."
fi

# =============================================================================
# PASO 1 · Autenticación OAuth2 (BFF Vercel → WSO2) para poder casar
# =============================================================================
paso 1 "Obtener token OAuth2 del BFF (/api/token → WSO2)"
TOKEN_JSON="$(curl -s --max-time 40 -X POST "$FRONT/api/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' || true)"
TOKEN="$(printf '%s' "$TOKEN_JSON" | jget access_token)"
if [ -z "$TOKEN" ]; then
  err "No se obtuvo token OAuth2. Respuesta: $TOKEN_JSON"; exit 1
fi
EXPIRA="$(printf '%s' "$TOKEN_JSON" | jget expires_in)"
ok "Token OAuth2 obtenido (expira en ${EXPIRA} s)"

# =============================================================================
# PASO 2 · Mostrar las órdenes del experimento y las reglas de casación
# =============================================================================
paso 2 "Órdenes a casar (reglas: misma comunidad · venta ≤ compra · partes distintas)"
echo "    VENTA : $VENDEDOR  · $COMUNIDAD · ${KWH_VENTA} kWh @ ${PRECIO_VENTA}/kWh"
echo "    COMPRA: $COMPRADOR · $COMUNIDAD · ${KWH_COMPRA} kWh @ ${PRECIO_COMPRA}/kWh"
if [ "$VENDEDOR" = "$COMPRADOR" ]; then err "Vendedor y comprador no pueden ser el mismo."; exit 1; fi
if [ "$PRECIO_VENTA" -gt "$PRECIO_COMPRA" ]; then err "precio venta > compra: no casa."; exit 1; fi
ok "Reglas OK → se casará min($KWH_VENTA,$KWH_COMPRA) kWh al precio promedio"

# =============================================================================
# PASO 3 · (1) CASAR → crea el contrato de negocio en liquidacion-p2p
# =============================================================================
paso 3 "(1) CASAR — POST /liquidacion-p2p/v1/api/v1/transacciones/emparejar"
TX_JSON="$(curl -s --max-time 40 -X POST \
  "$FRONT/api/gw/liquidacion-p2p/v1/api/v1/transacciones/emparejar" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"vendedorId\":\"$VENDEDOR\",\"kwhVenta\":$KWH_VENTA,\"precioVenta\":$PRECIO_VENTA,\"compradorId\":\"$COMPRADOR\",\"kwhCompra\":$KWH_COMPRA,\"precioCompra\":$PRECIO_COMPRA}" || true)"
TX_ID="$(printf '%s' "$TX_JSON" | jget id)"
if [ -z "$TX_ID" ]; then err "Falló el emparejamiento. Respuesta: $TX_JSON"; exit 1; fi
ENERGIA_KWH="$(printf '%s' "$TX_JSON" | jget energiaKwh)"
PRECIO_CAS="$(printf '%s' "$TX_JSON" | jget precioCasacionKwh)"
VALOR_TOTAL="$(printf '%s' "$TX_JSON" | jget valorTotal)"
TIPO_CAS="$(printf '%s' "$TX_JSON" | jget tipoCasacion)"
ok "Contrato casado: $TX_ID"
ok "  Energía casada: ${ENERGIA_KWH} kWh · precio casación: ${PRECIO_CAS} · total: ${VALOR_TOTAL} COP · casación: ${TIPO_CAS}"

# =============================================================================
# PASO 4 · (2) ANCLAR → envía el hash a Polygon Amoy (blockchain-gateway)
# =============================================================================
paso 4 "(2) ANCLAR — POST /api/v1/anclajes (directo al gateway en Render)"
ENERGIA_WH="$(printf '%s\n' "$ENERGIA_KWH" | awk '{printf "%.0f", $1*1000}')"
LIQUIDADA_EN="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
ANCLA_JSON="$(curl -s --max-time 90 -X POST "$GATEWAY/api/v1/anclajes" \
  -H 'Content-Type: application/json' \
  -d "{\"transaccionId\":\"$TX_ID\",\"vendedorId\":\"$VENDEDOR\",\"compradorId\":\"$COMPRADOR\",\"energiaWh\":$ENERGIA_WH,\"precioPorKwh\":\"$PRECIO_CAS\",\"valorTotal\":\"$VALOR_TOTAL\",\"liquidadaEn\":\"$LIQUIDADA_EN\",\"comunidadId\":\"$COMUNIDAD\"}" || true)"
ESTADO="$(printf '%s' "$ANCLA_JSON" | jget estado)"
HASH="$(printf '%s' "$ANCLA_JSON" | jget hashCasacion)"
ok "Anclaje solicitado · estado inicial: ${ESTADO:-?}"
ok "  Hash de casación (keccak256): $HASH"

# =============================================================================
# PASO 5 · Esperar CONFIRMACIÓN on-chain (polling, como el auto-refresco)
# =============================================================================
paso 5 "Esperar confirmación on-chain (polling cada 5 s, máx 120 s)"
TXHASH=""; BLOQUE=""; GAS=""; EXPLORER_URL=""
for i in $(seq 1 24); do
  A="$(curl -s --max-time 30 "$GATEWAY/api/v1/anclajes/$TX_ID" || true)"
  E="$(printf '%s' "$A" | jget estado)"
  if [ "$E" = "CONFIRMADO" ]; then
    TXHASH="$(printf '%s' "$A" | jget txHash)"
    BLOQUE="$(printf '%s' "$A" | jget bloque)"
    GAS="$(printf '%s' "$A" | jget gasUsado)"
    EXPLORER_URL="$(printf '%s' "$A" | jget explorerUrl)"
    ok "CONFIRMADO en el intento $i"
    ok "  txHash: $TXHASH"
    ok "  bloque: $BLOQUE · gas usado: $GAS"
    ok "  PolygonScan: ${EXPLORER_URL:-$EXPLORER/tx/$TXHASH}"
    break
  fi
  printf '  … intento %s/24 · estado=%s\r' "$i" "${E:-?}"
  sleep 5
done
echo
if [ -z "$TXHASH" ]; then
  warn "No confirmó en 120 s. Puede ser cold-start o modo demo. Reintenta GET /api/v1/anclajes/$TX_ID."
fi

# =============================================================================
# PASO 6 · VERIFICAR integridad on-chain (recomputa el hash y compara)
# =============================================================================
paso 6 "Verificar integridad — POST /api/v1/anclajes/verificar"
VER_JSON="$(curl -s --max-time 40 -X POST "$GATEWAY/api/v1/anclajes/verificar" \
  -H 'Content-Type: application/json' \
  -d "{\"transaccionId\":\"$TX_ID\",\"vendedorId\":\"$VENDEDOR\",\"compradorId\":\"$COMPRADOR\",\"energiaWh\":$ENERGIA_WH,\"precioPorKwh\":\"$PRECIO_CAS\",\"valorTotal\":\"$VALOR_TOTAL\",\"liquidadaEn\":\"$LIQUIDADA_EN\"}" || true)"
INTEGRO="$(printf '%s' "$VER_JSON" | jget integro)"
EXISTE="$(printf '%s' "$VER_JSON" | jget existe)"
if [ "$INTEGRO" = "True" ] || [ "$INTEGRO" = "true" ]; then
  ok "Integridad verificada ✓ (el hash recomputado coincide con el anclado on-chain)"
elif [ "$EXISTE" = "True" ] || [ "$EXISTE" = "true" ]; then
  warn "El anclaje existe pero el hash difiere (revisar datos)."
else
  warn "El gateway aún no conoce el anclaje (puede haber reiniciado — store en memoria)."
fi

# =============================================================================
# PASO 7 · Guardar evidencia (JSON) para la entrega
# =============================================================================
paso 7 "Guardar evidencia del experimento"
cat > "$REGISTRO" <<JSON
{
  "experimento": "marketplace-p2p-anclaje-onchain",
  "ejecutadoEn": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "red": { "red": "$RED_NOMBRE", "chainId": "$CHAINID", "contrato": "$CONTRATO", "explorer": "$EXPLORER" },
  "orden": {
    "vendedor": "$VENDEDOR", "comprador": "$COMPRADOR", "comunidad": "$COMUNIDAD",
    "kwhVenta": $KWH_VENTA, "precioVenta": $PRECIO_VENTA, "kwhCompra": $KWH_COMPRA, "precioCompra": $PRECIO_COMPRA
  },
  "contrato": {
    "transaccionId": "$TX_ID", "energiaKwh": "$ENERGIA_KWH", "precioCasacionKwh": "$PRECIO_CAS",
    "valorTotal": "$VALOR_TOTAL", "tipoCasacion": "$TIPO_CAS"
  },
  "anclaje": {
    "estado": "${TXHASH:+CONFIRMADO}", "hashCasacion": "$HASH", "txHash": "$TXHASH",
    "bloque": "$BLOQUE", "gasUsado": "$GAS", "explorerUrl": "${EXPLORER}/tx/$TXHASH"
  },
  "integridad": { "integro": "$INTEGRO", "existe": "$EXISTE" }
}
JSON
ok "Evidencia guardada en: $REGISTRO"

echo
echo "======================================================================="
printf '%s  EXPERIMENTO COMPLETADO%s\n' "$c_b$c_g" "$c_reset"
echo "  Contrato:  $TX_ID"
echo "  On-chain:  ${TXHASH:-<pendiente>}"
[ -n "$TXHASH" ] && echo "  Verifica:  $EXPLORER/tx/$TXHASH"
echo "  Evidencia: $REGISTRO"
echo "======================================================================="
