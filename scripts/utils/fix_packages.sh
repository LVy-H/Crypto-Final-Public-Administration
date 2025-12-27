#!/bin/bash
set -e

# Services and their corresponding subpackage names
declare -A services=( 
    ["cloud-sign"]="cloudsign" 
    ["ca-authority"]="caauthority" 
    ["identity-service"]="identityservice"
    ["ra-service"]="raservice"
    ["validation-service"]="validationservice"
    ["signature-core"]="signaturecore"
    ["api-gateway"]="apigateway"
)

for service in "${!services[@]}"; do
    subpackage=${services[$service]}
    base_dir="services/$service/src/main/java/com/gov/crypto"
    target_dir="$base_dir/$subpackage"
    
    echo "Processing $service -> $subpackage"
    
    # 1. Create target subpackage directory
    mkdir -p "$target_dir"
    
    # 2. Move files from base_dir to target_dir (avoiding moving target_dir into itself)
    # We find items in base_dir, exclude target_dir, and move them.
    find "$base_dir" -maxdepth 1 -mindepth 1 ! -name "$subpackage" -exec mv {} "$target_dir/" \;
    
    # 3. Fix Content
    # Replace old package usage
    find "$target_dir" -type f -name "*.java" -print0 | xargs -0 sed -i 's/package com.public.admin/package com.gov.crypto/g'
    find "$target_dir" -type f -name "*.java" -print0 | xargs -0 sed -i 's/import com.public.admin/import com.gov.crypto/g'
    
    # Also standardize the subpackage part if it was lost or malformed
    # We want 'package com.gov.crypto.cloudsign;' 
    # Existing might be 'package com.gov.crypto.cloudsign.service;' which is fine.
    # But files in root of target_dir might need explicit check.
done

echo "Fix completed."
