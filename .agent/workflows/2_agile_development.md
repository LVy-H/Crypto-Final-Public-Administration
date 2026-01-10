---
description: Development workflow for implementing features and fixes (Code -> Format -> Build -> Unit Test).
---
# Agile Development Workflow

Execute this workflow during the "Execution" phase of a task.

## 1. Implementation
- Write code according to `implementation_plan.md`.
- **Rule**: Keep changes atomic. Do not mix unrelated refactors.

## 2. Code Quality (Lint & Format)
Before building, ensure code style compliance:

### Frontend
```bash
cd apps/public-portal
npm run format
npm run lint
```
// turbo
### Backend
```bash
# Formats code if KtLint/Spotless is configured
./gradlew formatKotlin 2>/dev/null || echo "Skipping auto-format"
```

## 3. Build & Compile
Run the build workflow to ensure no compilation errors.
- Use `.agent/workflows/concrete_build_all.md`

## 4. Local Unit Tests
Run fast, local tests for the specific component you modified.
- **Backend**: `./gradlew :<service-name>:test`
- **Frontend**: `npm run test:unit` (if available)

## 5. Local Deploy (Optional)
If the change affects API or Runtime behavior:
- Use `.agent/workflows/concrete_deploy_local.md`
