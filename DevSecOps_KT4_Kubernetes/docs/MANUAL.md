# Пошаговая установка вручную

## 1. Подготовка виртуальных машин

Три свежих Ubuntu Server 22.04/24.04 x86_64 с устойчивыми адресами, например:

| Имя | IP | Роль |
|---|---|---|
| `master` | `192.168.0.10` | управляющий узел |
| `worker1` | `192.168.0.11` | рабочая нода |
| `worker2` | `192.168.0.12` | рабочая нода |

На контролирующей машине с SSH-клиентом:

```bash
ssh-keygen -t ed25519
ssh-copy-id ubuntu@192.168.0.10
ssh-copy-id ubuntu@192.168.0.11
ssh-copy-id ubuntu@192.168.0.12
```

Используйте свои IP и пользователя. У SSH-ключа может быть парольная фраза, а `ssh-agent` позволяет не вводить её при каждом входе. Настройте доступ `sudo` на всех узлах.

## 2. Установка Docker, containerd, bridge-utils и Kubernetes

Перенесите каталог проекта на каждую ноду (через `scp`, `git clone` или общий каталог). Затем запускайте **на каждой ноде** из каталога проекта:

```bash
sudo bash scripts/prepare-node.sh
```

Скрипт устанавливает Docker Engine из подписанного официального репозитория, `bridge-utils`, `containerd`, добавляет пакеты `kubelet`, `kubeadm`, `kubectl` для ветки `v1.37`, отключает swap и задаёт параметры ядра.

Проверить:

```bash
sudo docker --version
sudo containerd --version
kubeadm version
sudo systemctl is-active containerd
lsmod | grep br_netfilter
```

## 3. Инициализация control plane

**Только на master**:

```bash
sudo bash scripts/init-control-plane.sh 192.168.0.10
```

После выполнения скрипт выведет команду вида:

```text
kubeadm join 192.168.0.10:6443 --token ... --discovery-token-ca-cert-hash sha256:...
```

Она содержит секретный временный токен, храните её только в терминале. Выполните эту команду с `sudo` **на каждом worker**. Если `kubeadm` не определяет CRI самостоятельно, добавьте параметр `--cri-socket=unix:///run/containerd/containerd.sock`.

Если токен истёк, на master выполните:

```bash
sudo kubeadm token create --ttl 30m --print-join-command
```

## 4. Проверка сети и узлов

На master под обычным пользователем (после размещения `~/.kube/config`):

```bash
kubectl get nodes -o wide
kubectl -n kube-flannel get pods
```

Должны появиться три узла со статусом `Ready`. Сеть Pod - `10.244.0.0/16`; сеть Service - `10.96.0.0/12`. Следите, чтобы они не пересекались с физической сетью.

## 5. Запуск Nginx и сервисов

На master, в проекте:

```bash
bash scripts/deploy-nginx.sh
kubectl -n kt4 get pods -o wide
kubectl -n kt4 get svc
bash scripts/verify.sh
```

Скрипт создаёт Namespace `kt4`, ConfigMap, учебный TLS Secret, Deployment (6 реплик), Service HTTP 80 и Service HTTPS 443. Сертификат создаётся вне репозитория (`~/.local/share/kt4-tls`).

## 6. Доступ с рабочего компьютера

Сервисы имеют тип `ClusterIP` и не открыты в публичный интернет. Чтобы посмотреть их локально, установите SSH-подключение к master и используйте `kubectl port-forward`:

```bash
kubectl -n kt4 port-forward svc/nginx-http 8080:80
```

В другом терминале на **той же машине**: `curl http://127.0.0.1:8080`.

Для HTTPS:

```bash
kubectl -n kt4 port-forward svc/nginx-https 8443:443
```

В другом терминале: `curl -k https://127.0.0.1:8443`.

Если `port-forward` выполнен на удалённой ВМ, из локального компьютера подключайтесь через SSH-туннель или запускайте его в пределах этой ВМ. `curl -k` допустим только для учебного самостоятельно подписанного сертификата.

## 7. Разрешённые сетевые соединения

На самих нодах должны быть доступны порты: `6443/TCP` (API на master), `10250/TCP` (kubelet), `8472/UDP` (Flannel VXLAN). Разрешайте их только между узлами и доверенным администратором, не публично. Для UFW пример ограниченного правила:

```bash
sudo ufw allow from 192.168.0.0/24 to any port 6443 proto tcp
sudo ufw allow from 192.168.0.0/24 to any port 10250 proto tcp
sudo ufw allow from 192.168.0.0/24 to any port 8472 proto udp
```

UFW может также блокировать переадресацию трафика Pod. При активном UFW проверьте forwarding и правила CNI, а также откройте обязательные порты Kubernetes по документации. **Не отключайте файрвол на рабочем сервере без согласования.**
