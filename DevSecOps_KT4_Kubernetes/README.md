# КТ-4 по DevSecOps: кластер Kubernetes и Nginx

**Студент:** Алакберова Симран Ахлиман  
**Группа:** 4П1.23  
**Дисциплина:** DevSecOps  
**Тема:** установка Kubernetes на трёх Ubuntu-узлах, развёртывание Nginx в 6 репликах, настройка HTTP и HTTPS

## Что реализовано

- Подготовка трёх узлов Ubuntu (пример адресов: `192.168.0.10`, `.11`, `.12`).
- Установка Docker Engine, `bridge-utils`, `containerd`, `kubelet`, `kubeadm`, `kubectl`.
- Обмен SSH-ключами между управляющей машиной и узлами без хранения паролей в Git.
- Создание управляющего узла (control plane) через `kubeadm init` и присоединение двух рабочих узлов через `kubeadm join`.
- Настройка сети Flannel (`10.244.0.0/16`) и отдельной подсети сервисов Kubernetes (`10.96.0.0/12`).
- Развёртывание `Deployment` с **6 репликами Nginx**.
- Два сервиса: `nginx-http:80` и `nginx-https:443`. HTTPS использует TLS-секрет; учебный сертификат создаётся на сервере, но не сохраняется в репозитории.
- Скрипты для проверки: ноды `Ready`, 6 готовых Pod, HTTP/HTTPS.

**Важно:** исходный материал КТ-4 описывает устаревший Kubernetes `v0.17.0`, `minions`, `ReplicationController` и `kubectl run-container`. Здесь сохранена учебная цель, но команды адаптированы к поддерживаемой ветке Kubernetes **v1.37**, `kubeadm`, `Deployment` и `Service`. Современная версия использует **containerd** через CRI; Docker Engine установлен для соответствия лабораторному заданию, но не является CRI Kubernetes.

## Структура проекта

```text
DevSecOps_KT4_Kubernetes/
├── README.md
├── ansible.cfg
├── inventory/
│   └── hosts.ini.example
├── playbooks/
│   ├── 01_prepare_nodes.yml
│   ├── 02_init_control_plane.yml
│   ├── 03_join_workers.yml
│   └── 04_deploy_nginx.yml
├── scripts/
│   ├── prepare-node.sh
│   ├── init-control-plane.sh
│   ├── deploy-nginx.sh
│   └── verify.sh
├── k8s/
│   ├── 00-namespace.yaml
│   ├── 01-configmap.yaml
│   ├── 02-deployment.yaml
│   ├── 03-service-http.yaml
│   └── 04-service-https.yaml
├── docs/
│   ├── MANUAL.md
│   ├── ANSIBLE.md
│   ├── VERIFICATION.md
│   └── LEGACY_COMPARISON.md
├── tests/
│   └── validate_project.py
└── report/
    └── KT4_DevSecOps_Report.docx
```

## Быстрый старт (вариант с Ansible)

Предусловия: три чистые виртуальные машины Ubuntu 22.04/24.04 x86_64, одна управляющая машина с Ansible, IP-адреса без пересечения с Pod/Service CIDR, доступ к интернету. В файле inventory указать собственные IP, пользователя SSH и ключ.

```bash
cp inventory/hosts.ini.example inventory/hosts.ini
# Отредактируйте hosts.ini с реальными адресами и логином
ansible -i inventory/hosts.ini k8s -m ping
ansible-playbook playbooks/01_prepare_nodes.yml --ask-become-pass
ansible-playbook playbooks/02_init_control_plane.yml --ask-become-pass
ansible-playbook playbooks/03_join_workers.yml --ask-become-pass
ansible-playbook playbooks/04_deploy_nginx.yml --ask-become-pass
```

Если на узлах уже настроен `sudo` без пароля, `--ask-become-pass` можно опустить. Не запускайте установку на действующем сервере: она изменяет сетевые параметры, таблицу монтирования swap и конфигурацию containerd.

**Обратите внимание:** `inventory/hosts.ini` с реальными адресами исключён из Git, если вы не хотите их публиковать. Заполненный файл хранится только у вас локально.

## Быстрый старт (без Ansible)

В каждой из трёх ВМ:

```bash
sudo bash scripts/prepare-node.sh
```

На управляющей ноде с адресом `192.168.0.10`:

```bash
sudo bash scripts/init-control-plane.sh 192.168.0.10
```

Из вывода скопируйте команду `kubeadm join ...` и выполните её с `sudo` на обоих рабочих узлах. Затем на control plane:

```bash
bash scripts/deploy-nginx.sh
bash scripts/verify.sh
```

Скрипты `deploy-nginx.sh` и `verify.sh` используют текущий `KUBECONFIG` и `kubectl`. Если сертификат кластера ещё не настроен для текущего пользователя, воспользуйтесь руководством в `docs/MANUAL.md`.

## Проверка

```bash
kubectl get nodes -o wide
kubectl -n kt4 get deploy,pods,svc -o wide
kubectl -n kt4 rollout status deployment/nginx --timeout=300s
bash scripts/verify.sh
```

Ожидается: 3 узла в статусе `Ready`, 6 готовых реплик `nginx`, сервисы на 80 и 443, HTTP и HTTPS отвечают в пределах кластера. HTTPS использует самостоятельно подписанный сертификат для учебной работы; в проверке применяется `curl -k`, для реального сервиса нужен сертификат доверенного центра.

**Результат реального запуска на ВМ не заявлен.** Для подтверждения практической части приложите собственные скриншоты по `docs/VERIFICATION.md`.

## Источники

- https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/install-kubeadm/
- https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/create-cluster-kubeadm/
- https://kubernetes.io/docs/setup/production-environment/container-runtimes/
- https://docs.docker.com/engine/install/ubuntu/
- https://github.com/flannel-io/flannel

Дата подготовки: 13.11.2026.
