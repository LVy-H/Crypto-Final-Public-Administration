---
description: Runs Unit, API, and E2E tests.
---
# Run All Tests

1. **Backend Unit Tests**
   ```bash
   ./gradlew test
   ```

2. **API Integration Tests**
   ```bash
   python tests/scripts/test_api.py
   ```

3. **E2E Tests**
   ```bash
   cd tests/e2e
   npx playwright test
   ```
