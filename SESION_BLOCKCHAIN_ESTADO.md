# Estado de la sesión — Blockchain Voltera

> Punto de retome para la próxima sesión. Última actualización: 2026-10-22 (experimento reproducible + orquestación EDA cerrada en liquidacion-p2p).
> Lee este archivo primero al reconectar.

## 🧪 EXPERIMENTO REPRODUCIBLE + ORQUESTACIÓN EDA CERRADA (2026-10-22)

Objetivo: dejar la página /blockchain como un experimento profesional y reproducible
para la entrega, orquestando bien los servicios existentes.

**Qué se implementó (todo compilado y probado, BUILD SUCCESS, 13 tests verdes):**

1. **Script `Microservicios/experimento-p2p.sh`** — ejecuta el flujo completo end-to-end
   (token OAuth2 → casar → anclar → confirmar → verificar) contra Vercel+Render, imprime
   evidencia por paso y guarda un JSON en `evidencia-experimento/`. Verificado en vivo:
   contrato `e1671590…`, txHash `0xd6330049…`, bloque 50862256, gas 119988, integridad ✓.
   (Usa el python3 de CommandLineTools automáticamente si el del sistema está bloqueado por Xcode.)

2. **Orquestación por eventos (EDA) en `liquidacion-p2p`** — se cerró el eslabón que faltaba:
   - Nuevo evento de dominio `LiquidacionCalculada` (domain/model).
   - Puerto de salida `LiquidacionEventPublisherPort` (domain/port/out).
   - Adaptador `KafkaLiquidacionEventPublisher` + `NoopLiquidacionEventPublisher` (infra/messaging).
   - `MessagingConfiguration`: crea KafkaTemplate+publisher solo si `voltera.kafka.enabled=true`;
     no-op por defecto (Render sigue solo-REST; el broker es LAN).
   - `LiquidacionService` publica el evento al casar (emparejar y actualizar).
   - `EmparejarRequest`/`ComandoEmparejar` ganan `comunidadId` opcional (retrocompatible).
   - Al casar, `liquidacion-p2p` publica `topic.facturacion.liquidacion`; el gateway ya lo
     consumía (LiquidacionCalculadaListener) → anclaje automático. Idempotente por transaccionId.
   - pom: + `spring-kafka`. application.yml: `voltera.kafka.enabled`, topic y bootstrap.
   - Test nuevo `OrquestacionEdaTest` (verifica publicación, comunidad y Wh).

3. **Manifiesto k8s** `voltera-services.yaml`: `KAFKA_ENABLED=true` + `KAFKA_BOOTSTRAP` para
   el `liquidacion-p2p` del clúster (activa EDA donde sí hay broker Redpanda).

4. **Guía de entrega** `GUIA_EXPERIMENTO_P2P.md` (raíz): qué demuestra, cómo ejecutarlo
   (script y web), arquitectura, evidencia real, limitaciones honestas (estado in-memory).

5. **Diagramas** (regla permanente): `Voltera_Flujo_Experimento_P2P.{drawio,png}` (secuencia del
   experimento) y actualizado `Voltera_Arquitectura_Completa.{drawio,png}` (liquidacion-p2p ahora
   "casación + publica EDA"; arista anclaje = REST o evento topic.facturacion.liquidacion).

**PENDIENTE DE DESPLIEGUE (requiere Docker, apagado en esta sesión):** reconstruir la imagen
`liquidacion-p2p-service` y aplicar el manifiesto para activar el EDA en el clúster:
`cd Microservicios/k8s && ./build-push-all.sh && kubectl -n voltera-app apply -f voltera-services.yaml && kubectl -n voltera-app rollout restart deploy/liquidacion-p2p-service`.
Mientras tanto el experimento funciona end-to-end por REST (Vercel+Render), que es lo que se demuestra.

## 🗺️ DIAGRAMA ACTUALIZADO + VALIDACIÓN EN VIVO (2026-10-22)

Se validó todo el stack contra el estado real (no bitácoras) y se actualizó el diagrama vivo
`Modificados/Voltera_Arquitectura_Completa.drawio` (+ PNG regenerado con `drawio --export`, XML
verificado bien formado).

Qué se cambió en el `.drawio` y por qué:
- **Nuevo panel "ESTADO VERIFICADO EN VIVO · 2026-10-22"** (banda verde al pie) con el health de los
  5 frentes, para que el diagrama muestre de un vistazo qué está arriba.
- **SPA (Vercel)** y **gateway (Render)**: etiqueta `✅ LIVE`.
- **Contrato LiquidacionP2PAnchor**: marcado `✅ DESPLEGADO` (0xd0Ef…EBcf530, Amoy chainId 80002).
- **Contrato ComunidadEnergeticaRegistry**: borde punteado + `⚠ escrito+probado (9 tests), NO
  desplegado` (hoy comunidad = metadato off-chain). Refleja el único pendiente real.
- Pie con fecha 2026-10-22.

Evidencia de la validación en vivo (22-oct-2026):
- Git: `main == origin/main`, árbol limpio (HEAD `3655a1b`); nada pendiente de subir.
- Front (Vercel): sirve `main-JNXGBHCI.js` (idéntico al build local); `/` y `/blockchain` → 200.
- Gateway (Render): `/actuator/health` UP; `/api/v1/anclajes/comunidades` → 200 (endpoint del commit 58acd2f).
- WSO2 `atiesia.synology.me:3000` → 200.
- Clúster k8s `voltera-app` (SSH 192.168.3.11): 20/20 pods Running; 8 ScaledObjects READY; HPA sin
  targets unknown. Imágenes en vivo ≥ tags del repo → nada que empujar al clúster.
- Conclusión: lo "nuevo" (comunidades/marketplace) vive en Render + Vercel, NO en el clúster, y ya
  estaba desplegado. No había cambios pendientes de subir.

## 🏘️ COMUNIDADES ENERGÉTICAS + MEJORAS FRONT/CONTRATO (2026-10-21)
Trabajo nuevo pedido: "implementa todo lo que puedas de blockchain para las comunidades
energéticas y lo del contrato". Todo compila y pasa tests; el deploy a Amoy del contrato
nuevo queda pendiente (necesita tu wallet/gas).

### Contrato (carpeta `blockchain/`)
- **NUEVO `contracts/ComunidadEnergeticaRegistry.sol`**: padrón on-chain de comunidades
  energéticas y su membresía. `AccessControl` (rol `OPERADOR_ROLE`, Embedded Permission) +
  `Pausable`. Funciones: `registrarComunidad`, `agregarMiembro`, `removerMiembro`,
  `esMiembro`, `puedenCasar(comunidad, vendedor, comprador)` (regla de dominio: solo casan
  miembros activos de la MISMA comunidad), eventos para EDA, errores de no repudio.
- **Tests**: `test/ComunidadEnergeticaRegistry.test.js` (9) + anchor (8) = **17 passing**.
- `scripts/deploy.js` ahora despliega AMBOS contratos y guarda las 2 direcciones en
  `deployments/<red>.json` (`registroComunidades.address`). Verificado en red hardhat in-process.
- ⏳ PENDIENTE (requiere tu wallet con POL): `npm run deploy:amoy` para publicar el registro
  en Amoy y poner `VOLTERA_BLOCKCHAIN_REGISTRO_COMUNIDADES=<dir>` en el gateway.

### Gateway (`blockchain-gateway-service`) — retrocompatible, SIN romper lo Live
- `comunidadId` como **metadato OFF-CHAIN** en el anclaje (NO entra en el hash → no rompe
  idempotencia/verificación del contrato ya desplegado). DTO `AnclarRequest` y `AnclajeResponse`
  lo exponen; `LiquidacionP2P`/`Anclaje` lo portan; el listener Kafka lo extrae del evento.
- El `AnclajeResponse` ahora devuelve también **`bloque`** y **`gasUsado`** del recibo on-chain
  (capturados en `Web3jCadenaAdapter`; de paso se arregló un bug que llamaba
  `waitForTransactionReceipt` dos veces).
- **NUEVO endpoint** `GET /api/v1/anclajes/comunidades` → resumen agregado por comunidad
  (total, confirmados, pendientes, energíaWh total).
- Tests gateway: **8 passing** (antes 6; +2 de comunidad y de "comunidad no altera el hash").

### Frontend (`voltera-frontend`, página `/blockchain`)
- Campo **Comunidad energética** en el formulario de ofertas (con datalist de comunidades
  conocidas) y la comunidad mostrada en cada orden del libro.
- **Regla de casación**: solo casan órdenes de la MISMA comunidad; mensaje de motivo si no.
- **Panel de comunidades** (tarjetas con métricas, clic = filtrar contratos por comunidad).
- **Detalle de contrato** (modal): hash completo + copiar, txHash + copiar, bloque, gasUsado,
  intentos, último error, timeline de hitos, enlaces a PolygonScan y verificar.
- **Re-sincronizar** (re-hidrata desde el gateway tras un reinicio del store en memoria).
- **Exportar evidencia** a CSV y JSON (hash, txHash, bloque, gas, comunidad).
- Verificado: `tsc --noEmit` OK, `ng build --configuration production` OK
  (blockchain-component 44.79 kB).
- ⏳ Para que estos cambios lleguen a producción: `git push` (Render auto-deploya el gateway) y
  re-deploy del front a Vercel (ver sección Deploy Vercel).

### ✅ VALIDADO EN PRODUCCIÓN (2026-10-21, commit 58acd2f pusheado)
- Render auto-desplegó el gateway. `/actuator/health` 200; nuevo `/api/v1/anclajes/comunidades` 200.
- Anclaje real CON comunidad → CONFIRMADO, y la respuesta ahora incluye:
  `comunidadId:"COMUNIDAD-SOLAR-01"`, **`bloque:50778774`**, **`gasUsado:119976`**,
  txHash `0xf2a39159…b1b49fd`.
- `/comunidades` → agrega COMUNIDAD-SOLAR-01: 1 contrato, 1 confirmado, 11000 Wh.
- Tests: contrato Hardhat **17/17** (8 anchor + 9 comunidad), gateway **8/8**, liquidacion-p2p **12/12**.

### ⚠️ HALLAZGO IMPORTANTE sobre git (RESUELTO 2026-10-21)
- El `.gitignore` tenía `/Blockchain/` (para los PDFs del curso). En macOS (FS case-insensitive)
  ESA regla también ignoraba la carpeta **`blockchain/`** del proyecto Hardhat → el contrato
  Solidity, sus tests y `deploy.js` **NO estaban en git**.
- **RESUELTO**: se ajustó `.gitignore` para ignorar SOLO los PDFs (`*.pdf`) y los artefactos de
  build (`blockchain/node_modules|artifacts|cache|deployments`), y se versionó el código. También
  se añadieron negaciones para versionar el diagrama `Modificados/Voltera_Arquitectura_Completa.
  drawio` (+ `.png`) y las plantillas `**/.env.example` (el `.env` real sigue ignorado).
- Commits `eab68ab` (proyecto Hardhat + diagrama) y `c624af0` (.env.example). Ahora en GitHub:
  `blockchain/contracts/{LiquidacionP2PAnchor,ComunidadEnergeticaRegistry}.sol`, `test/` (17),
  `scripts/deploy.js`, `hardhat.config.js`, `package.json`, `README.md`, `.env.example`.
- Verificado post-commit: `npx hardhat test` → **17 passing**. Sin PDFs, node_modules ni .env reales.

### ⏳ Pendientes que requieren TUS credenciales
1. **Front a Vercel**: re-deploy con tu `VERCEL_TOKEN` (rotar el anterior). CLI local ya instalado:
   `cd Microservicios/voltera-frontend && VERCEL_TOKEN=<token> node_modules/.bin/vercel deploy --prod --yes`.
2. **Deploy del registro de comunidades a Amoy**: `cd blockchain && npm run deploy:amoy` (con tu
   wallet de prueba con POL) → poner `VOLTERA_BLOCKCHAIN_REGISTRO_COMUNIDADES=<dir>` en el gateway.

## 🔒 HARDENING CORS DEL GATEWAY (2026-10-21) — commit 0cc9da9 (local, sin push)
- `WebCorsConfiguration` ya NO usa el comodín `allowedOriginPatterns("*")`.
  Ahora lee `voltera.cors.allowed-origins` (env `VOLTERA_CORS_ORIGINS`), default:
  `https://voltera-frontend.vercel.app, https://*.vercel.app, http://localhost:4200`.
  Métodos reducidos a GET/POST/OPTIONS; headers a Content-Type/Accept/Authorization;
  `allowCredentials=false`; `maxAge=3600`.
- `application.yml`: añadido bloque `voltera.cors.allowed-origins`.
- Verificado: `./mvnw test` → **6/6 OK** con el cambio.
- Verificado que el origen real de producción (`voltera-frontend.vercel.app`) matchea los
  patrones permitidos → el front NO se romperá al redeplegar.
- ✅ **PUSHEADO y DESPLEGADO** (2026-10-21): `git push origin main` OK (7b8d8fb..24615a1).
  Render auto-desplegó el commit `24615a1` (trigger Auto-Deploy, 2m29s) y quedó **Live**.
- ✅ **CORS endurecido VERIFICADO en producción** (preflight live sobre Render):
  - Origen permitido (`voltera-frontend.vercel.app`) → `allow-methods: GET,POST,OPTIONS`,
    `max-age: 3600`, `allow-origin` correcto.
  - Origen NO permitido (`malicioso.example.com`) → NO devuelve ninguna cabecera `access-control-*`.
  - (Las verificaciones previas que mostraban el CORS viejo fueron falsos negativos por el
    cold-start del free tier: la instancia dormida tarda ~50-80s en despertar; health dio HTTP 000
    durante ese arranque. Tras despertar, el CORS nuevo responde correctamente.)
- Diagrama `.drawio`: SIN cambios estructurales (CORS es política del mismo gateway ya dibujado,
  no un nuevo componente/puerto/topic).

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

## ✅✅ VALIDACIÓN COMPLETA END-TO-END (2026-10-21, tras hardening CORS) — TODO OK
Re-validado el circuito entero después del redeploy con el CORS endurecido:
- **Git**: `main` en `1e1e39f`, árbol limpio, todo pusheado (origin == local).
- **Gateway Render**: `/actuator/health` 200; `/api/v1/anclajes/red` → `configurada:true,
  disponible:true, contrato 0xd0Ef…EBcf530, polygon-amoy, chainId 80002`.
- **Front Vercel**: `/` 200 y `/blockchain` 200.
- **Anclaje REAL nuevo** `VALIDA-1792597781` → estado **CONFIRMADO**, txHash
  `0x377cf87bc9287c4ad560eef7cb13e0e6f6633ded67d0b0476a40cb03f5470782`.
  - Receipt on-chain (RPC Amoy): **status 0x1**, bloque 0x306c4b8, gasUsed 0xb0ac,
    to = contrato 0xd0ef…cf530. CONFIRMADO de verdad en la cadena.
- **Integridad**: `/api/v1/anclajes/verificar` con los mismos datos → `integro:true`
  (hashRecomputado == hashAnclado); con datos alterados (energiaWh) → `integro:false`. ✔
  (Ruta correcta del endpoint: `/api/v1/anclajes/verificar`, no `/api/v1/verificar`.)
- **Tests locales**: contrato Hardhat **8/8**, blockchain-gateway **6/6**,
  liquidacion-p2p **12/12**. BUILD SUCCESS en todos.
> Nota: la lista del gateway arrancó en `[]` tras el redeploy porque el store es en memoria;
> los anclajes on-chain previos siguen en la cadena, solo se pierde el estado local al reiniciar.

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
