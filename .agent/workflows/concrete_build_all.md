---
description: Builds the entire project (Backend + Frontend).
---
# Build All

// turbo
1. **Backend Build**
   ```bash
   ./gradlew clean build -x test
   ```

2. **Frontend Build**
   ```bash
   cd apps/public-portal
   npm install
   npm run build
   ```
