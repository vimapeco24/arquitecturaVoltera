# Seguridad OAuth2 - API Gateway WSO2 (Voltera)

Las 6 APIs del gateway WSO2 están protegidas con **OAuth2**. El acceso sin token
es rechazado con 401; solo peticiones con un `Bearer` token válido pasan.

## Arquitectura

```
Postman  --(Bearer token)-->  WSO2 Gateway (tu Mac :8243)  --VPN-->  Microservicios (cluster 192.168.3.220)
             │
             └── token emitido por WSO2 :9443/oauth2/token
```

## Credenciales de la Application (VolteraClient)

La Application `VolteraClient` está suscrita a las 6 APIs. Sus claves de
produccion (client_credentials):

| Dato | Valor |
|------|-------|
| Application | VolteraClient |
| tokenUrl | https://localhost:9443/oauth2/token |
| consumerKey | (ver variable `consumerKey` en la coleccion Postman) |
| consumerSecret | (ver variable `consumerSecret` en la coleccion Postman) |

> Las claves quedaron guardadas en la coleccion Postman como variables.
> Trátalas como secreto: no las subas a un repositorio publico.

## Obtener un token (client_credentials)

```bash
CK="<consumerKey>"; CS="<consumerSecret>"
curl -sk -X POST "https://localhost:9443/oauth2/token" \
  -H "Authorization: Basic $(printf "%s:%s" "$CK" "$CS" | base64)" \
  -d "grant_type=client_credentials"
# -> { "access_token": "...", "expires_in": 3600, ... }
```

## Consumir una API con token

```bash
TOKEN="<access_token>"
curl -sk -H "Authorization: Bearer $TOKEN" \
  "https://localhost:8243/tarifas/v1/api/v1/tarifas"
```

## Prueba de seguridad (verificada)

| Petición | Resultado |
|----------|-----------|
| GET /tarifas SIN token | **401** (rechazado) |
| GET /tarifas CON token | **200** |
| POST /tarifas CON token | **201** |

## Colección Postman

`Voltera_Postman_Collection_gateway.json` ya trae la seguridad integrada:
- Variables `tokenUrl`, `consumerKey`, `consumerSecret`, `accessToken`.
- Un **pre-request a nivel de colección** que obtiene el token automáticamente
  (client_credentials) antes de cada request y lo guarda en `accessToken`.
- Todos los requests envían `Authorization: Bearer {{accessToken}}`.

No tienes que pegar el token a mano: al ejecutar cualquier request, Postman
pide el token solo y lo usa. Validado con newman: 30 requests, 0 fallos.

## Notas

- **tokenType JWT**: la Application emite tokens JWT autocontenidos.
- **validityTime**: 3600s (1h). El pre-request renueva en cada corrida.
- Grants habilitados: `client_credentials`, `password`, `refresh_token`.
- Para revocar acceso: elimina/regenera las keys de la Application en el DevPortal
  (https://localhost:9443/devportal).
