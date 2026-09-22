#!/usr/bin/env bash
# ============================================================================
# Voltera mesh | Observabilidad COMPLETA y expuesta (Grafana/Prometheus/Kiali/Jaeger)
# ----------------------------------------------------------------------------
# Reproduce lo aplicado en el cluster de Voltera:
#   1) Instala los addons Grafana y Jaeger (Istio 1.26).
#   2) Fija Grafana/Jaeger/Prometheus al nodo master (el worker está sin memoria)
#      tolerando el taint control-plane.
#   3) Expone los 4 tableros por NodePort para abrirlos desde la LAN.
#
# Tras ejecutarlo, obtén los NodePort y ponlos en el frontend
# (src/environments/environment*.ts -> observabilidad).
#
# URLs actuales (nodo master 192.168.3.11):
#   Grafana     http://192.168.3.11:32421
#   Prometheus  http://192.168.3.11:31659
#   Kiali       http://192.168.3.11:32375/kiali
#   Jaeger      http://192.168.3.11:31402
# ============================================================================
set -euo pipefail
NS="istio-system"
ISTIO_VER="${ISTIO_VER:-1.26}"
MASTER="${MASTER:-k8s-atiesia-master}"
BASE="https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VER}/samples/addons"

PIN='{"spec":{"template":{"spec":{"nodeSelector":{"kubernetes.io/hostname":"'"$MASTER"'"},"tolerations":[{"key":"node-role.kubernetes.io/control-plane","operator":"Exists","effect":"NoSchedule"}]}}}}'

echo ">> Instalando Grafana y Jaeger (release-${ISTIO_VER})..."
kubectl apply -f "${BASE}/grafana.yaml"
kubectl apply -f "${BASE}/jaeger.yaml"

echo ">> Fijando Grafana/Jaeger/Prometheus al master (el worker está sin memoria)..."
for d in grafana jaeger prometheus-server; do
  kubectl -n "$NS" patch deploy "$d" --type strategic -p "$PIN" || true
done

echo ">> Exponiendo por NodePort..."
kubectl -n "$NS" patch svc grafana           -p '{"spec":{"type":"NodePort"}}' || true
kubectl -n "$NS" patch svc tracing           -p '{"spec":{"type":"NodePort"}}' || true
kubectl -n "$NS" patch svc prometheus-server -p '{"spec":{"type":"NodePort"}}' || true
# Kiali ya suele venir como NodePort (20001).

echo ">> Esperando..."
for d in grafana jaeger prometheus-server; do kubectl -n "$NS" rollout status deploy/"$d" --timeout=180s || true; done

echo ">> NodePorts asignados:"
kubectl -n "$NS" get svc grafana tracing prometheus-server kiali -o wide
echo ">> Pon estas URLs (http://<IP-nodo>:<nodePort>) en environment*.ts -> observabilidad"
