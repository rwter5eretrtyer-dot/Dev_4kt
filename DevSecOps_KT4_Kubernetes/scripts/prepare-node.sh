#!/usr/bin/env bash
# Run on each fresh Ubuntu 22.04/24.04 AMD64 node as root.
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo 'Error: run with sudo.' >&2
  exit 1
fi

source /etc/os-release
if [[ "${ID}" != 'ubuntu' ]] || [[ "$(dpkg --print-architecture)" != 'amd64' ]]; then
  echo 'Error: this project requires Ubuntu AMD64.' >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y apt-transport-https bridge-utils ca-certificates curl gpg openssl

# kubelet requires swap to be disabled by default.
swapoff -a
cp -n /etc/fstab /etc/fstab.kt4.backup || true
sed -Ei '/^[[:space:]]*#/!{/[[:space:]]swap[[:space:]]/s/^/# /;}' /etc/fstab

# Kernel modules and sysctl settings needed by CNI.
printf 'overlay\nbr_netfilter\n' > /etc/modules-load.d/kt4-kubernetes.conf
modprobe overlay
modprobe br_netfilter
cat > /etc/sysctl.d/99-kt4-kubernetes.conf <<'SYSCTL'
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
SYSCTL
sysctl --system > /dev/null

# Install Docker Engine, which includes containerd.io, via signed apt repo.
install -d -m 0755 /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod 0644 /etc/apt/keyrings/docker.asc
printf 'deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu %s stable\n' "${UBUNTU_CODENAME:-$VERSION_CODENAME}" > /etc/apt/sources.list.d/kt4-docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Kubernetes talks to containerd through its CRI socket, not to the Docker daemon.
install -d -m 0755 /etc/containerd
containerd config default | sed 's/SystemdCgroup = false/SystemdCgroup = true/' > /etc/containerd/config.toml
if ! grep -Eq 'SystemdCgroup *= *true' /etc/containerd/config.toml; then
  echo 'Error: could not enable systemd cgroups in containerd.' >&2
  exit 1
fi
systemctl enable --now containerd docker
systemctl restart containerd

# Official Kubernetes v1.37 package repository and packages.
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.37/deb/Release.key -o /etc/apt/keyrings/kubernetes-apt-keyring.asc
chmod 0644 /etc/apt/keyrings/kubernetes-apt-keyring.asc
cat > /etc/apt/sources.list.d/kt4-kubernetes.list <<'K8S'
deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.asc] https://pkgs.k8s.io/core:/stable:/v1.37/deb/ /
K8S
apt-get update
apt-get install -y kubelet kubeadm kubectl
apt-mark hold kubelet kubeadm kubectl
systemctl enable kubelet

printf '\nPreparation finished on %s. Next: kubeadm init or kubeadm join.\n' "$(hostname)"
