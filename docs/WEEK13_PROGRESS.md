# Week 13 Progress: Ansible Provisioning

## Objective

Provision the local environment for the Child Education Sponsorship System with an idempotent Ansible playbook. The controller and target are the same Ubuntu WSL2 environment; this work did not provision a remote server or deploy/recreate the application container.

## Ansible controller and target

- Controller: Ubuntu 24.04.1 LTS under WSL2.
- Python: 3.12.3.
- Ansible Core: 2.16.3.
- Inventory target: `localhost`, configured with `ansible_connection=local` and `/usr/bin/python3`.
- `ansible-inventory -i ansible/inventory.ini --list` succeeded and listed only the `local` group containing `localhost`.
- `ansible-playbook -i ansible/inventory.ini ansible/site.yml --syntax-check` succeeded and identified `ansible/site.yml` as valid.

## Files created

- `ansible/inventory.ini`: local WSL target; no SSH, remote address, or credentials.
- `ansible/site.yml`: validates the expected Ubuntu/Python runtime and ensures the project H2 data directory and a WSL deployment-support directory exist.

## Provisioning tasks and data handling

The playbook verifies Ubuntu 24.04+ and Python 3.12+, ensures the repository's `data/` directory exists for the local H2 database, and ensures `$HOME/.local/share/child-education-sponsorship/ansible-provisioned` exists for local deployment support.

The Spring Boot configuration stores local H2 data under `./data/sponsorshipdb`. The Week 12 container instead mounts Docker's named volume `sponsorship-data` at `/app/data`. This playbook does not remove, recreate, inspect, or modify that Docker volume, and it does not duplicate Jenkins' Docker deployment.

## Provisioning results

Command run both times:

```text
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

First run: **exit 0**. Ubuntu/Python assertion and existing project data directory were `ok`; the dedicated support directory was created.

```text
localhost : ok=4 changed=1 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

Second run, same command: **exit 0**. All tasks were already correct; no task changed anything.

```text
localhost : ok=4 changed=0 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

This demonstrates idempotency: the first run made one directory change, and the second made zero changes. Both runs reported `failed=0`.

## Limitations

The WSL Docker executable path exists, but invoking it from this WSL session returned `Input/output error`; Docker Engine verification through WSL was therefore unavailable. Ansible provisioning did not require Docker and did not attempt to install or configure it. Docker Desktop's existing deployment remains managed by the Week 12 pipeline. No application container or named volume was changed by these Ansible runs.

## Screenshots to capture

1. WSL terminal showing `ansible --version`, `python3 --version`, and Ubuntu/WSL2 details.
2. Explorer or terminal view of `ansible/inventory.ini` and `ansible/site.yml`.
3. Terminal output of `ansible-inventory -i ansible/inventory.ini --list` and the playbook syntax check.
4. First playbook run recap showing `changed=1`, `ok=4`, and `failed=0`.
5. Second identical playbook run recap showing `changed=0`, `ok=4`, and `failed=0`.
6. Terminal listing the provisioned project `data/` directory and WSL deployment-support directory.

## Conclusion

The local Ubuntu WSL2 target is provisioned by an Ansible playbook that passed syntax validation and ran twice successfully. The second run made no changes. This is local WSL provisioning only; Docker deployment remains the responsibility of the existing Week 12 pipeline.