#!/usr/bin/env python3
"""Structural check of Kubernetes and Ansible YAML (requires PyYAML)."""
from pathlib import Path
import yaml

root = Path(__file__).resolve().parents[1]
manifest_paths = sorted((root / 'k8s').glob('*.yaml'))
playbook_paths = sorted((root / 'playbooks').glob('*.yml'))
for p in manifest_paths + playbook_paths:
    list(yaml.safe_load_all(p.read_text(encoding='utf-8')))

resources = [yaml.safe_load(p.read_text(encoding='utf-8')) for p in manifest_paths]
bykind = {(r['kind'], r['metadata']['name']): r for r in resources}
assert bykind[('Deployment', 'nginx')]['spec']['replicas'] == 6
assert bykind[('Service', 'nginx-http')]['spec']['ports'][0]['port'] == 80
assert bykind[('Service', 'nginx-https')]['spec']['ports'][0]['port'] == 443
assert bykind[('Service', 'nginx-http')]['spec']['selector'] == {'app': 'nginx-kt4'}
assert bykind[('Service', 'nginx-https')]['spec']['selector'] == {'app': 'nginx-kt4'}
assert bykind[('Deployment', 'nginx')]['spec']['template']['spec']['volumes'][1]['secret']['secretName'] == 'nginx-tls'
assert bykind[('ConfigMap', 'nginx-config')]['metadata']['namespace'] == 'kt4'
assert len(playbook_paths) == 4
print(f'PASS: {len(manifest_paths)} Kubernetes manifests, {len(playbook_paths)} Ansible playbooks; replicas=6, ports=80/443, TLS Secret mounted')
