# Tests Directory

This directory contains all test scripts and utilities for the GovTech PQC Platform.

## Structure

```
tests/
├── scripts/           # Python CLI and testing scripts
│   ├── pqc_cli.py     # Full-featured CLI client (click framework)
│   └── test_tamper.py # Tamper detection verification test
├── api/               # API endpoint tests (pytest)
├── unit/              # Unit tests for individual components
├── integration/       # Integration tests across services
├── utils/             # Shared test utilities and helpers
├── conftest.py        # Pytest fixtures and configuration
├── pytest.ini         # Pytest settings
└── requirements-test.txt  # Test dependencies
```

## Quick Start

### Run Python CLI Demo
```bash
nix-shell -p python3Packages.{requests,cryptography,click} --run "python tests/scripts/pqc_cli.py demo"
```

### Run Tamper Detection Test
```bash
nix-shell -p python3Packages.{requests,cryptography} --run "python tests/scripts/test_tamper.py"
```

### Run Pytest Suite
```bash
cd tests && pip install -r requirements-test.txt && pytest
```

## CLI Usage

The `pqc_cli.py` provides a complete CLI for platform interaction:

```bash
# Authentication
pqc_cli.py auth register <user> <pass>
pqc_cli.py auth login <user> <pass>

# Document Operations
pqc_cli.py doc upload <file>
pqc_cli.py doc sign <doc-id> --signer "Name"
pqc_cli.py doc countersign <asic-file> --signer "Name"
pqc_cli.py doc verify <asic-file>

# Admin
pqc_cli.py admin approve-kyc <username>

# Full Demo
pqc_cli.py demo
```

## Test Categories

| Category | Purpose | Location |
|----------|---------|----------|
| **Scripts** | CLI and manual testing | `scripts/` |
| **API** | REST endpoint validation | `api/` |
| **Unit** | Component isolation tests | `unit/` |
| **Integration** | Cross-service workflows | `integration/` |
