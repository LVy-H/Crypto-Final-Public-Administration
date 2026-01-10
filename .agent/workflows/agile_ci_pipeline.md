---
description: Comprehensive Agile CI Pipeline (Audit -> Plan -> Dev -> Test -> Deploy)
---

# Agile CI Pipeline

This workflow acts as the **Master Pipeline**, orchestrating the individual Agile phase workflows into a complete Continuous Integration / Continuous Delivery (CI/CD) cycle.

## 🔸 Phase 1: Health & Maintenance
Perform a health check and audit before starting work.
- [ ] Run **Maintenance Audit**:
  - Command: `/01_maintenance_audit`
  - *Goal*: Ensure dependencies are secure and linting passes.

## 🔸 Phase 2: Planning & Design
Define the work to be done.
- [ ] Run **Agile Planning**:
  - Command: `/1_agile_planning`
  - *Goal*: Create/Update `implementation_plan.md` and `task.md`.

## 🔸 Phase 3: Implementation (Development)
Execute the changes.
- [ ] Run **Agile Development**:
  - Command: `/2_agile_development`
  - *Goal*: Write code, pass Unit Tests and Lint checks.
  - *Key Action*: `concrete_build_all` and `concrete_lint_check` are run here.

## 🔸 Phase 4: Verification (Testing)
Validating the changes.
- [ ] Run **Agile Testing**:
  - Command: `/3_agile_testing`
  - *Goal*: Run Regression Suite (`concrete_run_tests`) and Manual Verifications.

## 🔸 Phase 5: Deployment
Deploying to the local environment.
- [ ] Run **Deploy Local**:
  - Command: `/concrete_deploy_local`
  - *Goal*: Deploy fresh Docker images to K3s cluster.

## 🔴 Fallback: Debugging
If any phase fails:
- [ ] Run **Debug Investigation**:
  - Command: `/4_debug_investigation`
  - *Goal*: Analyze logs and fix root causes.
