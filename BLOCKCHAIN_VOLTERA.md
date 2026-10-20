# BLOCKCHAIN — Voltera (ARTI4208 · Módulo 4)

> Archivo de contexto persistente para implementar blockchain en el proyecto Voltera.
> Creado tras leer los 51 PDFs de la carpeta `Blockchain/` y revisar la arquitectura actual.
> **Estado: IMPLEMENTADO. Contrato + proyecto Hardhat + microservicio de borde (gratuito en Polygon Amoy). Ver sección 7.**

---

## 0. Cómo se leyeron los PDFs (nota técnica para sesiones futuras)

- Los 51 archivos de `Blockchain/` son PDFs del curso (capítulos de `arquibook.virtual.uniandes.edu.co`).
- `pdftotext`/Homebrew/`/usr/bin/python3` estaban **bloqueados por la licencia de Xcode**
  (`xcode-select -p` apunta a Xcode.app). No se pudo ejecutar `sudo xcodebuild -license accept`
  (pide contraseña interactiva).
- **Solución que SÍ funcionó:** el Python de las Command Line Tools no exige esa licencia:
  ```bash
  /Library/Developer/CommandLineTools/usr/bin/python3 -m pip install --user pypdf
  /Library/Developer/CommandLineTools/usr/bin/python3 extract_all.py
  ```
- El texto extraído quedó en `_blockchain_txt/*.txt` (un `.txt` por cada PDF). El script es
  `extract_all.py` en la raíz. Muchos PDFs son versiones casi idénticas (sufijos 1–7).

---

## 1. Síntesis del material del curso (lo que enseñan los PDFs)

### Fundamentos
- **Blockchain = tecnología fundacional** (analogía TCP/IP), no una app. Adopción por olas
  (marco **Iansiti & Lakhani**: uso único → localización → sustitución → transformación;
  ejes = novedad × coordinación).
- **Tres generaciones:** 1.0 criptomonedas · 2.0 smart contracts · 3.0 DApps/DAO.
- **Elementos estructuradores (6):** criptografía, transacciones, descentralización P2P,
  consenso, DAO, DApps.
- **Tres pilares criptográficos:** llaves asimétricas (identidad/no repudio), hashing SHA-256
  (integridad), prueba de trabajo (inmutabilidad). Mito: "blockchain está cifrado" → FALSO;
  garantiza **integridad + autenticidad**, no confidencialidad por defecto.
- **Anatomía del bloque:** data + nonce + hash anterior + hash propio → encadenamiento = inmutabilidad.
- **Tipos de red × permiso:**
  | | Público | Consorcio | Privado |
  |---|---|---|---|
  | Consenso | todos los mineros | mineros seleccionados | 1 organización |
  | Inmutabilidad | casi imposible alterar | modificable | modificable |
  | Costo/eficiencia | baja (costoso) | alta | alta |
  | Ejemplos | Bitcoin, Ethereum | R3 Corda, EWF | Bankchain |
- **Consenso:** familias lotería (PoW, PoS) vs votación (DPoS, **PoA**). Problema de fondo =
  Generales Bizantinos (Lamport 1982). **Regla práctica:** partes que se conocen pero no se
  confían del todo → **consorcio + consenso por votación (PoA / DPoS)**. PoW solo para públicas.

### Árbol de decisión — ¿blockchain o base de datos? (9 compuertas, obligatorio antes de diseñar)
- **Etapa 1 (¿hace falta cadena?):**
  - **Cortan (un "no" termina en BD):** `1` ¿múltiples partes? · `4` ¿inmutabilidad y no repudio? · `7` ¿admin descentralizable?
  - **Inclinan (un "no" pesa en contra):** `2` ¿admin confiable/rotable? · `3` ¿operación centralizada = punto de falla? · `5` ¿transparencia relevante?
- **Etapa 2 (¿qué cadena y cómo?):**
  - `6` **Desempeño** → si exige ms, **sacar la cadena del camino crítico**; confirmar por evento.
  - `8` **Off-chain storage** → en la cadena solo el hash; el documento vive fuera.
  - `9` **¿Compartir cifrado?** → define el tipo de red (compartida cifrada vs privada/permissioned).
- Regla de oro: "una propuesta que no pasa el árbol no se arregla después" → volver al punto 1.

### Arquitectura
- **Arquitectura de referencia — 5 capas** (abajo→arriba): red · datos · consenso · incentivos · aplicación.
  Elegir tipo de cadena = **elegir perfil de atributos de calidad** (lenguaje ARTI-4109).
  El gran salto de calidad está en el eje **público/privado**, no en permissionless/permissioned.
- **Patrones (4 familias):**
  - Interacción externa: **Oracle**, **Reverse Oracle**.
  - Datos: **off-chain storage**, **tokenización**, **cifrado**, **state channel**.
  - Seguridad: **Multiple Authorization**, **X-confirmation**, **Embedded Permission** (= Zero Trust on-chain).
  - Control estructural: **contract registry**, **data contract**, **factory** (respuesta a la inmutabilidad; proxy para evolucionar).
- **Arquitectura DApp — 3 zonas:**
  - **off-chain**: frontend, backend tradicional (cotizar, datos personales, KYC, notificaciones).
  - **borde**: nodo **RPC**, **oráculo** (hacia la cadena), **indexador** (desde la cadena), a veces **relayer** (paga gas).
  - **on-chain**: contratos + estado compartido.
  - Lecturas vía indexador = **CQRS + event sourcing**. Billetera = **Zero Trust**. RPC = **API gateway**.
  - **The Graph** (indexador): subgraph = manifiesto (qué contrato/red/bloque) + schema GraphQL + mappings (AssemblyScript).
  - La DApp real es **híbrida**: backend tradicional + cadena (solo la verdad compartida) + servicios de borde.
- **Redes de oráculos:** el "problema del oráculo" = punto único de falla / confianza / fuente.
  Chainlink (DON, OCR, token LINK). Tolerancia bizantina **n ≥ 3f+1**, publica **mediana**.
  Servicios: **Data Feeds** (umbral de desviación + heartbeat; revisar antigüedad), **Functions**
  (llamar API), **Automation** (despertar al contrato), **VRF** (aleatoriedad), **CCIP** (cross-chain),
  **Proof of Reserve**. Alternativas: Pyth (pull), API3 (first-party), UMA (optimista), RedStone, Chronicle.

### Plataformas
- **Bitcoin** (L1, UTXO, PoW). **Ethereum** (L1, EVM, PoS). **EVM** (máquina virtual, gas).
- **Solidity:** contrato = objeto en la cadena con estado permanente e inmutable. Flujo: `.sol` →
  `solc` (bytecode + ABI) → test (Remix/Hardhat/Foundry) → deploy → invocar.
  - Ejemplo del curso: `SeguroRetrasoVuelo` (seguro paramétrico, oráculo única cuenta autorizada).
  - `msg.sender` = identidad; `payable`/`msg.value` = dinero; `emit` eventos = integración con EDA;
    `immutable`/`constant` = reglas fijas.
  - **Ataque de reentrada** (The DAO 2016) → defensa **checks-effects-interactions** + `nonReentrant`
    de **OpenZeppelin**. Requisitos: pruebas, análisis estático (Slither), auditorías.
- **Capa 2 / Polygon:** trilema (descentralización/seguridad/escalabilidad). Rollups:
  **optimista** (ventana de fraude ~7 días; Arbitrum, OP, Base) vs **ZK** (prueba de validez; zkSync, Starknet).
  **Polygon PoS** = sidechain EVM, Bor (producción, deriva de Geth) + Heimdall (Cosmos SDK/CometBFT),
  checkpoints en Ethereum. Token POL (antes MATIC). Costo fracción de centavo.

---

## 2. CLAVE: esto es Voltera, no Solventa

Todo el material usa **Solventa (seguros paramétricos)** como ejemplo. **Nuestro caso es
Voltera** — plataforma de **energía distribuida**: medición **AMI**, **mercado P2P**,
**comunidades energéticas**, **certificados de origen**. Hay que *traducir* cada concepto a Voltera.

### Arquitectura actual de Voltera (ya implementada — ver `Microservicios/ARQUITECTURA_ACTUAL.md`)
- Angular SPA → BFF serverless (Vercel) → **WSO2 API Gateway** → **Istio Ingress** →
  microservicios **Spring Boot 3.3 / Java 21 hexagonales** sobre **Kubernetes** (`voltera-app`).
- **EDA**: **Redpanda/Kafka** + **KEDA** (autoescala por lag) + patrón **OUTBOX** transaccional.
- Service mesh Istio + mTLS. Observabilidad: Prometheus, Grafana, Kiali, Jaeger.
- Microservicios de negocio: `habilitacion, integracion-ami, ingesta, telemetria-core,
  telemetria, tarifas, tarifa-eventos, liquidacion-mensual, liquidacion-p2p, facturacion,
  pagos, cobros, notificaciones, siniestros, reaseguro, volumetria`.
- Eventos de dominio ya existentes: `MedidorHabilitado, LecturaValidada,
  ConsumoIntervaloRegistrado, TarifaAsignada, LiquidacionCalculada, FacturaEmitida,
  PagoConfirmado, SiniestroAprobado`, etc.

### Candidatos de Voltera que pasarían el árbol de decisión
| Caso Voltera | Por qué encaja (gates 1/4/7) | Forma sugerida (gates 6/8/9) |
|---|---|---|
| **Certificados de origen / garantías renovables** | múltiples partes (prosumidor, comercializador, regulador, comprador), no repudio, transparencia | **tokenización** + off-chain storage (hash on-chain) |
| **Liquidación del mercado P2P** | prosumidores que no se confían, registro compartido inmutable de excedentes casados | smart contract de liquidación; medición vía **oráculo**; fuera del camino crítico (gate 6) → confirmar por evento Kafka |
| **Trazabilidad de medición AMI** | anclar hash de lecturas para auditoría del regulador | **off-chain storage** (solo hash) |

- Red probable: **consorcio permissioned (PoA)** para el libro entre operadores +
  posible **capa 2 EVM (Polygon)** para lo abierto al público. Mismo razonamiento que el material
  aplica a Solventa (consorcio para el libro de reaseguro; L2 para el producto masivo).
- **Encaje con lo existente:** el **borde** (oráculo/indexador) se conecta a la **EDA Kafka/Redpanda**
  vía OUTBOX: un servicio escucha eventos del contrato (`emit`) y los publica como evento de dominio.
  El **RPC** convive con WSO2/Istio (RPC ≈ API gateway on-chain). Lecturas on-chain = CQRS (ya se usa).

---

## 3. Mapa de los PDFs leídos (por tema)

**Fundamentos:** `por-que-blockchain` · `elementos-blockchain` · `tipos-y-consenso` ·
`arbol-de-decision` · `blockchain-solventa`.
**Arquitectura:** `arquitectura-referencia` · `patrones-blockchain` · `arquitectura-dapp` ·
`redes-de-oraculos` · `arquitectura-solventa` · `caso-reaseguro-blockchain` · `casos-de-negocio`.
**Plataformas:** `blockchain-plataformas` · `bitcoin-capa-1` · `ethereum-capa-1` ·
`maquina-virtual-ethereum` · `programacion-solidity` · `capa-2-y-polygon`.
> Texto completo extraído en `_blockchain_txt/`. Sin leer aún en detalle (versiones duplicadas):
> `arquitectura-solventa`, `caso-reaseguro-blockchain`, `casos-de-negocio`, `bitcoin-capa-1`,
> `ethereum-capa-1`, `maquina-virtual-ethereum`, `blockchain-solventa` — revisar mañana si hace falta
> para el entregable concreto.

---

## 4. PENDIENTE — decidir mañana con el usuario (antes de implementar)

"Implementar blockchain" puede ser diseño o código. Falta aclarar:

1. **¿Entregable del curso (diseño/modelado, p.ej. Entrega 4: árbol de decisión + arquitectura DApp
   + diagramas)** o **código real** (contrato Solidity + servicio de borde desplegable en el cluster)?
2. **¿Qué caso de Voltera?** certificados de origen · liquidación P2P · trazabilidad AMI · otro.
3. Si es código: **¿plataforma objetivo?** Hardhat local (demo) · testnet Polygon Amoy ·
   consorcio PoA (Besu/Quorum).

### Primer paso recomendado (lo marca el propio material)
Responder el **árbol de decisión de 9 compuertas aplicado al caso elegido de Voltera** antes de
diseñar o programar nada. Ese es el "punto 3 del taller" y alimenta: gate 8 → frontera on-chain/off-chain
en ArchiMate, gate 6 → qué viaja por evento en el mesh, gate 9 → tipo de red.

---

_Siguiente sesión: leer esta sección 4, pedir al usuario los 3 puntos y arrancar por el árbol de decisión._

---

## 5. Árbol de decisión aplicado — Caso: **Liquidación del mercado P2P de Voltera**

> Caso elegido: **liquidación del mercado energético P2P** (casación de excedentes entre
> prosumidores y su registro inmutable). Es el candidato más fuerte de la tabla de la sección 2.
> Recorrido compuerta por compuerta con la numeración del cuaderno (Entrega 4).
> Fuente del árbol: `_blockchain_txt/..._arbol-de-decision.pdf.txt`.

### Etapa 1 — ¿Hace falta una cadena?

| # | Compuerta | Tipo | Respuesta | Justificación en Voltera |
|---|---|---|---|---|
| **1** | ¿Sirve a múltiples partes? | **Corta** | **SÍ** | Prosumidores que venden excedentes, consumidores que compran, comercializador/operador de la comunidad energética y el regulador que audita. No es un único usuario. |
| **2** | ¿Administración confiable/rotable? | Inclina | **SÍ** | La operación de la comunidad energética puede rotarse entre operadores; ninguna parte quiere que un solo comercializador controle el libro de casaciones. |
| **3** | ¿Operación centralizada = punto de falla? | Inclina | **SÍ** | Hoy la liquidación depende del operador central; si manipula o cae, se pierde confianza en los pagos entre vecinos. Las partes prefieren un registro descentralizado. |
| **4** | ¿Datos inmutables y no repudio? | **Corta** | **SÍ** | Una vez casado un excedente (quién vendió, a quién, cuánta energía, a qué precio, en qué intervalo) nadie debe poder reescribirlo; es la base de la facturación y de la auditoría regulatoria. |
| **5** | ¿Transparencia relevante? | Inclina | **SÍ** | Los prosumidores quieren ver que la casación fue justa; el regulador exige trazabilidad del mercado local. |
| **7** | ¿Administración descentralizable? | **Corta** | **SÍ** | Ninguna parte decide sola sobre el libro compartido de casaciones; el consenso del consorcio valida las reglas de casación. |

**Resultado Etapa 1:** pasa las tres compuertas que cortan (1, 4, 7) con **SÍ** y además las tres
que inclinan (2, 3, 5) con **SÍ**. **La cadena se justifica** — no hay ningún «no» que corte ni que
pese en contra. Avanza a la etapa 2.

### Etapa 2 — ¿Qué cadena y con qué forma?

| # | Compuerta | Respuesta | Decisión de diseño para Voltera |
|---|---|---|---|
| **6** | ¿Desempeño relevante? | **SÍ → sacar la cadena del camino crítico** | La medición AMI y la casación operan a alta frecuencia; nadie puede esperar la confirmación on-chain en línea. La liquidación responde de inmediato en los microservicios y **la confirmación vuelve por evento Kafka/Redpanda** (encaja con la EDA + patrón OUTBOX ya existentes). |
| **8** | ¿Almacenamiento off-chain? | **SÍ → patrón off-chain storage** | En la cadena solo va el **hash** del lote de casaciones / lecturas AMI y los campos mínimos para liquidar (contrapartes, kWh, precio, intervalo). Las lecturas crudas y datos personales (KYC) viven off-chain en los microservicios. |
| **9** | ¿Compartir información cifrada? | **Privacidad crítica → red privada/permissioned** | Hay datos sensibles de consumo por hogar → **red de consorcio permissioned con consenso por votación (PoA)** entre operadores. Opcionalmente una **capa 2 EVM (Polygon)** para lo que sí se abre al público (p. ej. certificados de origen). |

### Conclusión del árbol
El caso **es viable para blockchain** (punto 3 del taller). La forma de la solución queda definida así:

- **Tipo de red:** consorcio **permissioned, consenso PoA** (partes que se conocen pero no se
  confían del todo → regla práctica del material). Besu/Quorum como candidatos.
- **Frontera on-chain/off-chain (gate 8):** on-chain solo hash + mínimos de liquidación; el resto
  off-chain en los microservicios Spring Boot.
- **Integración por evento (gate 6):** la cadena sale del camino crítico; confirmación por evento de
  dominio sobre Kafka/Redpanda vía OUTBOX. Un servicio de **borde** escucha los `emit` del contrato
  (indexador) y publica un evento de dominio (p. ej. `LiquidacionP2PAnclada`).
- **Esto alimenta el resto de la Entrega 4:** gate 8 → frontera on-chain/off-chain en ArchiMate ·
  gate 6 → qué viaja por evento en el service/event mesh · gate 9 → tipo de red modelada.

### Siguientes pasos sugeridos
1. Dibujar la **arquitectura DApp de 3 zonas** (off-chain / borde / on-chain) para la liquidación P2P,
   mapeada sobre la arquitectura actual de Voltera (WSO2 + Istio + microservicios + Kafka).
2. Definir el **contrato de liquidación** (interfaz: registrar casación, anclar hash de lote,
   emitir evento) y el **servicio de borde** (oráculo de medición + indexador hacia Kafka).
3. Confirmar con el usuario si el entregable es solo diseño/diagramas o también código Solidity.

---

## 6. Conexión a la cadena vía un TERCERO (proveedor RPC gratuito)

> Decisión del usuario: conectar la aplicación a un blockchain **a través de un tercero**
> (proveedor RPC gestionado, sin montar nodo propio) y que sea **gratuito**.
> Esto corresponde a la pieza **RPC** de la **zona de borde** de la arquitectura DApp.

### Por qué un tercero
La aplicación nunca habla directo con la cadena: habla con un **nodo RPC**. Montar el nodo propio
(en Polygon: Bor + Heimdall) es costoso. Un **proveedor RPC gestionado** (el "tercero") expone un
endpoint HTTPS JSON-RPC; el backend/servicio de borde le pega a esa URL y el proveedor reenvía las
llamadas a la cadena. Sustituye al nodo propio → **cero costo de infraestructura de nodo**.

### Cómo se logra que sea gratis
1. **Testnet, no mainnet** → el gas se paga con tokens de prueba sin valor real, obtenidos gratis de
   un *faucet*. Para Polygon la testnet actual es **Amoy** (chain ID **80002**, gas token **POL**,
   reemplazó a Mumbai, anclada a Ethereum Sepolia).
2. **Proveedor RPC con tier gratuito** como tercero.

### Opciones de tercero RPC gratuito (verificadas 2026-10)
| Tipo | Proveedor / endpoint | Nota |
|---|---|---|
| Público, sin registro | `https://polygon-amoy.drpc.org` · `https://rpc-amoy.polygon.technology` | Gratis pero con rate limits; ok para demo. |
| Gestionado free tier (recomendado) | **Alchemy**, **Infura**, **QuickNode**, **dRPC**, **OnFinality** | API key + endpoint más estable; free tier basta para el curso. |

- **Faucet de gas de prueba (gratis):** Alchemy (0.5 POL/día con cuenta, 0.2 sin cuenta) · QuickNode (drip base sin cuenta).
- Fuentes: docs.polygon.technology (rpc-endpoints, matic-faucet), faucet.quicknode.com/polygon.

### Encaje en la arquitectura de Voltera
```
[Angular SPA / BFF] ─▶ [WSO2 API GW] ─▶ microservicios Spring Boot (off-chain)
                                            │
                              servicio de borde "blockchain-gateway"
                                            │  JSON-RPC sobre HTTPS
                                            ▼
                         ┌──────────────────────────────────┐
                         │  TERCERO = proveedor RPC gratuito  │  Alchemy / Infura / dRPC
                         │  endpoint Polygon Amoy (testnet)   │
                         └──────────────────────────────────┘
                                            │
                                            ▼
                         Cadena Polygon Amoy · contrato liquidación P2P
```
- El `blockchain-gateway` hace de **oráculo** (empuja hash de casaciones a la cadena) e **indexador**
  (escucha `emit` del contrato y publica evento de dominio en Kafka/Redpanda vía **OUTBOX**).
- Cumple **gate 6**: cadena fuera del camino crítico, confirmación por evento.
- Cumple **gate 8**: solo el **hash** va on-chain → no se exponen datos sensibles aunque la red sea pública.

### Tensión a documentar en el entregable (gate 9)
- La sección 5 concluyó **red permissioned (consorcio PoA)** por privacidad.
- Un RPC público gratuito apunta a **red pública (Polygon Amoy)**.
- **Resolución:** para la **PoC / entregable del curso** se usa Polygon Amoy vía RPC de un tercero
  anclando **solo hashes**; en **producción** se migraría a consorcio permissioned. Defendible y
  coherente con el material (L2 pública para lo masivo, consorcio para el libro privado).

---

## 7. IMPLEMENTACIÓN COMPLETA — todo gratuito en Polygon Amoy

Se implementó la solución completa del caso **liquidación P2P**, 100% gratuita, sobre
**Polygon Amoy** (testnet, RPC de un **tercero**). Dos entregables, ambos compilan y pasan tests.

### 7.1 Contrato Solidity — carpeta `blockchain/`
- `contracts/LiquidacionP2PAnchor.sol` — ancla el **hash** (gate 8) de cada liquidación;
  `AccessControl` (rol `ORACULO_ROLE`, patrón Embedded Permission) + `Pausable`; idempotente
  (mismo hash = no-op, hash distinto = revert → no repudio, gate 4); `emit LiquidacionAnclada`.
- Proyecto **Hardhat** (`hardhat.config.js`, red `amoy` chainId 80002) + `scripts/deploy.js`
  (guarda la dirección en `deployments/amoy.json`) + `.env.example` (RPC del tercero, faucets).
- Tests: **8 pasan** (`npx hardhat test`). Compila con solc 0.8.24.
- Gratis: testnet Amoy + RPC público/free-tier + gas del faucet (0 coste real).

### 7.2 Microservicio de borde — `Microservicios/ServiciosDeNegocio/blockchain-gateway-service/`
- Spring Boot 3.3 / Java 21, **hexagonal** (idéntico al resto). Puerto **8093**. **web3j 4.12.1**.
- **Oráculo**: `Web3jCadenaAdapter` llama `anclar(...)` vía el RPC del tercero.
- **OUTBOX + relay** (`OutboxRelay @Scheduled`): anclaje `PENDIENTE` → reintenta → `CONFIRMADO`
  (gate 6: cadena fuera del camino crítico).
- **Confirmación por evento**: publica `LiquidacionAnclada` en `topic.blockchain.anclada`.
- **Disparo automático**: `LiquidacionCalculadaListener` consume el evento de `liquidacion-p2p`.
- **3 modos** (demo sin nada / cadena real / cadena+Kafka) por variables `VOLTERA_BLOCKCHAIN_*`
  y `KAFKA_ENABLED`. Tests: **5 pasan** (`./mvnw test`). Verificado arrancando en modo demo:
  `POST /api/v1/anclajes` computa keccak256 y guarda `PENDIENTE` correctamente.

### 7.3 Cómo desplegarlo gratis (resumen)
1. `cd blockchain && npm install && cp .env.example .env` → pon RPC del tercero + clave de prueba.
2. Pide POL gratis en un faucet de Amoy (faucet.polygon.technology).
3. `npm run deploy:amoy` → copia la dirección del contrato.
4. Exporta `VOLTERA_BLOCKCHAIN_RPC_URL / _PRIVATE_KEY / _CONTRACT_ADDRESS` y arranca el gateway
   con `./mvnw spring-boot:run`. Anclajes reales con `txHash` verificable en PolygonScan.

### 7.4 Despliegue recomendado (server saturado) — gateway EXTERNO
Dado que el cluster `voltera-app` está saturado, **se recomienda desplegar el
blockchain-gateway FUERA del server** (VM aparte / PaaS free / contenedor local), porque no
comparte estado, es de bajo tráfico y está fuera del camino crítico. Detalle completo en
`.../blockchain-gateway-service/DESPLIEGUE.md`. Comunicación **bidireccional** sin tocar los
servicios actuales:
- **Server → Blockchain:** el gateway consume `topic.facturacion.liquidacion` (listener ya hecho).
- **Blockchain → Server:** publica `LiquidacionAnclada` en `topic.blockchain.anclada` + REST vía WSO2.
- Requisito: Redpanda `:9092` alcanzable desde el gateway; si no, modo solo-REST (`KAFKA_ENABLED=false`).
- Alternativa dentro del cluster: `k8s/voltera-blockchain.yaml` (si el server se libera).

### 7.5 Integración completada en TODAS las capas
- **k8s:** `k8s/voltera-blockchain.yaml` (Deployment+Service+HTTPRoute, puerto **8101**, Secret
  `voltera-blockchain-secret`) + topic `topic.blockchain.anclada` en `k8s/voltera-topics.yaml`.
- **WSO2:** `wso2-gateway/publish-blockchain-api.sh` (API `BlockchainAPI`, ctx `/blockchain`,
  `BACKEND=` configurable para apuntar a la PaaS).
- **Server ↔ Blockchain (ambos lados):** `liquidacion-p2p-service` tiene un puerto de salida
  opcional `AnclajeBlockchainPort` + adaptador REST (`RestAnclajeBlockchainAdapter`, best-effort
  asíncrono) que ancla cada liquidación llamando al gateway; es **no-op** si
  `VOLTERA_BLOCKCHAIN_GATEWAY_URL` está vacío (no cambia el comportamiento actual). 12 tests OK.
- **Despliegue PaaS gratuito:** `blockchain-gateway-service/` incluye `Dockerfile`, `fly.toml`,
  `render.yaml`, `railway.json`, `.dockerignore` y `DESPLIEGUE_PAAS.md`. En la nube corre en
  **modo solo-REST** (`KAFKA_ENABLED=false`) porque el Kafka del server es LAN privada; la
  comunicación server↔blockchain es por REST vía WSO2. Endpoint `/api/v1/anclajes/red` expone
  estado de red/contrato.
- **Frontend Angular — MARKETPLACE P2P completo** (`pages/blockchain/`): libro de órdenes
  (ventas/compras), casar venta+compra → crea contrato en `liquidacion-p2p` y lo ancla en
  blockchain, registro de contratos con estado on-chain (hash, txHash, enlace a PolygonScan),
  métricas (contratos, on-chain ✓, kWh, volumen COP) y barra de estado de red. Service con
  `anclar/obtener/listar/red`, modelos `Anclaje/AnclarRequest/RedInfo`, ruta `/blockchain` y
  entrada de menú "Marketplace P2P". **Build OK** (chunk 19.5 kB).

> Puerto del gateway: **8101** (8081–8100 ya ocupados por otros servicios).
