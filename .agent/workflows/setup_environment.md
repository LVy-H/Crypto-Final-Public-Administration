---
description: Initial setup for the development environment.
---
# Setup Environment

1. **Install Languages**
   - Java 21+
   - Node 22+
   - Python 3.12+

2. **Install Tools**
   - Podman (or Docker)
   - K3s (or Kind)
   - Kubectl

3. **Setup PKI**
   ```bash
   ./scripts/setup_pki.sh
   ```

4. **Install Dependencies**
   ```bash
   cd apps/public-portal && npm install
   ```
   ```bash
   cd tests/e2e && npm install
   ```
