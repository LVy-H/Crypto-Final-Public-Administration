# Backend Security Implementation

## Overview

This document describes the RBAC (Role-Based Access Control) and ABAC (Attribute-Based Access Control) implementation in the GovTech PQC Digital Signature System backend.

## Security Architecture

### Authentication
- **Session-based** authentication using Spring Session with Redis
- Session ID stored in `JSESSIONID` cookie
- BCrypt password hashing
- Automatic session management

### Authorization
Two-level authorization:
1. **RBAC**: Role-Based Access Control at endpoint level
2. **ABAC**: Attribute-Based Access Control at resource level

## Roles

| Role | Description | Access Level |
|------|-------------|--------------|
| `USER` | Default role for citizens | Can submit CSRs, sign documents, verify signatures |
| `OFFICER` | Government officers | Can approve KYC, access private documents, countersign |
| `CA_OPERATOR` | Certificate Authority operators | Can manage CSR queue, sign certificates |
| `ADMIN` | System administrators | Full system access, user management |

## RBAC Implementation

### Identity Service

**SecurityConfig** (`identity-service/config/SecurityConfig.kt`):
- `/auth/register`, `/auth/login`, `/auth/logout` - Public
- `/admin/**` - Requires `ADMIN` or `OFFICER` role
- `/actuator/health` - Public
- `/actuator/**` - Requires `ADMIN` role
- All other endpoints - Requires authentication

**AdminController** annotations:
```kotlin
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
fun approveKyc(...)
```

### API Gateway

**SecurityConfig** (`api-gateway/config/SecurityConfig.kt`):
- `/api/v1/auth/register`, `/api/v1/auth/login` - Public
- `/api/v1/admin/**` - Requires `ADMIN` or `OFFICER` role
- `/api/v1/pki/admin/**` - Requires `CA_OPERATOR` or `ADMIN` role
- `/api/v1/pki/ca/certificate` - Public (CA cert download)
- `/api/v1/tsa/**` - Public (timestamping)
- `/api/v1/documents/verify-asic` - Public (verification)
- `/api/v1/documents/**` - Requires authentication (ABAC at service level)
- `/actuator/health` - Public
- `/actuator/**` - Requires `ADMIN` role

### PKI Service

**AdminController** annotations:
```kotlin
@PreAuthorize("hasAnyRole('CA_OPERATOR', 'ADMIN')")
fun getPendingCsrs(...)
```

Protects:
- CSR queue management
- Certificate upload
- CSR rejection

## ABAC Implementation

### Document Service

**DocumentAccessControl** (`document-service/security/DocumentAccessControl.kt`):

#### Read Access Rules
```
PRIVATE documents:
  - Owner can read
  - Assigned officer can read
  - Admin can read

PUBLIC documents:
  - Any authenticated user can read
```

#### Write Access Rules
```
- Only document owner can modify/delete
- Exception: Admins can override
```

#### Countersign Access Rules
```
- If assignedCountersignerId is set:
  - Only that specific user can countersign
  - OR admin can override
- If no assignedCountersignerId:
  - Any OFFICER or ADMIN can countersign
```

### Usage Example

```kotlin
@RestController
class DocumentController(
    private val accessControl: DocumentAccessControl
) {
    
    @PostMapping("/documents/{id}/finalize")
    fun finalizeDocument(@PathVariable id: Long) {
        // Check ABAC policy
        if (!accessControl.canWrite(id)) {
            throw AccessDeniedException("Not authorized to modify this document")
        }
        
        // Process request
        // ...
    }
}
```

## Security Features

### 1. Method-Level Security

Enabled with `@EnableMethodSecurity(prePostEnabled = true)`:

```kotlin
@PreAuthorize("hasRole('ADMIN')")
fun deleteUser(userId: Long)

@PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
fun approveKyc(username: String)

@PreAuthorize("@documentAccessControl.canWrite(#docId)")
fun updateDocument(docId: Long, ...)
```

### 2. Session Management

- Sessions stored in Redis for scalability
- Automatic timeout after inactivity
- Secure cookie flags in production
- Session fixation protection

### 3. CSRF Protection

- Disabled for API-only services (using session cookies)
- Should be enabled for services with HTML forms
- Use CSRF tokens in production for state-changing operations

### 4. Password Security

- BCrypt hashing with automatic salt
- Password strength validation (min 8 characters)
- Password history (prevent reuse) - TODO
- Account lockout after failed attempts - TODO

## Security Testing

### Test RBAC

```bash
# Try accessing admin endpoint as USER (should fail)
curl -X POST http://localhost:8081/admin/approve-kyc \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=$USER_SESSION" \
  -d '{"username": "test", "action": "APPROVE"}'
# Expected: 403 Forbidden

# Try accessing admin endpoint as ADMIN (should succeed)
curl -X POST http://localhost:8081/admin/approve-kyc \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=$ADMIN_SESSION" \
  -d '{"username": "test", "action": "APPROVE"}'
# Expected: 200 OK
```

### Test ABAC

```bash
# Try accessing another user's document (should fail)
curl -X GET http://localhost:8083/documents/123 \
  -H "Cookie: JSESSIONID=$USER1_SESSION"
# Expected: 403 Forbidden if not owner

# Try accessing own document (should succeed)
curl -X GET http://localhost:8083/documents/123 \
  -H "Cookie: JSESSIONID=$OWNER_SESSION"
# Expected: 200 OK
```

## Security Compliance

### Decree 23/2025/ND-CP Requirements

| Requirement | Implementation | Status |
|-------------|----------------|--------|
| **Sole Control** | Keys stored client-side only | ✅ Compliant |
| **Role Separation** | Officers cannot sign for users | ✅ Compliant |
| **Access Control** | RBAC + ABAC implementation | ✅ Compliant |
| **Audit Logging** | TODO: Add audit trail | ⚠️ Pending |

### Circular 15/2025/TT-BKHCN Requirements

| Requirement | Implementation | Status |
|-------------|----------------|--------|
| **Authentication** | Session-based auth | ✅ Compliant |
| **Authorization** | Role and attribute based | ✅ Compliant |
| **Encryption** | TLS for transport | ✅ Compliant |
| **Key Management** | Client-side PQC keys | ✅ Compliant |

## Future Enhancements

### Phase 2
- [ ] Fine-grained ABAC with policy engine (OPA/Cedar)
- [ ] Audit logging for all security events
- [ ] Rate limiting per user/IP
- [ ] Account lockout after failed login attempts
- [ ] Password complexity requirements
- [ ] MFA/2FA support
- [ ] Document visibility field (PRIVATE/PUBLIC/INTERNAL)
- [ ] Assigned officer field for documents

### Phase 3
- [ ] Dynamic role assignment
- [ ] Delegation capabilities
- [ ] Time-based access (temporal policies)
- [ ] Geo-fencing (location-based access)
- [ ] Risk-based authentication
- [ ] Behavioral analytics

## Configuration

### Development
```yaml
# application-dev.yml
spring:
  security:
    debug: true  # Enable security debugging
```

### Production
```yaml
# application-prod.yml
spring:
  security:
    debug: false
  session:
    redis:
      namespace: "spring:session:prod"
    cookie:
      secure: true  # HTTPS only
      http-only: true
      same-site: strict
```

## Troubleshooting

### Common Issues

**403 Forbidden when accessing endpoint:**
1. Check if user has required role
2. Verify session cookie is valid
3. Check SecurityConfig path matchers
4. Enable debug logging: `logging.level.org.springframework.security=DEBUG`

**401 Unauthorized:**
1. Session expired - re-login
2. Invalid session cookie
3. Missing JSESSIONID in request

**Session not persisting:**
1. Check Redis connection
2. Verify Spring Session configuration
3. Check cookie domain/path settings

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [OWASP Access Control Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)
- [NIST RBAC](https://csrc.nist.gov/projects/role-based-access-control)
