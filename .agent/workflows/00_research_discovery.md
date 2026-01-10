---
description: Deep dive research into documentation, GitHub, and web resources to solve complex problems.
---
# Research & Discovery Workflow

This workflow is designed for investigating complex topics, finding library usage examples, or understanding new requirements.

## 1. Define Goal
1.  Identify the core question or problem (e.g., "How to implement client-side ML-DSA signing?").
2.  Determine the output format (e.g., update `task.md`, create `docs/research/TOPIC.md`, or just mental context).

## 2. Web Search
Use `search_web` to find high-level documentation, specifications, and discussions.
- **Query Types**:
    - Official Docs: "OpenQuantumSafe liboqs-js documentation"
    - Standards: "NIST FIPS 204 ML-DSA specification"
    - Discussions: "kotlin spring boot 3.2 virtual threads issues"

## 3. GitHub Search ("Real Case Reader")
Use `search_repositories` and `search_code` to see how others have solved similar problems.
- **Search Repos**: Find relevant libraries or example projects.
- **Search Code**: Find specific usage patterns of APIs.
    - Example: `filename:pqc.ts "ML-DSA-44"` to see how others configure it.
    - Example: `language:kotlin "BouncyCastleProvider"` to see provider registration.

## 4. Documentation Reading
Use `read_url_content` to ingest specific documentation pages found in previous steps.
- **Focus**: Key API references, configuration guides, and migration notes.

## 5. Synthesize & Plan
1.  Summarize findings.
2.  If the research changes the current approach, update `implementation_plan.md`.
3.  If it clarifies a bug, move to `4_debug_investigation.md`.
