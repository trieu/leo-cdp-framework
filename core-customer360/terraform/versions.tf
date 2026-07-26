terraform {
  required_version = ">= 1.3.0"

  required_providers {
    vngcloud = {
      source = "vngcloud/vngcloud"
      # Check https://registry.terraform.io/providers/vngcloud/vngcloud for the
      # latest version and pin it (e.g. version = "~> 0.3"). Left open so the
      # first `terraform init` selects a compatible published release.
      version = ">= 0.1.0"
    }
  }

  # Recommended: keep state OFF laptops (it contains DB passwords). VNG Cloud
  # vStorage is S3-compatible, so the s3 backend works with custom endpoints:
  #
  # backend "s3" {
  #   bucket                      = "customer360-tfstate"
  #   key                         = "core-customer360/terraform.tfstate"
  #   region                      = "hcm03"
  #   endpoints                   = { s3 = "https://hcm03.vstorage.vngcloud.vn" }
  #   skip_credentials_validation = true
  #   skip_region_validation      = true
  #   skip_requesting_account_id  = true
  #   skip_s3_checksum            = true
  #   use_path_style              = true
  # }
}
