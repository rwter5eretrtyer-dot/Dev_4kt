#!/usr/bin/env bash
# Run on control plane where kubectl and the cluster kubeconfig are configured.
set -Eeuo pipefail

printf '\n1. Cluster nodes:\n'
kubectl get nodes -o wide

printf '\n2. Deployment, Pods and Services:\n'
kubectl -n kt4 get deployment,pods,services -o wide

printf '\n3. Check deployment readiness:\n'
kubectl -n kt4 rollout status deployment/nginx --timeout=300s
READY="$(kubectl -n kt4 get deployment nginx -o jsonpath='{.status.readyReplicas}')"
if [[ "${READY:-0}" != 6 ]]; then
  echo "ERROR: 6 ready replicas expected, found ${READY:-0}" >&2
  exit 1
fi

HTTP_IP="$(kubectl -n kt4 get svc nginx-http -o jsonpath='{.spec.clusterIP}')"
HTTPS_IP="$(kubectl -n kt4 get svc nginx-https -o jsonpath='{.spec.clusterIP}')"
printf '\n4. HTTP service %s:80:\n' "${HTTP_IP}"
curl --fail --silent --show-error --max-time 15 "http://${HTTP_IP}:80/"

printf '\n5. HTTPS service %s:443 (self-signed lab cert):\n' "${HTTPS_IP}"
curl --insecure --fail --silent --show-error --max-time 15 "https://${HTTPS_IP}:443/"

printf '\nPASS: 6 Nginx replicas ready; HTTP and HTTPS services responded.\n'
