# Voltera · Frontend Angular — Comunidades Energéticas

Interfaz web elegante para la plataforma **Voltera** (Universidad de los Andes ·
ARTI4208 Arquitectura de Nueva Generación). Consume los **6 microservicios de
negocio** a través del **API Gateway WSO2** con seguridad **OAuth2**.

## Stack

- Angular 18 (standalone components, lazy routes, signals)
- HttpClient + interceptor funcional para OAuth2 (client_credentials)
- SCSS con sistema de diseño propio (tema energía renovable, modo oscuro)

## Arquitectura del front

```
src/app/
├── core/
│   ├── auth.service.ts       OAuth2 client_credentials + cache de token
│   ├── auth.interceptor.ts   Inyecta Bearer en llamadas al gateway
│   ├── gateway.ts            Constructor de URLs {gw}/<servicio>/v1/...
│   ├── models.ts             Tipos TS espejo de los DTOs de Java
│   └── toast.service.ts      Notificaciones
├── services/                 Un servicio por microservicio + health
└── pages/                    dashboard + 6 páginas de dominio
```

## Servicios consumidos (vía gateway)

| Paso | Servicio | Ruta base en gateway |
|---|---|---|
| 1 | Tarifas | `/tarifas/v1/api/v1/tarifas` |
| 2 | Volumetría | `/volumetria/v1/api/v1/lecturas` |
| 3 | Liquidación P2P | `/liquidacion-p2p/v1/api/v1/transacciones` |
| 4 | Liquidación Mensual | `/liquidacion-mensual/v1/api/v1/liquidaciones` |
| 5 | Facturación | `/facturacion/v1/api/v1/facturas` |
| 6 | Pagos | `/pagos/v1/api/v1/pagos` |

## OAuth2

El `AuthService` solicita un token a `POST /oauth2/token`
(`grant_type=client_credentials`) usando `Basic base64(consumerKey:consumerSecret)`,
lo cachea en memoria y lo renueva automáticamente. El interceptor lo añade como
`Authorization: Bearer <token>` a todas las peticiones dirigidas al gateway.

> Nota de seguridad: en un despliegue real el `consumerSecret` no debería viajar al
> navegador. Lo ideal es un **BFF** que custodie el secret. Aquí se mantiene en el
> cliente para el flujo demo académico.

## Ejecutar en desarrollo

Requiere **VPN Plus activa** para alcanzar el servidor `192.168.3.13`.

```bash
npm install
npm start          # ng serve con proxy.conf.json
# abre http://localhost:4200
```

El `proxy.conf.json` redirige:
- `/gw/**`      → `https://192.168.3.13:8243`  (gateway, `secure:false` por cert autofirmado)
- `/oauth2/**`  → `https://192.168.3.13:9443`  (token endpoint)

## Build de producción

```bash
npm run build
# salida: dist/voltera-frontend
```

En producción (`environment.prod.ts`) las URLs apuntan directamente al gateway
(`https://192.168.3.13:8243` y `:9443`). Ajusta esos valores si el gateway cambia
de host.
