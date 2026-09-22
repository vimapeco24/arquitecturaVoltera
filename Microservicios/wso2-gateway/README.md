# WSO2 API Manager - Gateway de los Microservicios Voltera

Implementación **contenerizada** de WSO2 API Manager 4.7.0 como API Gateway
frente a los 6 microservicios de negocio de Voltera.

## Qué hace

Centraliza el acceso a los microservicios detrás de un solo gateway, con
gestión de APIs, ciclo de vida (publicación), throttling y portal de consumo.

```
Cliente / Postman
      │
      ▼
WSO2 API Manager (Docker)
  · Gateway            :8243 (https) / :8280 (http)
  · Publisher/DevPortal:9443
      │  rutas: /tarifas/v1, /volumetria/v1, ...
      ▼
Microservicios Voltera  (K8s Istio Gateway 192.168.3.220)
```

## Contenido

| Archivo | Descripción |
|---------|-------------|
| `docker-compose.wso2.yml` | Levanta WSO2 API Manager contenerizado |
| `publish-apis.sh` | Registra, crea revisión, despliega y publica las 6 APIs automáticamente vía REST API |
| `wso2/deployment.toml` | Config de referencia (NO se monta; ver nota) |

## Cómo levantarlo

### 1. Iniciar WSO2 (contenedor)
```bash
docker compose -f docker-compose.wso2.yml up -d
```
El arranque tarda ~3-4 min (más en Mac ARM por emulación). Espera a ver
`WSO2 Carbon started` en `docker logs voltera-wso2am`.

Consolas:
- Publisher: https://localhost:9443/publisher (admin/admin)
- DevPortal: https://localhost:9443/devportal
- Admin:     https://localhost:9443/admin

### 2. Publicar las APIs de los microservicios
```bash
# Backend por defecto: http://192.168.3.220 (cluster K8s)
./publish-apis.sh

# O apuntando a otro backend:
BACKEND=http://192.168.3.220 ./publish-apis.sh
```
El script hace por cada servicio: DCR → token OAuth2 → crear API →
**crear revisión → desplegar al gateway** → publicar.

### 3. Consumir vía el gateway
```
https://localhost:8243/<servicio>/v1/api/v1/...
```
Ejemplos verificados:
```bash
curl -sk https://localhost:8243/tarifas/v1/actuator/health          # -> 200
curl -sk https://localhost:8243/tarifas/v1/api/v1/tarifas           # -> 200
curl -sk -X POST https://localhost:8243/tarifas/v1/api/v1/tarifas \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Res","franjas":[{"horaInicio":0,"horaFin":24,"precioKwh":700}]}'  # -> 201
```

## Rutas del gateway (context + version)

| Servicio | Ruta en el gateway |
|----------|--------------------|
| Tarifas | `https://localhost:8243/tarifas/v1/api/v1/tarifas` |
| Volumetria | `https://localhost:8243/volumetria/v1/api/v1/lecturas` |
| Liquidacion P2P | `https://localhost:8243/liquidacion-p2p/v1/api/v1/transacciones` |
| Liquidacion Mensual | `https://localhost:8243/liquidacion-mensual/v1/api/v1/liquidaciones` |
| Facturacion | `https://localhost:8243/facturacion/v1/api/v1/facturas` |
| Pagos | `https://localhost:8243/pagos/v1/api/v1/pagos` |

## Notas importantes

- **Autenticación de las APIs:** el script publica con `authType: None` en los
  recursos (consumo directo sin token, para pruebas). Para producción, cambia a
  OAuth2/API-Key y suscribe una aplicación en el DevPortal.
- **deployment.toml:** NO se monta el toml parcial (rompía la config obligatoria
  de WSO2). Se usa el default de la imagen; los backends se registran vía el
  script. Si necesitas personalizar, monta un `deployment.toml` COMPLETO.
- **Memoria:** WSO2 necesita ~4-6 GB. En Mac ARM corre por emulación (Rosetta) y
  consume más; el compose asigna `mem_limit: 6g`. En servidor amd64, 4 GB bastan.
- **WSO2 4.x requiere desplegar revisión:** publicar el ciclo de vida NO basta;
  hay que crear una revisión y desplegarla al gateway (el script ya lo hace).

## Contenerización para el servidor amd64

En el servidor (nativo amd64) no hace falta `platform: linux/amd64` ni tanta RAM.
Puedes desplegarlo con el mismo compose, o llevarlo a Kubernetes con la imagen
`wso2/wso2am:4.7.0` (WSO2 publica charts de Helm oficiales para producción).
