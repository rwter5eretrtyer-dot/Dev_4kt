#!/usr/bin/env bash
# Run with sudo on control-plane node, pass its reachable interface IP.
set -Eeuo pipefail
if [[ "${EUID}" -ne 0 ]] || [[ $# -ne 1 ]]; then
  echo 'Usage: sudo bash scripts/init-control-plane.sh <CONTROL_PLANE_IP>' >&2
  exit 1
fi
CONTROL_PLANE_IP="$1"
if ! [[ "${CONTROL_PLANE_IP}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo 'Error: expected IPv4 address.' >&2
  exit 1
fi

if [[ ! -f /etc/kubernetes/admin.conf ]]; then
  kubeadm init \
    --apiserver-advertise-address="${CONTROL_PLANE_IP}" \
    --pod-network-cidr=10.244.0.0/16 \
    --service-cidr=10.96.0.0/12 \
    --cri-socket=unix:///run/containerd/containerd.sock
fi

# Protect admin kubeconfig in the SSH user's account.
USERNAME="${SUDO_USER:-root}"
USER_HOME="$(getent passwd "${USERNAME}" | cut -d: -f6)"
install -d -m 0700 -o "${USERNAME}" -g "$(id -gn "${USERNAME}")" "${USER_HOME}/.kube"
install -m 0600 -o "${USERNAME}" -g "$(id -gn "${USERNAME}")" /etc/kubernetes/admin.conf "${USER_HOME}/.kube/config"

# Flannel release manifest is the supported installation artifact.
kubectl --kubeconfig=/etc/kubernetes/admin.conf apply -f https://github.com/flannel-io/flannel/releases/download/v0.28.9/kube-flannel.yml

printf '\nControl plane initialized. Run this JOIN command with sudo on each worker (do not commit it to Git):\n'
kubeadm token create --ttl 30m --print-join-command
printf '\nIf several CRI runtimes are installed, append: --cri-socket=unix:///run/containerd/containerd.sock\n'
