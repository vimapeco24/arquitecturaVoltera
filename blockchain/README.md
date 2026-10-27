# Voltera · Blockchain (Polygon Amoy — 100% gratuito)

Contrato que ancla en Polygon la **prueba de integridad (hash)** de cada liquidación
del mercado energético **P2P** de Voltera. Diseño alineado con el árbol de decisión
documentado en `../BLOCKCHAIN_VOLTERA.md` (secciones 5 y 6).

- **Red:** Polygon **Amoy** testnet (chainId `80002`, gas token `POL`) — gratuita.
- **RPC:** a través de un **tercero** (proveedor gestionado gratuito), sin nodo propio.
- **On-chain solo el hash** (gate 8): no se exponen datos sensibles aunque la red sea pública.
- **Confirmación por evento** (gate 6): el gateway indexa `LiquidacionAnclada` y lo republica en Kafka.

## Requisitos
- Node.js 18+ (probado con v24).
- Una wallet de **prueba** (MetaMask) con algo de POL de testnet (del faucet, gratis).

## Instalación
```bash
cd blockchain
npm install
cp .env.example .env   # y edita AMOY_RPC_URL + PRIVATE_KEY
```

## Conseguir el RPC gratuito (el "tercero")
Elige una opción en `.env` (`AMOY_RPC_URL`):
- **Público sin registro:** `https://rpc-amoy.polygon.technology` o `https://polygon-amoy.drpc.org`.
- **Free tier más estable:** crea una app gratis en Alchemy / Infura / QuickNode y pega tu endpoint.

## Conseguir gas de prueba (gratis)
Pide POL de Amoy en un faucet (necesitas la dirección de tu wallet de prueba):
- https://faucet.polygon.technology (selecciona **Amoy**, token **POL**)
- https://www.alchemy.com/faucets/polygon-amoy
- https://faucet.quicknode.com/polygon

## Comandos
```bash
npm run build          # compila el contrato
npm test               # corre los tests (red Hardhat en memoria, sin gastar nada)

# Opción local (sin internet): nodo Hardhat en otra terminal
npm run node
npm run deploy:local

# Opción testnet gratuita (Polygon Amoy via tercero)
npm run deploy:amoy    # despliega y guarda la dirección en deployments/amoy.json
npm run verify:amoy -- <ADDRESS> <ADMIN_ADDRESS>   # opcional, verifica en PolygonScan
```

Tras `deploy:amoy`, copia la dirección impresa en la config del gateway
(`VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS`).

## Contrato `LiquidacionP2PAnchor`
| Función | Rol | Qué hace |
|---|---|---|
| `anclar(txId, hash, energiaWh, liquidadaEn)` | `ORACULO_ROLE` | Ancla una liquidación. Idempotente (mismo hash = no-op; hash distinto = revierte). |
| `verificar(txId, hash)` | público (view) | ¿El hash recomputado coincide con el anclado? (auditoría del regulador). |
| `obtener(txId)` / `estaAnclada(txId)` | público (view) | Lee el anclaje. |
| `autorizarOraculo` / `revocarOraculo` | `DEFAULT_ADMIN_ROLE` | Gestiona qué gateways pueden anclar. |
| `pausar` / `reanudar` | `DEFAULT_ADMIN_ROLE` | Parada de emergencia. |

Evento emitido: `LiquidacionAnclada(txId indexed, hashCasacion, energiaWh, liquidadaEn, ancladaEn, oraculo indexed)`.

## Contrato `CesionCertificadoMultisig` (patrón Multiple Authorization)
Cesión de certificados de origen renovable que exige **dos firmas distintas** antes de
registrar (umbral M-de-N con M=2). Familia **seguridad**. Ver `../PATRONES_BLOCKCHAIN_VOLTERA.md`.

| Función | Rol | Qué hace |
|---|---|---|
| `proponerCesion(cesionId, certificadoId, cedenteId, cesionarioId, hashDetalle)` | `FIRMANTE_ROLE` | 1ª firma: deja la cesión PENDIENTE (1/2). |
| `confirmarCesion(cesionId)` | `FIRMANTE_ROLE` (otra cuenta) | 2ª firma: al llegar a 2, pasa a REGISTRADA. |
| `estaRegistrada(cesionId)` / `obtenerCesion(cesionId)` | público (view) | Lee el estado de la cesión. |
| `autorizarFirmante` / `revocarFirmante` | `DEFAULT_ADMIN_ROLE` | Gestiona el padrón de firmantes. |
| `pausar` / `reanudar` | `DEFAULT_ADMIN_ROLE` | Parada de emergencia. |

Eventos: `CesionPropuesta`, `CesionFirmada`, `CesionRegistrada`.
⚠️ Requiere ≥2 firmantes autorizados; con uno solo nada se registra (es el costo del patrón).

## Contrato `CertificadoOrigenNFT` (patrón Tokenización · ERC-721 NO fungible)
Certificado de origen renovable como **token NO fungible** (cada certificado es único),
usando las implementaciones de **OpenZeppelin**: `ERC721` + `ERC721URIStorage` (metadata
off-chain) + `ERC721Burnable` + `AccessControl`. Familia **datos**. Se eligió ERC-721
(no fungible) y **no** ERC-20 (fungible) ni ERC-1155 (semi-fungible) porque cada certificado
es único e irrepetible. Ver `../PATRONES_BLOCKCHAIN_VOLTERA.md`.

| Función | Rol | Qué hace |
|---|---|---|
| `emitirCertificado(propietario, uri, hashDetalle, energiaMWh, tecnologia)` | `EMISOR_ROLE` | Acuña un NFT único y lo asigna. Devuelve el `tokenId`. |
| `tokenURI(tokenId)` / `certificados(tokenId)` | público (view) | Metadata off-chain + datos mínimos on-chain. |
| `transferFrom` / `safeTransferFrom` | propietario | Transferencia estándar ERC-721. |
| `burn(tokenId)` | propietario | Retira/cancela el certificado (ERC721Burnable). |

Eventos: `CertificadoEmitido` + los estándar de ERC-721 (`Transfer`, `Approval`).

## Seguridad
- `AccessControl` + `Pausable` de OpenZeppelin (patrón *Embedded Permission* / Zero Trust on-chain).
- Sin transferencias de valor → sin superficie de reentrada.
- La `PRIVATE_KEY` del `.env` es **solo de testnet**; nunca uses una con fondos reales.
