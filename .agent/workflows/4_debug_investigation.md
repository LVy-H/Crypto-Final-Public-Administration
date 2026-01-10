---
description: Systematic process for investigating and fixing bugs.
---
# Debug Investigation Workflow

Use this workflow when a test fails or a bug is reported.

## 1. Analyze Symptoms
1.  **Read Logs**:
    ```bash
    kubectl logs -n crypto-backend -l app=<service-name> --tail=100
    ```
2.  **Check Pod Status**:
    ```bash
    kubectl get pods -n crypto-backend
    kubectl describe pod <failed-pod> -n crypto-backend
    ```

## 2. Reproduce
1.  Create a minimal reproduction script (e.g., a specific Python test case in `tests/scripts/repro_issue.py`).
2.  Confirm the script fails reliably.

## 3. Root Cause Analysis
- Use `00_research_discovery.md` if the error message is obscure.
- Add debug logs to the code if needed.
- Build and Deploy debug version.

## 4. Fix & Verify
1.  Apply the fix (Back to `2_agile_development.md`).
2.  Run the reproduction script again (Must Pass).
3.  Run the full regression suite (`3_agile_testing.md`).
