# Customer360 — Terraform for GreenNode / VNG Cloud

Provisions the `core-customer360` stack on **GreenNode** (which runs on **VNG
Cloud** infrastructure) using the [`vngcloud/vngcloud`](https://registry.terraform.io/providers/vngcloud/vngcloud/latest/docs)
Terraform provider.

Topology: **managed databases + one app VM** (the option chosen for this
deployment). It mirrors [`../docker-compose.yml`](../docker-compose.yml) and
[`../docs/dockers/docker-compose-architecture.png`](../docs/dockers/docker-compose-architecture.png),
but the stateful containers become managed services.

## What maps to what

| docker-compose service | Cloud resource |
|---|---|
| `postgres` (PostGIS + pgvector) | `vngcloud_vdb_relational_database` — **managed PostgreSQL (RDS)** |
| `redis` | `vngcloud_vdb_memstore_database` — **managed Redis (MemoryStore)** |
| `keycloak-db-init` | `bootstrap.sh` creates `db_keycloak` on the managed PostgreSQL |
| `keycloak` | container on the **app VM** (`vngcloud_vserver_server`) |
| `cir` (identity-resolution worker) | container on the app VM |
| `api` (FastAPI) | container on the app VM |
| `cir-demo-seed` (dev) | not deployed — run manually (see below) |
| network / volumes | `vngcloud_vserver_network` + `_subnet` + `_secgroup`; DB storage is managed |

The app VM's **cloud-init** installs Docker, clones the repo, runs
`00-extensions.sql` + `database-schema.sql` + `02-create-keycloak-db.sql`
against the managed PostgreSQL, then `docker compose up -d` for `api`, `cir`,
`keycloak` ([`files/docker-compose.cloud.yml`](files/docker-compose.cloud.yml)).

## Files

```
versions.tf     provider requirements + backend hint
provider.tf     vngcloud provider (client_id/client_secret)
variables.tf    all inputs (defaults marked VERIFY must be checked)
network.tf      VPC network, subnet, security group + rules
database.tf     managed PostgreSQL + Redis
compute.tf      SSH key, flavor/image/volume lookups, app VM
cloudinit.tf    renders app.env + cloud-init user_data
outputs.tf      endpoints, floating IP, next steps
templates/      env.tftpl, cloud-init.yaml.tftpl
files/          docker-compose.cloud.yml, bootstrap.sh
terraform.tfvars.example
```

## Usage

```bash
cd core-customer360/terraform
cp terraform.tfvars.example terraform.tfvars   # then edit — fill REQUIRED + VERIFY values
terraform init
terraform plan
terraform apply
terraform output next_steps
```

Find flavor/image/package names for the VERIFY values in the GreenNode/VNG
console, or via the provider data sources — the names in
`terraform.tfvars.example` are examples from the provider docs, not guaranteed
to exist in your project/region.

## ⚠️ Caveats — read before applying

1. **PostGIS + pgvector on managed PostgreSQL.** The compose stack uses a
   *custom* image (`postgis/postgis:16` + `postgresql-16-pgvector`); the schema
   needs `CREATE EXTENSION postgis` and `vector`. A managed RDS instance is
   stock PostgreSQL — `bootstrap.sh` attempts those `CREATE EXTENSION`s and
   **fails loudly if the plan doesn't allow them**. Confirm your GreenNode/VNG
   PostgreSQL supports `postgis` and `vector` *before* relying on this. If it
   doesn't, self-host Postgres on a VM with the repo's `postgres/Dockerfile`
   instead of the managed instance (i.e. drop `vngcloud_vdb_relational_database`
   and run a second VM or a container).

2. **`db_keycloak` creation** needs `CREATEDB` on the master user. Usually
   granted; if not, give Keycloak its own managed instance.

3. **Versions/flavors/packages are unverified defaults.** `pg_engine_version`,
   `redis_engine_version`, `vm_flavor_name`, `vm_image_name`, `*_package_name`,
   `*_volume_type` are best-effort — check availability in your project.

4. **API endpoints/region.** Defaults target HCM03. If your GreenNode account
   uses different gateways, override the `vng_*_base_url` + `vng_token_url`
   vars. Get `client_id`/`client_secret` from the console IAM / API-keys page.

5. **Keycloak runs `start-dev` by default.** Fine for testing. For production,
   set `keycloak_command = "start"`, set `keycloak_hostname` to a real
   domain/floating IP, and front it with TLS.

6. **Private app repo.** cloud-init `git clone`s `app_repo_url` from the VM.
   For a private repo, embed a token in the URL or switch to a deploy key.

7. **Provider version is unpinned** (`>= 0.1.0`). After the first `init`, pin
   the resolved version in `versions.tf`.

## Seeding demo data (optional, was `cir-demo-seed`)

```bash
ssh ubuntu@<floating_ip>
cd /opt/customer360/src/core-customer360
docker compose -f docker-compose.cloud.yml run --rm cir \
  sh -c "python scripts/init_sample_data.py && \
         python scripts/run_demo_resolution.py && \
         python scripts/seed_full_demo_data.py"
```

## Notes

- Managed DBs are `public_access = false` and only accept traffic from
  `subnet_cidr`, so they're private to the VPC; the app VM reaches them over
  the subnet.
- State contains DB passwords — use the remote backend hint in `versions.tf`.
