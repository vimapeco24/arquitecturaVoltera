# Desplegar el blockchain-gateway en una PaaS gratuita (Fly.io / Render / Railway)

El server (cluster `voltera-app`) está saturado y en **LAN privada** (192.168.3.x):
solo WSO2 está expuesto, y Redpanda hace `advertise-kafka-addr` al DNS **interno**
`redpanda.voltera-app:9092`. Conclusión: **Kafka NO es alcanzable desde la nube**, así
que en una PaaS el gateway corre en **modo solo-REST** (`KAFKA_ENABLED=false`). La
comunicación con los microservicios actuales se hace **por API (REST)**, en los dos sentidos.

Puerto interno del gateway: **8101** (los 8081–8100 ya están ocupados por otros servicios).

## Arquitectura de conexión (ambos lados)

```
   INTERNET: RPC gratuito del tercero (Polygon Amoy)
        ▲ JSON-RPC
 ┌──────┴───────────────────────────────┐
 │ blockchain-gateway (PaaS, HTTPS)      │  URL pública: https://<app>.fly.dev
 │ KAFKA_ENABLED=false  ·  solo-REST     │
 └───▲───────────────────────────┬───────┘
     │ (1) POST /api/v1/anclajes  │ (2) GET /api/v1/anclajes/{id}
     │                            ▼
 ┌───┴────────────────────────────────────────────────┐
 │ SERVER (LAN): liquidacion-p2p ──▶ llama al gateway   │
 │               WSO2 ──▶ publica BlockchainAPI →gateway│
 │               Angular SPA ──▶ WSO2 ──▶ gateway        │
 └──────────────────────────────────────────────────────┘
```

- **(1) Server → Blockchain (anclar):** cuando `liquidacion-p2p` liquida una transacción,
  llama por REST (best-effort, asíncrono) al gateway en la nube. Esto se activa poniendo
  en `liquidacion-p2p-service` la variable:
  ```
  VOLTERA_BLOCKCHAIN_GATEWAY_URL=https://<app>.fly.dev
  ```
  Si se deja vacía, el servicio se comporta igual que hoy (no-op). **No cambia nada más.**

- **(2) Blockchain → Server (consultar / exponer):** el estado on-chain se consulta por REST.
  WSO2 publica la API apuntando a la URL pública del gateway, de modo que el SPA y cualquier
  micro acceden **a través del API Gateway** (como pediste).

> La vía por eventos Kafka (`LiquidacionCalculadaListener` / `topic.blockchain.anclada`) sigue
> existiendo y se usa **solo si el gateway corre DENTRO del cluster**. En la nube se usa REST.

## Paso a paso — Fly.io (recomendado)
```bash
cd Microservicios/ServiciosDeNegocio/blockchain-gateway-service

# 1) instalar flyctl y loguear (una vez): https://fly.io/docs/hands-on/install-flyctl/
fly auth login

# 2) crear la app (usa el fly.toml incluido; no despliega aún)
fly launch --no-deploy --copy-config --name voltera-blockchain-gateway

# 3) secrets (clave de PRUEBA testnet; nunca una real)
fly secrets set \
  VOLTERA_BLOCKCHAIN_RPC_URL="https://rpc-amoy.polygon.technology" \
  VOLTERA_BLOCKCHAIN_PRIVATE_KEY="0x<clave_de_prueba>" \
  VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS="0x<direccion_desplegada>"

# 4) desplegar
fly deploy

# 5) probar
curl https://voltera-blockchain-gateway.fly.dev/actuator/health
```

## Paso a paso — Render.com
1. Sube el repo a GitHub. En Render: New → Blueprint → selecciona el repo (usa `render.yaml`).
2. Define en el dashboard los 3 valores `sync:false`: `VOLTERA_BLOCKCHAIN_RPC_URL`,
   `VOLTERA_BLOCKCHAIN_PRIVATE_KEY`, `VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS`.
3. Deploy. URL: `https://voltera-blockchain-gateway.onrender.com`.

## Paso a paso — Railway
1. New Project → Deploy from GitHub (usa `railway.json` + Dockerfile).
2. Variables: `KAFKA_ENABLED=false`, `VOLTERA_BLOCKCHAIN_CHAIN_ID=80002`,
   `VOLTERA_BLOCKCHAIN_NETWORK=polygon-amoy`, y los 3 secrets de cadena.
3. Genera un dominio público en Settings → Networking.

## Conectar el server con el gateway en la nube

### A) Que liquidacion-p2p ancle automáticamente al liquidar
En el Deployment de `liquidacion-p2p` (k8s) añade la env var con la URL pública:
```yaml
env:
  - { name: VOLTERA_BLOCKCHAIN_GATEWAY_URL, value: "https://voltera-blockchain-gateway.fly.dev" }
```
o, si prefieres pasar por WSO2 (todo el tráfico saliente por el gateway de APIs):
```yaml
  - { name: VOLTERA_BLOCKCHAIN_GATEWAY_URL, value: "https://192.168.3.13:8243/blockchain/v1" }
```

### B) Publicar la BlockchainAPI en WSO2 apuntando a la nube
```bash
cd Microservicios/wso2-gateway
BACKEND=https://voltera-blockchain-gateway.fly.dev ./publish-blockchain-api.sh
```
Así el SPA (vía `/gw/blockchain/v1/...`) y Postman consumen el gateway por WSO2.

## Checklist de verificación
- [ ] `GET https://<app>.fly.dev/actuator/health` → `{"status":"UP"}`
- [ ] `POST https://<app>.fly.dev/api/v1/anclajes` con un cuerpo de prueba → 202 + `txHash`
- [ ] En PolygonScan Amoy aparece la tx (`https://amoy.polygonscan.com/tx/<txHash>`)
- [ ] Liquidar en `liquidacion-p2p` con `VOLTERA_BLOCKCHAIN_GATEWAY_URL` puesto → el log del
      gateway muestra "Liquidacion registrada en OUTBOX" para esa transacción
- [ ] `GET /gw/blockchain/v1/api/v1/anclajes` desde el SPA lista los anclajes
