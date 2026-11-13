#!/usr/bin/env bash
# Deploy application after the three nodes become Ready.
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
KUBECTL=(kubectl)

"${KUBECTL[@]}" apply -f "${ROOT}/k8s/00-namespace.yaml"

# A self-signed certificate is for the isolated lab only. Keep the key off GitHub.
TLS_DIR="${HOME}/.local/share/kt4-tls"
umask 077
mkdir -p "${TLS_DIR}"
if [[ ! -f "${TLS_DIR}/tls.crt" || ! -f "${TLS_DIR}/tls.key" ]]; then
  openssl req -x509 -newkey rsa:2048 -nodes -days 30 \
    -keyout "${TLS_DIR}/tls.key" -out "${TLS_DIR}/tls.crt" \
    -subj '/CN=nginx-https'
fi

"${KUBECTL[@]}" -n kt4 create secret tls nginx-tls \
  --cert="${TLS_DIR}/tls.crt" --key="${TLS_DIR}/tls.key" \
  --dry-run=client -o yaml | "${KUBECTL[@]}" apply -f -

"${KUBECTL[@]}" apply -f "${ROOT}/k8s/01-configmap.yaml"
"${KUBECTL[@]}" apply -f "${ROOT}/k8s/02-deployment.yaml"
"${KUBECTL[@]}" apply -f "${ROOT}/k8s/03-service-http.yaml"
"${KUBECTL[@]}" apply -f "${ROOT}/k8s/04-service-https.yaml"
"${KUBECTL[@]}" -n kt4 rollout status deployment/nginx --timeout=300s
