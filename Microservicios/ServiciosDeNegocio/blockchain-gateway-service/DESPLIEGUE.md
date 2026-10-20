# Despliegue del blockchain-gateway-service

El server actual (k8s `voltera-app`) está **saturado**. El gateway de blockchain es
el componente ideal para desplegarse **FUERA** del cluster, porque:

- No comparte estado con los demás microservicios (su OUTBOX es propio).
- Es de **bajo tráfico** y está **fuera del camino crítico** (gate 6 del árbol de decisión):
  una liquidación se ancla de forma asíncrona, nadie espera la cadena en línea.
- Solo necesita dos conexiones: salir a internet al **RPC del tercero** y hablar con
  **Redpanda/Kafka** del server.

## Opción recomendada — gateway EXTERNO al server

```
                 INTERNET  (RPC gratuito del tercero: Polygon Amoy)
                      ▲ JSON-RPC
        ┌─────────────┴───────────────┐
        │ blockchain-gateway (8101)    │  ← VM aparte / PaaS free / tu máquina
        └──────┬───────────────▲───────┘
   Kafka produce│               │ REST (vía WSO2 o directo)
    /consume    ▼               │
 ┌──────────────────────────────┴──────────────────────┐
 │ SERVER ACTUAL: Redpanda :9092 · WSO2 :8243 · micros  │
 └───────────────────────────────────────────────────────┘
```

### Comunicación en AMBOS sentidos (sin tocar los servicios actuales)
- **Server → Blockchain (entrada):** el gateway **consume** `topic.facturacion.liquidacion`
  de Redpanda (clase `LiquidacionCalculadaListener`). `liquidacion-p2p` sigue emitiendo
  su evento igual que hoy; no se modifica nada en él.
- **Blockchain → Server (salida):** al confirmar, el gateway **publica**
  `LiquidacionAnclada` en `topic.blockchain.anclada`, que cualquier micro del server puede
  consumir. Además expone REST `/api/v1/anclajes` (consulta vía WSO2).

### Requisito único para vivir fuera
El puerto Kafka de Redpanda (`9092`) debe ser **alcanzable** desde donde despliegues el
gateway. Dos formas:
1. **Exponer Redpanda** con un Service `type=LoadBalancer`/NodePort o un `advertise-kafka-addr`
   público/VPN. (Ver nota de `advertise-kafka-addr` en `k8s/voltera-eda.yaml`.)
2. Si Kafka NO puede exponerse, usar el gateway en **modo solo-REST** (`KAFKA_ENABLED=false`):
   el server llama por HTTP al gateway para anclar, y consulta el estado por HTTP. Se pierde
   el disparo automático por evento, pero la comunicación bidireccional REST queda intacta.

### Desplegarlo como contenedor fuera del cluster
```bash
# build local
cd Microservicios/ServiciosDeNegocio/blockchain-gateway-service
docker build -t voltera/blockchain-gateway:1.0.0 .

# run en la VM/host externo
docker run -d --name blockchain-gateway -p 8101:8101 \
  -e KAFKA_ENABLED=true \
  -e KAFKA_BOOTSTRAP=<host-redpanda-alcanzable>:9092 \
  -e VOLTERA_BLOCKCHAIN_RPC_URL=https://rpc-amoy.polygon.technology \
  -e VOLTERA_BLOCKCHAIN_PRIVATE_KEY=0x<clave_de_prueba> \
  -e VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS=0x<direccion_desplegada> \
  voltera/blockchain-gateway:1.0.0
```
Opciones de hosting gratuitas para el contenedor: Fly.io, Render, Railway (free tier),
o una VM pequeña. El coste de la cadena sigue siendo **cero** (testnet + RPC free).

### Conectar WSO2 al gateway externo
Publica la API apuntando el backend al host externo en lugar del Istio interno:
```bash
cd Microservicios/wso2-gateway
BACKEND=http://<host-gateway-externo>:8101 ./publish-blockchain-api.sh
```
Así el server se comunica con el blockchain **a través del API Gateway** (como pediste):
el SPA/BFF → WSO2 → gateway externo → Polygon.

## Opción alternativa — dentro del cluster (si el server se libera)
Usa `k8s/voltera-blockchain.yaml` (Deployment+Service+HTTPRoute, puerto 8101) tal cual.
Recursos pedidos deliberadamente bajos (`requests: 50m CPU / 200Mi`). Para no sumar carga,
puedes dejar `replicas: 1` y, si instalas KEDA, escalar a cero por lag como reaseguro.

## Resumen de la decisión
| Escenario | KAFKA_ENABLED | Comunicación server↔blockchain |
|---|---|---|
| **Externo + Kafka alcanzable** (recomendado) | `true` | Eventos (entrada y salida) + REST |
| **Externo sin exponer Kafka** | `false` | Solo REST (anclar/consultar vía WSO2) |
| **Dentro del cluster** | `true` | Eventos + REST (suma carga al server) |
