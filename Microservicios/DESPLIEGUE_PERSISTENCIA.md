# Despliegue de la persistencia real (láminas 03 · CQRS y 10 · Idempotencia)

Esta guía deja todo listo para activar la persistencia real **sin tocar código**.
Todo se controla con **perfiles de Spring** y variables de entorno. Si no activas
ningún perfil, los servicios siguen funcionando con adaptadores **in-memory** (como
hasta ahora).

## Qué se persiste y dónde

| Dato | Adaptador real | Perfil | Backend |
|---|---|---|---|
| Vista de lectura por intervalo (`ConsumoVista`) | `RedisConsumoVistaRepository` | `redis` | Redis |
| Vista agregada por medidor (`ConsumoAgregadoVista`) | `RedisConsumoAgregadoRepository` | `redis` | Redis |
| Serie de medición append-only (`SerieDeMedicion`) | `JdbcSerieRepository` | `tsdb` | TimescaleDB |
| Tabla inbox de idempotencia (todos los consumidores) | `JdbcInbox` | `inbox-jdbc` | PostgreSQL/TimescaleDB |

- **telemetria-core-service**: usa `redis`, `tsdb` y (opcional) `inbox-jdbc`.
- **ingesta, tarifas, liquidacion-mensual, facturacion, notificaciones, integracion-ami, habilitacion, tarifa-eventos**: usan `inbox-jdbc`.

Sin perfil → in-memory. Es seguro desplegar primero sin perfiles y activarlos después.

## Tablas (DDL idempotente, la crean los servicios al arrancar)

- `inbox_evento (consumidor, clave, tipo_evento, primera_vez, veces_vistas)` — PK `(consumidor, clave)`, upsert `ON CONFLICT DO NOTHING`.
- `tc_lectura (medidor_serial, consumo_kwh, capturada_en)` — append-only; hypertable TimescaleDB si la extensión está disponible.
- `tc_serie_snapshot (medidor_serial PK, ...)` — snapshot para rehidratar la serie.

Las claves/vistas de Redis: `tc:vista:{serial}` (Hash), `tc:vista:index` (Set), `tc:agregado` (Hash).

---

## Opción A — Local con Docker Compose

```bash
cd Microservicios
docker compose -f docker-compose.persistence.yml up -d
# Levanta: redis:6379 y timescaledb:5432 (crea las bases voltera_tsdb y voltera_inbox).
```

Arrancar un servicio con persistencia (ejemplo telemetria-core):

```bash
cd ServiciosDeNegocio/telemetria-core-service
SPRING_PROFILES_ACTIVE=redis,tsdb,inbox-jdbc \
REDIS_HOST=localhost REDIS_PORT=6379 \
TSDB_URL=jdbc:postgresql://localhost:5432/voltera_tsdb TSDB_USER=voltera TSDB_PASSWORD=voltera \
INBOX_URL=jdbc:postgresql://localhost:5432/voltera_inbox INBOX_USER=voltera INBOX_PASSWORD=voltera \
mvn spring-boot:run
```

Un consumidor con solo inbox (ejemplo tarifas):

```bash
cd ServiciosDeNegocio/tarifas-service
SPRING_PROFILES_ACTIVE=inbox-jdbc \
INBOX_URL=jdbc:postgresql://localhost:5432/voltera_inbox INBOX_USER=voltera INBOX_PASSWORD=voltera \
mvn spring-boot:run
```

---

## Opción B — Kubernetes (namespace `voltera-app`)

1. Desplegar Redis + TimescaleDB:

```bash
kubectl apply -f k8s/voltera-persistence.yaml
kubectl -n voltera-app rollout status deployment/timescaledb
kubectl -n voltera-app rollout status deployment/redis
```

2. Crear la base del inbox (una vez):

```bash
kubectl -n voltera-app exec deploy/timescaledb -- \
  psql -U voltera -d voltera_tsdb -c "CREATE DATABASE voltera_inbox OWNER voltera;"
```

3. Activar perfiles en los servicios (sin editar YAML, con `set env`). Ver el detalle
   y el fragmento `env:` en `k8s/voltera-persistence-env.yaml`:

```bash
# telemetria-core: redis + tsdb + inbox
kubectl -n voltera-app set env deployment/telemetria-core-service \
  SPRING_PROFILES_ACTIVE=redis,tsdb,inbox-jdbc \
  REDIS_HOST=redis.voltera-app REDIS_PORT=6379 \
  TSDB_URL=jdbc:postgresql://timescaledb.voltera-app:5432/voltera_tsdb TSDB_USER=voltera TSDB_PASSWORD=voltera \
  INBOX_URL=jdbc:postgresql://timescaledb.voltera-app:5432/voltera_inbox INBOX_USER=voltera INBOX_PASSWORD=voltera

# resto de consumidores: solo inbox
for s in ingesta tarifas liquidacion-mensual facturacion notificaciones integracion-ami habilitacion tarifa-eventos; do
  kubectl -n voltera-app set env deployment/${s}-service \
    SPRING_PROFILES_ACTIVE=inbox-jdbc \
    INBOX_URL=jdbc:postgresql://timescaledb.voltera-app:5432/voltera_inbox INBOX_USER=voltera INBOX_PASSWORD=voltera
done
```

> Nota: los deployments de estos 9 servicios de negocio deben existir en el clúster.
> Los manifiestos EDA actuales (`k8s/voltera-eda.yaml`) cubren siniestros, reaseguro,
> telemetria y tarifa-eventos. Si falta algún deployment, créalo con el mismo patrón
> (imagen en Harbor `192.168.3.13/app-voltaire/<svc>:1.0.0`, `KAFKA_BOOTSTRAP_SERVERS`,
> readinessProbe `/actuator/health`) y añade el bloque `env:` de perfiles.

---

## Verificación post-despliegue

```bash
# Inbox: filas registradas
kubectl -n voltera-app exec deploy/timescaledb -- \
  psql -U voltera -d voltera_inbox -c "SELECT consumidor, count(*) FROM inbox_evento GROUP BY 1;"

# Serie TSDB
kubectl -n voltera-app exec deploy/timescaledb -- \
  psql -U voltera -d voltera_tsdb -c "SELECT count(*) FROM tc_lectura;"

# Redis: vistas agregadas
kubectl -n voltera-app exec deploy/redis -- redis-cli HGETALL tc:agregado

# API de lectura (vista agregada) de telemetria-core
curl -s http://<host>/telemetria-core/api/v1/telemetria-core/consumo-agregado | jq .
```

## Rollback (volver a in-memory)

```bash
kubectl -n voltera-app set env deployment/telemetria-core-service SPRING_PROFILES_ACTIVE-
# idem para el resto; o quitar inbox-jdbc del valor.
```

Al quitar el perfil, el servicio vuelve a los adaptadores in-memory. Los datos en
Redis/Timescale quedan intactos para cuando se reactive.

## Build de imágenes

El patrón de build/push a Harbor y rollout ya está en `k8s/deploy-lamina10.sh` y
`ESTADO_SESION_EDA.md` (build `--load` + `docker push`, por el registry autofirmado).
Reconstruye las imágenes de los servicios tocados antes del rollout:

```bash
# por cada servicio modificado
docker buildx build --platform linux/amd64 \
  -t 192.168.3.13/app-voltaire/<svc>:1.0.0 --load ServiciosDeNegocio/<svc>
docker push 192.168.3.13/app-voltaire/<svc>:1.0.0
kubectl -n voltera-app rollout restart deployment/<svc>
```
