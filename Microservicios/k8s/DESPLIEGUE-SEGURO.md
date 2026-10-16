# Despliegue seguro — Voltera (NO saturar el server)

> **Leer ANTES de ejecutar `build-push-all.sh` o `deploy-eda-completo.sh`.**

## Regla de oro
Antes de subir/re-desplegar CUALQUIER microservicio, **validar primero en qué nodo está y la
carga de ese nodo**. El master tiene solo **6 GB** y además corre el control-plane + etcd; si se
le suma un pod pesado sin verificar, entra en **OOM** y tumba en cascada:

```
master sin RAM → OOM kube-apiserver → MetalLB cae → el LB 192.168.3.220 deja de anunciarse
             → Istio no enruta → el gateway WSO2 responde 500/503 → el front muestra errores
```

El worker tiene **8 GB** y más margen. Objetivo: mantener el master por debajo de ~85% de memoria.

## Paso a paso (obligatorio)

### 1. ¿Dónde está cada servicio y cómo está el balance?
```bash
kubectl get pods -n voltera-app -o custom-columns="POD:.metadata.name,NODE:.spec.nodeName" --no-headers | sort -k2
kubectl get pods -n voltera-app -o custom-columns="N:.spec.nodeName" --no-headers | sort | uniq -c
```

### 2. ¿Cuánta RAM real hay libre por nodo?
```bash
kubectl top nodes
```
- master **> 85% mem** → NO desplegar ahí. Anclar el servicio al worker o liberar antes.

### 3. Reparto declarado (fuente de verdad: `voltera-scheduling.yaml`)
- **WORKER** (anclados con nodeSelector): `volumetria-service`, `liquidacion-p2p-service`, `siniestros-service`.
- Resto: libre, pero vigilar que el master no pase de ~85%.
- Aplicar/mantener el reparto + topes de memoria:
  ```bash
  ./aplicar-scheduling.sh          # ancla al worker + pone limits de memoria
  ./aplicar-scheduling.sh --revert # revierte el nodeSelector si hace falta
  ```

### 4. Desplegar de UNO EN UNO (nunca todos a la vez)
```bash
kubectl set image deploy/<svc> -n voltera-app <svc>=192.168.3.13/app-voltaire/<svc>:<TAG>
# esperar 2/2 Ready ANTES del siguiente:
kubectl get pods -n voltera-app -l app=<svc> -o wide
```
- NO usar `kubectl rollout status` bloqueante por SSH si el master está cargado (cuelga la sesión).
  Usar consultas con `--request-timeout=20s`.

### 5. Verificar por el gateway (no solo el pod)
```bash
# token
TOK=$(curl -sk -u <CK>:<CS> -d grant_type=client_credentials https://192.168.3.13:9443/oauth2/token | jq -r .access_token)
# health del servicio a través del gateway
curl -sk -H "Authorization: Bearer $TOK" https://192.168.3.13:8243/<svc>/v1/actuator/health   # -> 200
```

## Endpoints de API en WSO2: SIEMPRE al LoadBalancer, nunca a un pod
Al crear/editar una API en WSO2, el backend debe ser:
```
http://192.168.3.220/<contexto>      # LB de Istio (MetalLB)
```
NUNCA una IP de pod (`10.x`) ni de nodo. Así, cuando el pod cambie de nodo (reprogramación o
escalado KEDA), el gateway lo sigue alcanzando sin reconfigurar nada. El Service `istio-ingress`
usa `externalTrafficPolicy: Cluster`, que balancea a cualquier pod en cualquier nodo.

## Topes ya aplicados (mantener)
- `limits.memory` en todos los deployments (512Mi; telemetria-core 640Mi) → ningún pod dispara el OOM del nodo.
- KEDA `maxReplicaCount` acotado (4–6, NO 48) en `voltera-keda-ajustado.yaml` → el autoescalado no revienta el cluster.

## Checklist rápido
- [ ] `kubectl top nodes` → master < 85% mem
- [ ] `kubectl get pods -n voltera-app -o wide` → nodo de cada pod + balance
- [ ] desplegar de uno en uno, esperar `2/2 Ready` entre cada uno
- [ ] `GET .../<svc>/v1/actuator/health` por el gateway → 200
- [ ] endpoint de API nueva en WSO2 = `http://192.168.3.220/<ctx>` (LB, no pod)
