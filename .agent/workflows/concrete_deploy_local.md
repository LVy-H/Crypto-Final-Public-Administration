---
description: Deploys the system to the local K3s cluster.
---
# Deploy Local

// turbo
1. **Run Deployment Script**
   ```bash
   ./scripts/deploy_k3s.sh
   ```

2. **Wait for Rollout**
   ```bash
   kubectl rollout status deployment/api-gateway -n crypto-backend --timeout=120s
   ```
