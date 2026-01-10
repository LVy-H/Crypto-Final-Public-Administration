---
description: QA and Verification workflow (Regression Tests + Manual Validation).
---
# Agile Testing Workflow

Execute this workflow after Development is complete, before marking the task as Done.

## 1. Automated Regression Suite
Run the full test suite to catch regressions.
- Use `.agent/workflows/concrete_run_tests.md`
- **Must Pass**: All Unit Tests, API Integration Tests.
- **Should Pass**: E2E Tests (unless UI flow changed significantly).

## 2. Manual Verification
Perform the "Manual Verification" steps from `implementation_plan.md`.
- **UI Checks**: Open `http://localhost:3000` (or appropriate port).
- **API Checks**: Use `curl` or Postman to verify endpoints.

## 3. Documentation (Proof of Work)
Create or update `walkthrough.md`.
- **Screenshots**: Capture UI states (Success/Error).
- **Logs**: Capture relevant backend logs showing successful processing.
- **Instructions**: How to reproduce the verification steps.

## 4. Acceptance
- If all checks pass: Mark task as `[x]` in `task.md`.
- If issues found: Switch to `4_debug_investigation.md`.
