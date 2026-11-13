# Сопоставление с материалом преподавателя

Исходный текст описывает пример с Kubernetes `v0.17.0` и историческими утилитами 2015 года. Дата обновления страницы сама по себе не означает, что старые команды актуальны.

| В исходном задании | В выполненном проекте | Почему |
|---|---|---|
| `wget ... v0.17.0/kubernetes.tar.gz` | Пакеты ветки `v1.37` из `pkgs.k8s.io` | Исходная версия давно не поддерживается |
| `KUBERNETES_PROVIDER=ubuntu ./kube-up.sh` | `kubeadm init` / `kubeadm join` | Современный стандарт для собственной ВМ |
| Docker как среда для Kubernetes | Docker Engine + `containerd` как CRI | Docker Engine установлен по заданию, но Kubernetes использует containerd |
| `minion`, `ReplicationController` | `Node`, `Deployment` | Актуальные объекты Kubernetes |
| `run-container nginx --replicas=6` | `Deployment.spec.replicas: 6` | Шесть одинаковых Pod под управлением Deployment |
| `expose rc nginx --port=80` | `Service nginx-http:80` | Эквивалент публикации HTTP внутри кластера |
| `expose rc nginx --port=443` | `Service nginx-https:443` + TLS Secret | HTTPS действительно шифруется, а не только имеет порт 443 |
| `192.168.3.0/24` для сервисов | `10.96.0.0/12` | Предлагаемый учебный Service CIDR, отделённый от Pod CIDR |
| `172.16.0.0/16` для Flannel | `10.244.0.0/16` | Настройка в совместимости с поставляемым Flannel manifest |

Конечная учебная задача сохранена: три узла, установка Docker и настройка сети, Nginx в шести репликах, два сервиса, проверка HTTP и HTTPS.
