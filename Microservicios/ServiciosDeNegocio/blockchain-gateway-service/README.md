# blockchain-gateway-service

Servicio de **borde** de Voltera (puerto `8101`). Ancla en **Polygon Amoy** (testnet
gratuita, vía RPC de un **tercero**) la prueba de integridad de cada liquidación del
mercado **P2P**, e indexa la confirmación de vuelta a **Kafka/Redpanda**.

Arquitectura hexagonal idéntica al resto de microservicios. Contrato en `../../../blockchain/`.

## Qué hace (alineado con el árbol de decisión, ver `../../../BLOCKCHAIN_VOLTERA.md`)
- **Oráculo**: computa `keccak256` del registro canónico de la liquidación y lo ancla
  llamando a `LiquidacionP2PAnchor.anclar(...)` (gate 8: solo el hash on-chain).
- **OUTBOX**: persiste el anclaje `PENDIENTE`; un relay (`@Scheduled`) reintenta hasta
  confirmar (gate 6: la cadena queda fuera del camino crítico).
- **Indexador / confirmación por evento**: al confirmar, publica `LiquidacionAnclada`
  en `topic.blockchain.anclada`.
- **Disparo automático**: `LiquidacionCalculadaListener` escucha el evento de
  `liquidacion-p2p` y ancla sin intervención (además del REST manual).

## Modos de ejecución
| Modo | Config | Resultado |
|---|---|---|
| **Demo** (por defecto) | sin variables | Arranca sin RPC ni broker; los anclajes quedan `PENDIENTE` (ideal para probar la API). |
| **Cadena real gratuita** | `VOLTERA_BLOCKCHAIN_*` | Ancla de verdad en Polygon Amoy vía el RPC del tercero. |
| **Cadena + EDA** | + `KAFKA_ENABLED=true` | Además consume `LiquidacionCalculada` y publica `LiquidacionAnclada`. |

## Arrancar en local (demo, sin nada que configurar)
```bash
./mvnw spring-boot:run
# POST de prueba:
curl -X POST http://localhost:8101/api/v1/anclajes -H 'Content-Type: application/json' -d '{
  "transaccionId":"TX-DEMO-1","vendedorId":"prosumidor-A","compradorId":"prosumidor-B",
  "energiaWh":12500,"precioPorKwh":"0.45","valorTotal":"5.625","liquidadaEn":"2026-10-20T10:00:00Z"
}'
curl http://localhost:8101/api/v1/anclajes/TX-DEMO-1
```

## Conectar a Polygon Amoy (gratis, vía tercero)
1. Despliega el contrato: `cd ../../../blockchain && npm run deploy:amoy` → copia la dirección.
2. Exporta variables y arranca:
```bash
export VOLTERA_BLOCKCHAIN_RPC_URL=https://rpc-amoy.polygon.technology   # o tu endpoint Alchemy/Infura free
export VOLTERA_BLOCKCHAIN_PRIVATE_KEY=0x<clave_de_prueba_con_POL_del_faucet>
export VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS=0x<direccion_desplegada>
./mvnw spring-boot:run
```
La respuesta del anclaje incluye `txHash` y `explorerUrl` (https://amoy.polygonscan.com/tx/...).

## Activar la integración EDA (Kafka/Redpanda)
```bash
export KAFKA_ENABLED=true
export KAFKA_BOOTSTRAP=redpanda.voltera-app:9092   # o localhost:9092 en compose
```

## API
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/anclajes` | Registra y ancla una liquidación (202 Accepted). |
| `GET` | `/api/v1/anclajes/{transaccionId}` | Estado del anclaje. |
| `GET` | `/api/v1/anclajes` | Lista todos. |
| `GET` | `/actuator/health` | Salud. |

A través de WSO2: `/blockchain/v1/api/v1/anclajes`.

## Variables de entorno
| Variable | Default | Uso |
|---|---|---|
| `VOLTERA_BLOCKCHAIN_RPC_URL` | (vacío) | Endpoint RPC del tercero. |
| `VOLTERA_BLOCKCHAIN_PRIVATE_KEY` | (vacío) | Clave de la cuenta oráculo (solo testnet). |
| `VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS` | (vacío) | Dirección del contrato. |
| `VOLTERA_BLOCKCHAIN_CHAIN_ID` | `80002` | chainId (Amoy). |
| `KAFKA_ENABLED` | `false` | Activa consumer+producer. |
| `KAFKA_BOOTSTRAP` | `redpanda.voltera-app:9092` | Broker. |
| `EUREKA_ENABLED` | `false` | Registro en Eureka. |
