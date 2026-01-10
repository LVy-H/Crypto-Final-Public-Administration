---
description: Perform health checks, security audits, and dependency updates.
---
# Maintenance & Audit Workflow

Run this workflow periodically (e.g., start of a sprint) to ensure project health and security.

## 1. Frontend Health Check (apps/public-portal)
1.  **Check Dependencies**:
    ```bash
    cd apps/public-portal
    npm outdated
    ```
2.  **Security Audit**:
    ```bash
    npm audit
    ```
    - *Action*: If critical vulnerabilities exist, plan an immediate fix.
3.  **Lint Check**:
    ```bash
    npm run lint
    ```

## 2. Backend Health Check (backend/)
1.  **Check Dependencies**:
    ```bash
    ./gradlew dependencyUpdates -Drevision=release
    ```
    *(Note: May fail if versions plugin is not applied. Fallback: manual check of `build.gradle.kts`)*
2.  **Run Checks**:
    ```bash
    ./gradlew check
    ```
    - This runs unit tests and any configured linters (e.g., KtLint, Checkstyle).

## 3. Infrastructure Check
1.  **Docker/Podman**: Prune unused images if disk space is low.
    ```bash
    podman system prune -f
    ```
2.  **K3s**: Check for crashed pods or restart loops.
    ```bash
    kubectl get pods -A | grep -v Running
    ```

## 4. Reporting
- Summarize significant findings in `task.md`.
- Create new tasks for critical upgrades or security fixes.
