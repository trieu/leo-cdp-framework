# VNG Cloud provider. GreenNode (dashboard.console.greennode.ai) runs on VNG
# Cloud infrastructure, so its resources are managed with this provider.
#
# Auth: create an API credential (client_id / client_secret) in the IAM / API
# keys section of the console. client_id/client_secret can also come from the
# CLIENT_ID / CLIENT_SECRET environment variables instead of tfvars.
provider "vngcloud" {
  client_id     = var.vng_client_id
  client_secret = var.vng_client_secret

  token_url        = var.vng_token_url
  vserver_base_url = var.vng_vserver_base_url
  vlb_base_url     = var.vng_vlb_base_url
  vdb_base_url     = var.vng_vdb_base_url
}
