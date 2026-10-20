# Guía completa de despliegue — Blockchain Voltera (Polygon Amoy, gratis)

Esta guía cubre TODO lo que falta por hacer y que **requiere tus credenciales**
(registrarte en servicios, crear wallet, desplegar). El código ya está listo y
probado; aquí solo quedan los pasos manuales, explicados desde cero.

> Resumen del orden: (1) wallet de prueba → (2) gas gratis del faucet →
> (3) desplegar el contrato en Amoy → (4) desplegar el gateway en una PaaS →
> (5) publicar la API en WSO2 → (6) conectar los microservicios → (7) verificar.

---

## 0. Requisitos previos (una sola vez)
- **Node.js 18+** (ya tienes v24) y **npm**.
- **Navegador con MetaMask** (extensión) — https://metamask.io/download
- Cuenta de **GitHub** (ya la tienes: el repo está en GitHub).
- Nada de esto cuesta dinero: todo es testnet + free tier.

---

## 1. Crear una wallet de PRUEBA (MetaMask)

> ⚠️ Usa una wallet NUEVA, exclusiva para pruebas. NUNCA uses una con fondos reales.
> La clave privada va en un `.env`/secret de testnet.

1. Instala MetaMask y crea una wallet nueva (guarda la frase semilla en un lugar seguro).
2. Copia tu **dirección pública** (empieza por `0x...`, 42 caracteres).
3. Exporta la **clave privada** de esa cuenta:
   MetaMask → menú (⋮) → *Detalles de la cuenta* → *Mostrar clave privada* →
   introduce tu contraseña → copia el valor `0x...` (64 hex).
   - Esta es la `PRIVATE_KEY` / `VOLTERA_BLOCKCHAIN_PRIVATE_KEY`.

### Añadir la red Amoy a MetaMask (opcional, para ver el saldo)
- Red: **Polygon Amoy** · RPC `https://rpc-amoy.polygon.technology` ·
  chainId `80002` · símbolo `POL` · explorer `https://amoy.polygonscan.com`
- (O entra a https://amoy.polygonscan.com y usa "Add to MetaMask".)

---

## 2. Conseguir gas de prueba GRATIS (POL de Amoy)

Necesitas un poco de POL (sin valor real) para pagar el gas del despliegue.
Ve a un faucet, pega tu dirección pública y pide tokens:

- https://faucet.polygon.technology  → elige **Amoy** y token **POL**.
- https://www.alchemy.com/faucets/polygon-amoy  (0.5 POL/día con cuenta Alchemy).
- https://faucet.quicknode.com/polygon  → red **Amoy**.

Espera 1–2 min y confirma en https://amoy.polygonscan.com/address/<TU_DIRECCION>
que tienes saldo. Con 0.1–0.5 POL sobra para desplegar.

---

## 3. Desplegar el contrato en Polygon Amoy

```bash
cd blockchain
npm install                      # ya hecho si corriste los tests
cp .env.example .env
```
Edita `.env` con tus valores:
```
AMOY_RPC_URL=https://rpc-amoy.polygon.technology
PRIVATE_KEY=0x<tu_clave_privada_de_prueba>
POLYGONSCAN_API_KEY=            # opcional (ver paso 3.1)
```
Despliega:
```bash
npm run deploy:amoy
```
Al terminar imprime algo como:
```
✓ LiquidacionP2PAnchor desplegado en: 0xABC123...
  Explorer: https://amoy.polygonscan.com/address/0xABC123...
  Direccion guardada en: deployments/amoy.json
```
**Copia esa dirección** `0xABC123...` — es `VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS`.

### 3.1 (Opcional) Verificar el contrato en PolygonScan
Para ver el código fuente en el explorer:
1. Regístrate gratis en https://polygonscan.com/register y crea una API key
   en https://polygonscan.com/myapikey → ponla en `.env` (`POLYGONSCAN_API_KEY`).
2. ```bash
   npm run verify:amoy -- 0xABC123... 0x<tu_direccion_publica>
   ```
   (el 2º argumento es el `adminInicial` del constructor = tu dirección.)

---

## 4. Desplegar el gateway en una PaaS GRATUITA

El servidor está saturado y su Kafka es LAN privada, así que el gateway va en la
nube en **modo solo-REST**. Elige UNA plataforma (todas tienen free tier).
Puerto interno del servicio: **8101**.

### Opción A — Fly.io (recomendada)
```bash
# 1) instalar flyctl (una vez)
curl -L https://fly.io/install.sh | sh        # sigue las instrucciones del PATH
fly auth signup        # o 'fly auth login' si ya tienes cuenta

cd Microservicios/ServiciosDeNegocio/blockchain-gateway-service
fly launch --no-deploy --copy-config --name voltera-blockchain-gateway

# 2) secrets (los valores del paso 1 y 3)
fly secrets set \
  VOLTERA_BLOCKCHAIN_RPC_URL="https://rpc-amoy.polygon.technology" \
  VOLTERA_BLOCKCHAIN_PRIVATE_KEY="0x<tu_clave_de_prueba>" \
  VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS="0xABC123..."

# 3) desplegar
fly deploy

# 4) tu URL pública será algo como:
#    https://voltera-blockchain-gateway.fly.dev
curl https://voltera-blockchain-gateway.fly.dev/actuator/health
```

### Opción B — Render.com
1. Entra a https://render.com y regístrate (puedes usar GitHub).
2. New → **Blueprint** → conecta este repo → detecta `render.yaml`.
3. En el dashboard define los 3 valores marcados `sync:false`:
   `VOLTERA_BLOCKCHAIN_RPC_URL`, `VOLTERA_BLOCKCHAIN_PRIVATE_KEY`,
   `VOLTERA_BLOCKCHAIN_CONTRACT_ADDRESS`.
4. Deploy. URL: `https://voltera-blockchain-gateway.onrender.com`
   (Nota: el free tier de Render "duerme" el servicio tras inactividad; el primer
    request tarda ~30s en despertar.)

### Opción C — Railway
1. https://railway.app → regístrate con GitHub.
2. New Project → Deploy from GitHub repo → selecciona este repo.
   Railway detecta `railway.json` + `Dockerfile`.
3. Variables: `KAFKA_ENABLED=false`, `VOLTERA_BLOCKCHAIN_CHAIN_ID=80002`,
   `VOLTERA_BLOCKCHAIN_NETWORK=polygon-amoy`, y los 3 de cadena del paso 1/3.
4. Settings → Networking → **Generate Domain** para obtener la URL pública.

> Prueba la que elijas: `GET <url>/api/v1/anclajes/red` debe devolver
> `"disponible": true` y tu dirección de contrato.

---

## 5. Publicar la API del gateway en WSO2

Para que el frontend y el server consuman el gateway por el API Gateway:
```bash
cd Microservicios/wso2-gateway
# apunta el backend a tu gateway en la nube
BACKEND=https://voltera-blockchain-gateway.fly.dev ./publish-blockchain-api.sh
```
Esto publica `BlockchainAPI` en el contexto `/blockchain`. Verás la ruta:
`https://192.168.3.13:8243/blockchain/v1/api/v1/anclajes`.

---

## 6. Conectar los microservicios actuales con el blockchain

### 6.1 Que liquidacion-p2p ancle automáticamente al liquidar
En el Deployment de `liquidacion-p2p` (k8s) añade la variable con la URL del gateway:
```yaml
env:
  - name: VOLTERA_BLOCKCHAIN_GATEWAY_URL
    value: "https://voltera-blockchain-gateway.fly.dev"
```
Aplica el cambio (de uno en uno, ver `k8s/DESPLIEGUE-SEGURO.md`):
```bash
kubectl -n voltera-app set env deploy/liquidacion-p2p-service \
  VOLTERA_BLOCKCHAIN_GATEWAY_URL=https://voltera-blockchain-gateway.fly.dev
```
Si dejas la variable vacía, el servicio funciona como hoy (no-op). No rompe nada.

### 6.2 (Alternativa) gateway dentro del cluster
Si algún día liberas el server, en vez de la PaaS usa:
```bash
# crea el secret con tus valores (clave de prueba)
kubectl -n voltera-app create secret generic voltera-blockchain-secret \
  --from-literal=rpc-url='https://rpc-amoy.polygon.technology' \
  --from-literal=private-key='0x<tu_clave_de_prueba>' \
  --from-literal=contract-address='0xABC123...'
# construye y sube la imagen (incluye el gateway en build-push-all.sh)
# luego aplica:
kubectl apply -f k8s/voltera-topics.yaml      # crea topic.blockchain.anclada
kubectl apply -f k8s/voltera-blockchain.yaml
```

---

## 7. Desplegar el FRONTEND a Vercel

El frontend ya tiene la página Marketplace P2P. Para publicarlo:
```bash
cd Microservicios/voltera-frontend
npm install
npx vercel --prod        # te pedirá login la primera vez
```
O conéctalo desde https://vercel.com (Import Project → este repo →
root `Microservicios/voltera-frontend`). El `vercel.json` y el BFF `/api/gw`
ya enrutan hacia WSO2, incluida la ruta `/blockchain`.

> El marketplace llama a `/gw/blockchain/v1/...`, que el BFF proxyea a WSO2. Si
> publicaste la API en el paso 5, funcionará sin cambios de código.

---

## 8. Checklist de verificación final
- [ ] `amoy.polygonscan.com/address/<contrato>` muestra el contrato desplegado.
- [ ] `GET <url-gateway>/api/v1/anclajes/red` → `"disponible": true`.
- [ ] `POST <url-gateway>/api/v1/anclajes` (cuerpo demo) → 202 + `txHash`.
- [ ] `amoy.polygonscan.com/tx/<txHash>` muestra la transacción de anclaje.
- [ ] En el front (Marketplace P2P): publicar venta + compra → "Casar y anclar" →
      el contrato aparece con estado `CONFIRMADO` y enlace a PolygonScan.
- [ ] Botón "🔎 verificar" en un contrato → toast "Integridad verificada ✓".
- [ ] Liquidar vía `liquidacion-p2p` con `VOLTERA_BLOCKCHAIN_GATEWAY_URL` puesto →
      el anclaje aparece en el gateway sin tocar el front.

---

## Apéndice — colección Postman
Importa `Microservicios/Voltera_Postman_Blockchain.json` en Postman. Cambia la
variable `urlGateway` a tu URL (local `http://localhost:8101`, WSO2, o la PaaS) y
ejecuta las requests en orden (health → red → anclar → consultar → verificar).

## Apéndice — costos
Todo es **gratis**: Polygon Amoy es testnet (gas sin valor), el RPC es de un
proveedor con free tier, y Fly/Render/Railway tienen plan gratuito suficiente para
este servicio de bajo tráfico. El único "gasto" es el tiempo de registro.
