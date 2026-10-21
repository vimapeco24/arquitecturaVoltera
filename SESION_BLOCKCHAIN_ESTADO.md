# Estado de la sesión — Blockchain Voltera

> Punto de retome para la próxima sesión. Última actualización: 2026-10-20 (cierre 3 — TODO LIVE).
> Lee este archivo primero al reconectar.

## TL;DR — dónde vamos
**TODO FUNCIONANDO END-TO-END Y EN LA NUBE.**
- Código blockchain terminado, probado y en GitHub (`main`, https://github.com/vimapeco24/arquitecturaVoltera.git).
- **Frontend LIVE en Vercel:** https://voltera-frontend.vercel.app (marketplace P2P completo).
- **Gateway blockchain LIVE en Render:** https://voltera-blockchain-gateway.onrender.com
  (free tier: duerme tras ~15 min, 1er request ~50 s).
- **Contrato real en Polygon Amoy:** `0xd0Ef1686fcAF3f026134DE4188bd977c4EBcf530`.
- **Anclaje real CONFIRMADO on-chain** (txHash `0x4963bf…0182f5d`, status 0x1). Circuito verificado.
- Diagrama de arquitectura: `Modificados/Voltera_Arquitectura_Completa.drawio` (+ PNG).
- PENDIENTE opcional: conectar `liquidacion-p2p-service` del cluster con
  `VOLTERA_BLOCKCHAIN_GATEWAY_URL=https://voltera-blockchain-gateway.onrender.com` (si el server está activo).
- ⚠️ SEGURIDAD: rotar el token de Vercel (`vcp_...`) pegado en el chat; la wallet de prueba es desechable.

## Deploy Vercel — COMPLETADO (2026-10-20)
- Vercel CLI instalado **local** en `Microservicios/voltera-frontend` (`npm install --no-save vercel@latest`)
  porque el global requería sudo (EACCES). Binario: `node_modules/.bin/vercel` (v64.7.0).
- Deploy a producción OK: `VERCEL_TOKEN=<token> node_modules/.bin/vercel deploy --prod --yes`.
  - Producción: https://voltera-frontend-9se8bi455-vimapeco.vercel.app
  - Alias: https://voltera-frontend.vercel.app  → responde **HTTP 200** público.
  - Cuenta: `vimapeco24-2439`. Build en Vercel compiló las API functions gw/obs/token.
- Verificaciones de código re-ejecutadas y OK: frontend `ng build`, contrato Hardhat **8/8**,
  `blockchain-gateway-service` **6/6**, `liquidacion-p2p-service` **12/12**.
- ⚠️ El token de Vercel usado se pegó en el chat → **rotarlo/revocarlo** en Vercel → Settings → Tokens.

## ✅ Hecho y verificado (commit 2086dd7, ya en GitHub)
- **Contrato** `blockchain/contracts/LiquidacionP2PAnchor.sol` + Hardhat — **8 tests pasan**.
- **Microservicio** `blockchain-gateway-service` (puerto **8101**, Spring Boot hexagonal + web3j)
  — **6 tests pasan**; verificado en vivo (anclar, listar, red, verificar integridad).
  Endpoints REST: `POST /api/v1/anclajes`, `GET /{id}`, `GET /`, `GET /red`, `POST /verificar`.
- **Integración** `liquidacion-p2p-service` → gateway por REST (puerto de salida opcional,
  no-op si no se configura `VOLTERA_BLOCKCHAIN_GATEWAY_URL`) — **12 tests pasan**.
- **k8s**: `k8s/voltera-blockchain.yaml` + topic `topic.blockchain.anclada` en `voltera-topics.yaml`
  + gateway añadido a `build-push-all.sh`.
- **WSO2**: `wso2-gateway/publish-blockchain-api.sh` (API `/blockchain`).
- **PaaS**: `fly.toml`, `render.yaml`, `railway.json`, `.dockerignore` en el gateway.
- **Frontend Angular**: página **Marketplace P2P** (`pages/blockchain/`) con libro de órdenes,
  casar+anclar, registro de contratos on-chain, botón verificar integridad, barra de estado de
  red. Service + modelos + ruta `/blockchain` + menú. **Build OK**.
- **Docs**: `BLOCKCHAIN_VOLTERA.md` (análisis + árbol de decisión + implementación),
  `GUIA_DESPLIEGUE_BLOCKCHAIN.md` (paso a paso completo), `DESPLIEGUE.md`, `DESPLIEGUE_PAAS.md`,
  colección `Voltera_Postman_Blockchain.json`.

## ✅✅ CIRCUITO BLOCKCHAIN VERIFICADO END-TO-END (2026-10-20) — CERRADO
Tras 3 fixes de depuración en PaaS, un anclaje REAL quedó confirmado en la cadena:
- Fixes finales de gas en `Web3jCadenaAdapter`:
  - `resolverGasPrice()`: eth_gasPrice +20%, piso 30 gwei (Amoy exige >=25 gwei).
  - gasLimit acotado a **200k** (el default web3j 9M causaba "insufficient funds" con el saldo del faucet).
  - Commit `0dc09e7`.
- **Anclaje real LIQ-FINAL-001 → CONFIRMADO**:
  - txHash `0x4963bf3b9662327bb5d32a42d86802a6123ceecb4ae4ad1df93ce5cef0182f5d`
  - PolygonScan: https://amoy.polygonscan.com/tx/0x4963bf3b9662327bb5d32a42d86802a6123ceecb4ae4ad1df93ce5cef0182f5d
  - Receipt on-chain: status 0x1 (OK), bloque 0x305ef53, gasUsed 137928.
  - Verificación de integridad: `integro:true` (hash recomputado == hash anclado).
- Flujo probado: Front (Vercel) / REST → gateway (Render) → web3j → contrato (Polygon Amoy) → txHash real.
- Saldo wallet tras anclaje: ~0.077 POL menos ~0.004 → alcanza para ~15 anclajes más.

## 🗺️ DIAGRAMA DE ARQUITECTURA VIVO (2026-10-20)
- Creado/actualizado **`Modificados/Voltera_Arquitectura_Completa.drawio`** + PNG exportado
  (`Voltera_Arquitectura_Completa.png`, 4705×2464, vía `drawio` CLI de Homebrew).
- El diagrama rotula en NEGRITA la TECNOLOGÍA de cada componente y marca los NUEVOS (blockchain):
  Angular 18 · Vercel · WSO2 API Manager · Istio/mTLS · Spring Boot 3.3/Java 21 · Docker · K8s ·
  Redpanda/Kafka · KEDA · TSDB/Redis · Spring Boot+web3j (Render) · Solidity+Hardhat · Polygon Amoy ·
  Prometheus/Grafana/Kiali/Jaeger. Barra superior con el stack completo.
- Exportar PNG: `drawio --export --format png --scale 2 --border 20 --output <png> <drawio>`.
- **REGLA PERMANENTE** en `.kiro/steering/arquitectura-voltera.md`: cada vez que se agregue/
  modifique/elimine algo de arquitectura, hay que actualizar TAMBIÉN este .drawio en el mismo
  trabajo, y dejar constancia aquí. La regla se incluye siempre (todas las sesiones).

## 🟢 GATEWAY BLOCKCHAIN EN RENDER — LIVE (2026-10-20)
- **URL pública:** https://voltera-blockchain-gateway.onrender.com (free tier, "duerme" tras ~15 min).
- Desplegado por **Blueprint** (`render.yaml` en raíz con `rootDir`). Variables de cadena como env vars.
- `/actuator/health` = 200; `/api/v1/anclajes/red` = `disponible:true, configurada:true, contrato 0xd0Ef…EBcf530`.
- **Front conectado:** `environment.prod.blockchainBase = https://voltera-blockchain-gateway.onrender.com`;
  `blockchain.service.ts` llama DIRECTO a esa URL (no vía WSO2 LAN). CORS abierto en el gateway.
- **3 fixes aplicados al depurar el deploy en PaaS (todos en git):**
  1. `BeanConfiguration.normalizar()` — quita espacios/saltos de la private key.
  2. `BlockchainProperties` setters — limpian rpc/key/contrato (el CONTRACT_ADDRESS llegó con `\n`).
  3. `Web3jCadenaAdapter.resolverGasPrice()` — gas dinámico `eth_gasPrice x2`, piso 30 gwei
     (Amoy rechazaba 4.1 gwei con "gas price below minimum"). Commit `df9169a`.
- PENDIENTE verificar tras el deploy de `df9169a`: que un anclaje real pase a CONFIRMADO con txHash.

## ✨ MARKETPLACE P2P — mejoras Nivel 2 + re-deploy (2026-10-20)
Front `pages/blockchain/` ampliado para quedar "muy completo" y re-desplegado a Vercel:
- **Persistencia** del libro de órdenes y de los contratos en `localStorage`
  (`voltera.mp.ordenes.v1`, `voltera.mp.contratos.v1`) → sobreviven al recargar.
- **Auto-refresco on-chain** (polling 10 s, toggle en la barra de red): los anclajes
  PENDIENTE pasan a CONFIRMADO solos sin recargar.
- **Timeline de hitos** por contrato (casado → anclado → confirmado) con tooltips de fecha +
  marca de verificación de integridad (✓/✗).
- **Casación parcial**: si venta/compra no son iguales, reduce la orden restante en el libro
  (usa `pendienteVentaKwh/pendienteCompraKwh` de la TransaccionP2P).
- **Filtros** de la tabla (Todos / On-chain / Pendientes / Sin anclar), botón quitar orden,
  botón reiniciar, y 6 métricas (añadidas Pendientes y Precio medio /kWh).
- Verificado: `tsc --noEmit` OK, `ng build --configuration production` OK
  (blockchain-component 29.12 kB). Re-deploy Vercel OK → https://voltera-frontend.vercel.app
  (200 en `/` y `/blockchain`).
> Nota: mientras el gateway NO esté en una PaaS, la barra dirá "modo demo" y `listar/red/verificar`
> no alcanzarán la cadena. El marketplace funciona (libro, casar, persistencia) pero el anclaje
> real requiere desplegar el gateway (paso siguiente).

## 🟢 CONTRATO DESPLEGADO EN POLYGON AMOY (2026-10-20)
- **Wallet de prueba (DESECHABLE, nunca fondos reales):** `0xa0A18C1D64272f9a6a248Deb5106994F5D8AFDf2`
  - Clave privada quedó visible en el chat → wallet comprometida a propósito, solo PoC.
  - POL recibido del faucet oficial (faucet.polygon.technology): 0.1 POL. Tras deploy: ~0.078 POL.
- **Contrato `LiquidacionP2PAnchor` DESPLEGADO:**
  - Dirección: **`0xd0Ef1686fcAF3f026134DE4188bd977c4EBcf530`**
  - Explorer: https://amoy.polygonscan.com/address/0xd0Ef1686fcAF3f026134DE4188bd977c4EBcf530
  - Verificado on-chain con `eth_getCode` → tiene bytecode real. `deployments/amoy.json` guardado.
- `blockchain/.env` creado con RPC `https://polygon-amoy.drpc.org` + clave + dirección.
  Confirmado que git lo IGNORA (`git check-ignore` OK) → la clave no se sube al repo.

### Pasos que SIGUEN (ahora que el contrato está en Amoy)
1. Desplegar el gateway en PaaS (Fly/Render/Railway) con 3 secrets:
   `VOLTERA_BLOCKCHAIN_RPC_URL=https://polygon-amoy.drpc.org`
   `VOLTERA_BLOCKCHAIN_PRIVATE_KEY=0x...` (la del .env)
   `VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS=0xd0Ef1686fcAF3f026134DE4188bd977c4EBcf530`
2. Publicar API en WSO2: `BACKEND=<url-gateway> ./publish-blockchain-api.sh`.
3. Conectar `liquidacion-p2p-service`: `VOLTERA_BLOCKCHAIN_GATEWAY_URL=<url-gateway>`.

## ✅ Pipeline validado END-TO-END contra una cadena real (2026-10-20)
Antes de pedir tus credenciales de Amoy, se validó TODO el flujo de despliegue en local
(nodo Hardhat + gateway real), para garantizar que con tu wallet funcionará igual:
- Nodo Hardhat local (`npx hardhat node`, chainId 31337) + `deploy.js` → contrato desplegado
  OK en `0x5FbDB2315678afecb367f032d93F642f64180aa3` (guardado en `deployments/localhost.json`).
- `blockchain-gateway-service` arrancado real apuntando a esa cadena
  (`VOLTERA_BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545`, KAFKA_ENABLED=false). Arrancó en :8101,
  `/actuator/health`=200, `GET /api/v1/anclajes/red` → `disponible:true, configurada:true`.
  (El único error en logs era Zipkin/tracing hacia `zipkin.istio-system` — inofensivo fuera del cluster.)
- **Anclaje real**: `POST /api/v1/anclajes` → estado CONFIRMADO + `txHash` real minado.
- **Integridad (gate 4)**: `POST /verificar` mismos datos → `integro:true`; datos alterados → `integro:false`.
- **Idempotencia**: re-anclar misma transacción no duplica (lista sigue en 1, mismo txHash).
- Nombres EXACTOS del DTO de anclaje (para Postman/front): `transaccionId, vendedorId, compradorId,
  energiaWh (long>0), valorTotal, precioPorKwh, liquidadaEn (Instant ISO-8601)`.
- Procesos de prueba cerrados al terminar (puertos 8545/8101 liberados).
> Conclusión: el código de despliegue está probado contra una cadena de verdad. Lo único que
> falta es repetir `deploy:amoy` con TU wallet de prueba (no se puede automatizar: la clave privada
> la debes generar/controlar tú en MetaMask).

## ⏳ A medias (retomar aquí)
- **(RESUELTO 2026-10-20)** Deploy frontend a Vercel: HECHO. Ver "Deploy Vercel — COMPLETADO" arriba.
  Ya no queda nada a medias en el frontend.

## ❌ Falta (requiere credenciales del usuario — ver GUIA_DESPLIEGUE_BLOCKCHAIN.md)
1. Crear wallet de prueba (MetaMask) + pedir POL gratis en faucet de Amoy.
2. Desplegar el contrato: `cd blockchain && cp .env.example .env` (poner RPC+clave) →
   `npm run deploy:amoy` → copiar la dirección del contrato.
3. Desplegar el gateway en Fly/Render/Railway (free tier) con los 3 secrets de cadena.
4. Publicar la API en WSO2: `BACKEND=<url-gateway> ./publish-blockchain-api.sh`.
5. Conectar el server: `kubectl set env deploy/liquidacion-p2p-service VOLTERA_BLOCKCHAIN_GATEWAY_URL=<url>`.
6. Deploy frontend a Vercel.

## Notas técnicas importantes
- **git bloqueado por licencia Xcode**: usar el git de Command Line Tools:
  `export PATH="/Library/Developer/CommandLineTools/usr/bin:$PATH"` antes de cualquier `git`.
  (El `/usr/bin/git` da el error de licencia; el de CLT funciona.)
- **Puertos**: el gateway usa **8101** (8081–8100 ocupados; cobros=8100, habilitacion=8095, etc.).
- **Red del server**: LAN privada (192.168.3.x); Kafka NO alcanzable desde la nube → el gateway
  en PaaS corre en **modo solo-REST** (`KAFKA_ENABLED=false`); comunicación por API/WSO2.
- **Decisión de arquitectura**: caso = liquidación P2P; red = consorcio PoA en prod, pero para
  la PoC se usa Polygon Amoy (pública) anclando SOLO hashes (gate 8), así no se exponen datos.

## Verificación rápida al volver
```bash
export PATH="/Library/Developer/CommandLineTools/usr/bin:$PATH"
cd /Users/vimapeco/Downloads/ArquitectutaNuevaGenegeracion
git log --oneline -1          # debe mostrar 2086dd7 (o posterior)
git status --short            # limpio salvo material ignorado
```
