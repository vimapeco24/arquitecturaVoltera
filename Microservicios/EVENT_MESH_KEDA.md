# Lámina 04 · KEDA y Event Mesh — guía de la implementación

Cómo escala Voltera (KEDA por lag) y cómo se distribuye (event mesh multi-región).
Materializa la lámina 04 en manifiestos reales. No despliega nada por sí sola.

## Archivos
- `k8s/voltera-keda-lamina04.yaml` — 5 ScaledObjects Kafka (Ingesta, Telemetría Core, Proyector, Notificaciones, Habilitación).
- `k8s/voltera-keda-ami-prometheus.yaml` — ScaledObject del Adaptador AMI (Prometheus scaler).
- `k8s/voltera-event-mesh.yaml` — brokers Redpanda por país + replicación (MirrorMaker2) + mTLS + política de residencia.

---

## KEDA · qué mide y cómo decide

KEDA escala por el **lag del consumer group** (mensajes publicados que el grupo aún
no procesó, sumado en todas las particiones), **no por CPU**. La fórmula:

```
replicas_deseadas = ceil( lag_total / lagThreshold )   acotado a [min, max]
```

### Cómo se fija cada valor (regla, no número fijo)
- **Umbral (`lagThreshold`)** = capacidad de una réplica × retraso que tolera el negocio.
  Ingesta: 500 lecturas/s × 10 s = **5.000** mensajes.
- **Mínimo** = carga base ÷ capacidad de una réplica (o 0 si puede esperar).
  Ingesta: 2.200 ÷ 500 ≈ 5 (la lámina fija **2** por ruta crítica 99,99%).
- **Máximo** = pico ÷ capacidad de una réplica → define las **particiones** del tópico.
  Ingesta: 22.000 ÷ 500 = 44 → tópico con **48** particiones (máx 48, porque Kafka
  entrega cada partición a un solo consumidor del grupo: el techo nunca supera las particiones).
- **Activación** (`activationLagThreshold`) = lag mínimo para despertar un servicio en
  0 réplicas. Solo aplica a los que escalan a cero (Notificaciones).

### Tabla implementada (coincide con la lámina 12)

| Componente | Topic (real) | consumerGroup | lagThreshold | min | max |
|---|---|---|---|---|---|
| MS Ingesta | `ami-lecturas-crudas` | `ingesta-consumer` | 5000 | 2 | 48 |
| Telemetría Core (escritura) | `telemetria-lecturas-validadas` | `telemetria-core-consumer` | 5000 | 2 | 48 |
| Proyector (lectura/CQRS) | `telemetria-consumo-intervalos` | `telemetria-core-proyector` | 1000 | 1 | 24 |
| Adaptador AMI | gateway MQTT (Prometheus) | — | 10000 | 2 | 20 |
| Notificaciones (alertas) | `telemetria-alertas` | `notificaciones-consumer` | 500 | 0 | 30 |
| MS Habilitación | `ordenes-instalacion` | `habilitacion-consumer` | 50 | 1 | 4 |

> Nombres de topic: la lámina los escribe con punto (`ami.lecturas.crudas`); el
> broker/código usa guion (`ami-lecturas-crudas`). Los manifiestos usan los reales.

> Proyector: en el código vive dentro de telemetria-core. El ScaledObject usa un
> consumerGroup de proyección (`telemetria-core-proyector`) y apunta hoy a
> `telemetria-core-service`; si se separa en un `proyector-service`, solo cambia
> `scaleTargetRef`.

> Adaptador AMI: NO escala por Kafka sino por **mensajes pendientes en el gateway
> MQTT** del head-end (métrica Prometheus `mqtt_broker_pending_messages`). Por eso
> usa el Prometheus scaler. Es el punto de entrada: mínimo 2 por país, nunca 0.

### Ciclo de vida (reposo → pico → drenaje → reposo)
1. **Reposo**: lag ≈ 0, réplicas en el mínimo.
2. **Pico** (reconexión masiva, p.ej. granizo: 10× lecturas): el lag crece; Kafka
   retiene sin pérdida (contrapresión).
3. **Escalado**: cada `pollingInterval` (10 s) KEDA recalcula réplicas = ceil(lag/umbral),
   hasta el máximo (= particiones).
4. **Drenaje**: Kafka entrega lo pendiente; el lag baja sin pérdida.
5. **Vuelta a reposo**: tras `cooldownPeriod` estable (p.ej. 300 s) se retiran réplicas.

Requisito: KEDA instalado (`helm install keda kedacore/keda -n keda --create-namespace`).
Sin KEDA, los ScaledObject no se crean y los deployments quedan en réplicas fijas.

---

## Event Mesh · cómo se distribuye

Red de brokers Redpanda interconectados entre **Colombia (principal), Chile, México,
Brasil**. Cada región procesa localmente ("las lecturas del país se quedan aquí") y el
mesh replica **solo** los topics críticos.

### Tópico jerárquico
```
voltera/{país}/{dominio}/{evento}
  voltera/co/telemetria/consumo-intervalo
  voltera/cl/habilitacion/medidor-habilitado
  voltera/mx/telemetria/alerta-red
```
Particionado por `medidorId` → conserva el orden por medidor (base del escalado KEDA).

### Qué cruza y qué no (residencia del dato)
- **Cruza**: consumo agregado por intervalo, `MedidorHabilitado` sin datos personales,
  alertas de red. Los consumen los servicios centrales (mesa de energía, reportes CREG,
  analítica) desde `voltera/*/telemetria/consumo-intervalo` sin saber en qué clúster se publicó.
- **No cruza**: lecturas crudas (`.../ami/lectura-cruda`) ni datos personales. El
  `topics.exclude` del replicador lo garantiza.

### Replicación y continuidad
- Replicador tipo **MirrorMaker2** copia la allowlist de topics `cross_region` con
  **mTLS** entre brokers (CA común del mesh, Secret `redpanda-tls`).
- Replica también offsets de consumer groups (`sync.group.offsets.enabled`): si cae una
  región, los consumidores retoman desde los offsets replicados. Objetivo **RTO ≤ 10 min,
  RPO ≤ 10 s**.
- Zero Trust en el bus: cada región define por ACL qué puede publicar/suscribir; el
  listener del mesh (`:9093`) es TLS mutuo, separado del listener interno (`:9092`).

### Despliegue del mesh
`voltera-event-mesh.yaml` trae namespaces por país, el broker de Colombia (plantilla),
los topics jerárquicos (ConfigMap), la config del replicador y el Secret mTLS. Para
Chile/México/Brasil se duplica el bloque del broker cambiando namespace y
`advertise-kafka-addr`. El material mTLS se genera con cert-manager o PKI (nunca se
versiona en git).

---

## Verificación (post-despliegue)
```bash
# KEDA: ScaledObjects y réplicas actuales
kubectl -n voltera-app get scaledobject
kubectl -n voltera-app get hpa          # KEDA crea un HPA por ScaledObject
kubectl -n voltera-app get deploy ingesta-service -o jsonpath='{.spec.replicas}'

# Forzar lag para ver el escalado (publicar al topic sin consumir) y observar:
kubectl -n voltera-app get deploy ingesta-service -w

# Mesh: topics replicados en otra región
kubectl -n voltera-cl exec deploy/redpanda -- rpk topic list | grep consumo-intervalo
```
