# Автоматизированная установка с Ansible

Установите Ansible на управляющую машину и проверьте SSH-доступ по ключам. На Ubuntu можно выполнить `sudo apt install ansible -y`. Рекомендуемую версию Ansible можно получить из официального источника.

```bash
cp inventory/hosts.ini.example inventory/hosts.ini
# Укажите настоящие IP и SSH-пользователя.
ansible -i inventory/hosts.ini k8s -m ping

# Подготовка ОС и пакетов на всех трёх узлах
ansible-playbook playbooks/01_prepare_nodes.yml --ask-become-pass

# kubeadm init и установка Flannel на master
ansible-playbook playbooks/02_init_control_plane.yml --ask-become-pass

# Присоединение worker1 и worker2 по временной команде
ansible-playbook playbooks/03_join_workers.yml --ask-become-pass

# Deployment, ConfigMap, Secret, HTTP/HTTPS Services
ansible-playbook playbooks/04_deploy_nginx.yml --ask-become-pass
```

**Безопасность:** реальный `inventory/hosts.ini` не коммитится; токен `kubeadm join` создаётся на время работы и скрыт через `no_log`; закрытый TLS-ключ остаётся на master. `host_key_checking` не выключен.

**Режим использования:** только для чистых учебных ВМ Ubuntu 22.04/24.04. При повторной установке в существующий кластер необходимо отдельно оценить сохранность данных и конфигурации containerd.

**Диагностика:**

```bash
ansible -i inventory/hosts.ini k8s -m command -a 'systemctl is-active containerd' --become
ssh ubuntu@192.168.0.10
kubectl get nodes -o wide
kubectl -n kt4 get deployment,pods,services
```

Если Ansible требует пароль `sudo`, добавьте `--ask-become-pass`. Не сохраняйте пароли в инвентаре.
