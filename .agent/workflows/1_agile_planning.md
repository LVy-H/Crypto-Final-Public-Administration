---
description: Agile Planning workflow for defining tasks and designing architecture.
---
# Agile Planning Workflow

Use this workflow at the start of a task or when switching contexts.

## 1. Review Context
1.  Read `task.md` to identify the current objective.
2.  Read `README.md` and related `docs/` to understand existing architecture.
3.  (Optional) Run `00_research_discovery.md` if the requirement is unclear.

## 2. Check Constraints
- **Tech Stack**: Pure PQC, Kotlin Backend, Vue 3 Frontend, K3s Infra.
- **Code Quality**: Review `01_maintenance_audit.md` results (if any). Ensure no technical debt blocks this task.

## 3. Draft Implementation Plan
Create or update `implementation_plan.md` with:
- **Goal**: One-sentence summary.
- **Proposed Changes**:
    - Backend: Files to modify/create.
    - Frontend: Components to update.
    - Infra: K8s manifests to change.
- **Verification Plan**:
    - Automated tests to run.
    - Manual scenarios to verify.

## 4. User Review
- Call `notify_user` to present the plan.
- Iterate based on feedback.
- **Do not proceed to Development until the plan is approved.**
