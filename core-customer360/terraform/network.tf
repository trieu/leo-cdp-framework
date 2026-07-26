# VPC network + subnet + security group for the app VM.
# The managed databases (see database.tf) are placed in the same subnet and
# only accept traffic from `subnet_cidr`, so they stay private to this VPC.

resource "vngcloud_vserver_network" "main" {
  project_id = var.project_id
  name       = "${var.name_prefix}-net"
  cidr       = var.network_cidr
  zone_id    = var.zone_id
}

resource "vngcloud_vserver_subnet" "main" {
  project_id = var.project_id
  name       = "${var.name_prefix}-subnet"
  cidr       = var.subnet_cidr
  network_id = vngcloud_vserver_network.main.id
  zone_id    = var.zone_id
}

resource "vngcloud_vserver_secgroup" "app" {
  project_id  = var.project_id
  name        = "${var.name_prefix}-app-sg"
  description = "Customer360 app VM (api, cir, keycloak)"
}

# ----- Ingress ---------------------------------------------------------------
resource "vngcloud_vserver_secgrouprule" "ssh" {
  project_id        = var.project_id
  security_group_id = vngcloud_vserver_secgroup.app.id
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "TCP"
  port_range_min    = 22
  port_range_max    = 22
  remote_ip_prefix  = var.allowed_ssh_cidr
  description       = "SSH"
}

resource "vngcloud_vserver_secgrouprule" "api" {
  project_id        = var.project_id
  security_group_id = vngcloud_vserver_secgroup.app.id
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "TCP"
  port_range_min    = 8000
  port_range_max    = 8000
  remote_ip_prefix  = var.allowed_api_cidr
  description       = "Customer360 API"
}

resource "vngcloud_vserver_secgrouprule" "keycloak" {
  project_id        = var.project_id
  security_group_id = vngcloud_vserver_secgroup.app.id
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "TCP"
  port_range_min    = 8080
  port_range_max    = 8080
  remote_ip_prefix  = var.allowed_keycloak_cidr
  description       = "Keycloak SSO"
}

# ----- Egress (allow all — needed for apt/git/docker pulls + managed DBs) ----
resource "vngcloud_vserver_secgrouprule" "egress_tcp" {
  project_id        = var.project_id
  security_group_id = vngcloud_vserver_secgroup.app.id
  direction         = "egress"
  ethertype         = "IPv4"
  protocol          = "TCP"
  port_range_min    = 1
  port_range_max    = 65535
  remote_ip_prefix  = "0.0.0.0/0"
  description       = "All outbound TCP"
}

resource "vngcloud_vserver_secgrouprule" "egress_udp" {
  project_id        = var.project_id
  security_group_id = vngcloud_vserver_secgroup.app.id
  direction         = "egress"
  ethertype         = "IPv4"
  protocol          = "UDP"
  port_range_min    = 1
  port_range_max    = 65535
  remote_ip_prefix  = "0.0.0.0/0"
  description       = "All outbound UDP (DNS etc.)"
}
