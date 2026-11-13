# Проверка КТ-4 и скриншоты для преподавателя

После выполнения на реальных виртуальных машинах последовательно сохраните скриншоты:

1. `sudo docker --version`, `sudo containerd --version`, `kubeadm version` на любой ноде.
2. `kubectl get nodes -o wide` со статусом `Ready` для master, worker1 и worker2.
3. `kubectl -n kube-flannel get pods` (сеть Flannel запущена).
4. `kubectl -n kt4 get deploy,pods -o wide`: в Deployment `READY 6/6`, показаны 6 Pod и их NODE.
5. `kubectl -n kt4 get services`: два сервиса `nginx-http` (80/TCP) и `nginx-https` (443/TCP).
6. `bash scripts/verify.sh`: HTTP и HTTPS отвечают, в конце строка `PASS`.
7. Дополнительно: браузер или `curl` по портам 8080 и 8443 через `kubectl port-forward`.

Не заменяйте эти доказательства вымышленными изображениями. Сам факт наличия файлов не подтверждает запуск кластера. Кластер проверяется только после создания ВМ и успешного выполнения установочных команд.

## Таблица результатов для заполнения после запуска

| Проверка | Команда | Фактический результат |
|---|---|---|
| Ноды | `kubectl get nodes` | [впишите после запуска] |
| Реплики | `kubectl -n kt4 get deployment nginx` | [впишите после запуска] |
| Сервисы | `kubectl -n kt4 get svc` | [впишите после запуска] |
| HTTP | `curl http://<HTTP_CLUSTER_IP>:80` | [впишите после запуска] |
| HTTPS | `curl -k https://<HTTPS_CLUSTER_IP>:443` | [впишите после запуска] |

Все фотографии должны относиться к вашему действительному кластеру, а не к демонстрационному выводу из исходной статьи.
