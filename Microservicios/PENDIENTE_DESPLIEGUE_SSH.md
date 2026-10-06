# PENDIENTE_DESPLIEGUE_SSH.md — Qué hacer cuando habiliten el acceso

Estado al 2026-10-06: **todo el código, manifiestos, scripts y docs están listos y
verificados** (15 servicios BUILD SUCCESS, tests verdes; YAML y bash validados).
Lo único pendiente es **ejecutar el despliegue y verificar en vivo**, que requiere
acceso por SSH al clúster. Este archivo es el checklist para ese día.

---

## 0) Datos de acceso (de las notas; confirmar/rotar credenciales)
- Synology / k8s:   `ssh atiesia@192.168.3.11`  (pass por variable de entorno, NO en chat)
- Namespace:        `voltera-app`
- Registry Harbor:  `192.168.3.13/app-voltaire`  (login `voltera-user`)
- Requiere ruta a la LAN `192.168.3.x` (VPN o misma red). Verificar primero:
  ```bash
  nc -z -G 5 192.168.3.11 22 && echo "SSH OK" || echo "cluster inaccesible"
  ```

---

## 1) Prerrequisitos en el clúster (una sola vez)
```bash
# KEDA (operador de autoescalado por eventos)
helm repo add kedacore https://kedacore.github.io/charts && helm repo update
helm install keda kedacore/keda -n keda --create-namespace

# Secret para bajar imagenes de Harbor (si no existe)
kubectl -n voltera-app create secret docker-registry harbor-cred \
  --docker-server=192.168.3.13 \
  --docker-username=voltera-user --docker-password='***' || true
```

---

## 2) Construir y subir imágenes (desde la Mac, con Docker corriendo)
```bash
cd Microservicios/k8s
./build-push-all.sh          # 16 imagenes amd64 -> Harbor
```
> Estrategia del Harbor autofirmado: el script usa `buildx --load` + `docker push`
> (ver ESTADO_SESION_EDA.md). Si falla el push por TLS, revisar que el daemon tenga
> 192.168.3.13 como insecure registry.

---

## 3) Desplegar todo en orden (script maestro)
```bash
cd Microservicios/k8s

# 3a. Validar sin aplicar (contra el API server)
./deploy-eda-completo.sh --dry-run

# 3b. Aplicar todo (persistencia -> redpanda/eda -> negocio-eda -> KEDA)
./deploy-eda-completo.sh

# 3c. (opcional) incluir el event mesh multi-region
./deploy-eda-completo.sh --with-mesh
```
El script hace, en orden:
1. Redis + TimescaleDB (`voltera-persistence.yaml`) y crea la base `voltera_inbox`.
2. Redpanda + servicios EDA base (`voltera-eda.yaml`).
3. Servicios de negocio EDA (`voltera-negocio-eda.yaml`): habilitacion, integracion-ami,
   ingesta, telemetria-core, notificaciones (con env de perfiles de persistencia).
4. `set env` perfil `inbox-jdbc` a tarifas/facturacion/liquidacion-mensual.
5. KEDA ScaledObjects (`voltera-keda-lamina04.yaml` + `voltera-keda-ami-prometheus.yaml`).

---

## 4) mTLS del event mesh (solo si usas --with-mesh)
Pendiente generar el material real (el Secret `redpanda-tls` es placeholder):
```bash
# Opcion recomendada: cert-manager con una CA comun para las 4 regiones.
# Crear Issuer (CA) + Certificate por broker (co/cl/mx/br) y montar en redpanda-tls.
# Nunca versionar las llaves privadas en git.
```

---

## 5) Verificación en vivo (lo que NO se ha podido probar sin servidor)
```bash
# Pods arriba
kubectl -n voltera-app get pods

# KEDA activo (crea un HPA por ScaledObject)
kubectl -n voltera-app get scaledobject,hpa

# Inbox persistente (idempotencia) — tabla creada por los servicios al arrancar
kubectl -n voltera-app exec deploy/timescaledb -- \
  psql -U voltera -d voltera_inbox -c "\dt"
kubectl -n voltera-app exec deploy/timescaledb -- \
  psql -U voltera -d voltera_inbox -c "SELECT consumidor, count(*) FROM inbox_evento GROUP BY 1;"

# Serie TSDB (append-only) y snapshot
kubectl -n voltera-app exec deploy/timescaledb -- \
  psql -U voltera -d voltera_tsdb -c "SELECT count(*) FROM tc_lectura;"

# Redis: vista agregada del lado query
kubectl -n voltera-app exec deploy/redis -- redis-cli HGETALL tc:agregado

# API de lectura (vista agregada) de telemetria-core
curl -s http://<host-ingress>/telemetria-core/api/v1/telemetria-core/consumo-agregado | jq .
```

### Prueba de escalado KEDA (pico)
```bash
# Observa como ingesta sube de replicas cuando crece el lag
kubectl -n voltera-app get deploy ingesta-service -w
# (en otra terminal, generar carga publicando a ami-lecturas-crudas)
```

### Prueba de idempotencia
Reenviar el mismo evento (mismo eventId) dos veces y confirmar que `veces_vistas`
sube pero el efecto de negocio NO se duplica (una sola factura/consumo).

---

## 6) Rollback (volver a in-memory sin perder datos)
```bash
# Quitar el perfil de persistencia de un servicio -> vuelve a in-memory
kubectl -n voltera-app set env deployment/telemetria-core-service SPRING_PROFILES_ACTIVE-
# Los datos en Redis/Timescale quedan intactos para reactivar luego.
```

---

## Checklist rápido (marcar al ejecutar)
- [ ] Ruta a LAN 192.168.3.x / VPN OK
- [ ] KEDA instalado (`helm`)
- [ ] Secret `harbor-cred` presente
- [ ] `build-push-all.sh` OK (16 imagenes en Harbor)
- [ ] `deploy-eda-completo.sh --dry-run` sin errores
- [ ] `deploy-eda-completo.sh` aplicado
- [ ] Pods `Running` y `scaledobject/hpa` creados
- [ ] Tablas `inbox_evento` / `tc_lectura` creadas; Redis con `tc:agregado`
- [ ] Prueba de escalado KEDA observada
- [ ] Prueba de idempotencia (duplicado no reprocesa)
- [ ] (opcional) Event mesh + mTLS con certificados reales

---

## Referencias
- `DESPLIEGUE_PERSISTENCIA.md` — detalle de perfiles, variables y rollback.
- `EVENT_MESH_KEDA.md` — fórmulas KEDA, topics del mesh, política de residencia.
- `ESTADO_SESION_EDA.md` — estrategia de build/push al Harbor autofirmado.
- Manifiestos: `k8s/voltera-persistence.yaml`, `voltera-negocio-eda.yaml`,
  `voltera-keda-lamina04.yaml`, `voltera-keda-ami-prometheus.yaml`,
  `voltera-event-mesh.yaml`.

## Lo que ya quedó hecho (no repetir)
- Código láminas 03 y 10 (alertas, ECST, CQRS, idempotencia) — 15 servicios en verde.
- Adaptadores de persistencia por perfil (Redis/TSDB/inbox JDBC) con in-memory por defecto.
- Manifiestos KEDA (6 ScaledObjects) + event mesh + 5 deployments EDA faltantes.
- Scripts build/deploy y documentación.
