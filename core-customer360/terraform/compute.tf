# App VM (vServer) that runs the stateless containers: api, cir, keycloak.
# Flavor / image / volume-type are resolved BY NAME via data sources so the
# config stays readable — you supply human names, Terraform looks up the UUIDs.

data "vngcloud_vserver_flavor_zone" "main" {
  project_id = var.project_id
  name       = var.flavor_zone_name
}

data "vngcloud_vserver_flavor" "app" {
  project_id     = var.project_id
  name           = var.vm_flavor_name
  flavor_zone_id = data.vngcloud_vserver_flavor_zone.main.id
}

data "vngcloud_vserver_image" "app" {
  project_id     = var.project_id
  name           = var.vm_image_name
  flavor_zone_id = data.vngcloud_vserver_flavor_zone.main.id
}

data "vngcloud_vserver_volume_type_zone" "main" {
  project_id = var.project_id
  name       = var.volume_type_zone_name
}

data "vngcloud_vserver_volume_type" "root" {
  project_id          = var.project_id
  name                = var.root_volume_type_name
  volume_type_zone_id = data.vngcloud_vserver_volume_type_zone.main.id
}

resource "vngcloud_vserver_sshkey" "app" {
  project_id = var.project_id
  name       = "${var.name_prefix}-key"
  public_key = var.ssh_public_key
}

resource "vngcloud_vserver_server" "app" {
  project_id = var.project_id
  name       = "${var.name_prefix}-app"

  flavor_id         = data.vngcloud_vserver_flavor.app.id
  image_id          = data.vngcloud_vserver_image.app.id
  network_id        = vngcloud_vserver_network.main.id
  subnet_id         = vngcloud_vserver_subnet.main.id
  root_disk_size    = var.vm_root_disk_size
  root_disk_type_id = data.vngcloud_vserver_volume_type.root.id
  encryption_volume = false

  ssh_key        = vngcloud_vserver_sshkey.app.id
  security_group = [vngcloud_vserver_secgroup.app.id]

  # Public IP so the VM can pull packages/images and serve the API/Keycloak.
  attach_floating = true

  zone_id = var.zone_id

  # cloud-init: installs Docker, initialises the managed DB (extensions +
  # schema + db_keycloak), then brings up api/cir/keycloak. Rendered in
  # cloudinit.tf; references the managed DB endpoints, which forces the VM to
  # be created after both databases exist.
  user_data = local.user_data
}
