# Secure Library Management API

Secure mini web application project for the **Secure Software Development** module.
This repository implements a REST API for a library system with security controls integrated into the codebase.

## Project Scope

### Functional features
- User registration and login
- Two roles: `USER` and `LIBRARIAN`
- List books and search by title or category
- User borrow request flow
- Librarian add/update/delete books
- Librarian approve/reject borrow requests

### Security features implemented
- BCrypt password hashing
- JWT authentication and role-based authorization
- Input validation on request body and key path/query parameters
- Global exception handling with sanitized API error responses
- Custom `401/403` handlers for consistent REST security responses
- No hardcoded JWT/bootstrap token in source code (environment-based config)
- Security logging for auth and librarian operations
- Safe bootstrap mechanism for first librarian account

## Security Design Summary

Security was considered throughout the project lifecycle rather than added at the end. The application combines:

- Strong password storage with BCrypt
- Stateless authentication with JWT
- Role-based authorization for protected routes
- Request validation at API boundaries
- Sanitized exception handling
- Security event logging
- Static and dynamic security testing
- CI/CD security gates in GitHub Actions

The design goal was to keep the application simple, clear, and secure by default.

## Threat Model (STRIDE)

The application threat model was built using the STRIDE methodology to identify realistic threats early in the design phase and map them to concrete mitigations.

| Threat | Example in this project | Main mitigation |
|---|---|---|
| Spoofing | Attacker attempts to impersonate a valid user with fake or stolen credentials/token | JWT signature validation, token expiration, BCrypt password hashing |
| Tampering | Malicious modification of request payloads or identifiers | Input validation, server-side checks, deny-by-default authorization |
| Repudiation | User denies performing a borrow or admin action | Security logging for authentication and librarian operations |
| Information Disclosure | Sensitive fields or internal errors exposed in API responses | Sanitized error handling, `@JsonIgnore` on sensitive fields, role-based access control |
| Denial of Service | Excessive requests against login or borrow endpoints | Controlled API exposure, secure defaults, future rate-limiting in the hardening roadmap |
| Elevation of Privilege | Normal user attempts to access librarian-only endpoints | Spring Security RBAC, `@PreAuthorize`, protected admin routes |

This threat model guided secure coding decisions and helped prioritize the most important controls before testing. The STRIDE analysis was also used as the basis for the formal threat modeling section in the accompanying security report deliverable.

## Security Artifacts

- DFD source: `docs/dfd-current-code.mmd`
- DFD image: `docs/dfd-current-code.png`
- Report source (LaTeX): `security_report.tex`

## Tech Stack
- Java 17+
- Spring Boot 3
- Spring Security
- Spring Data JPA
- H2 Database
- Maven
- OpenAPI (Swagger UI)

## Run Locally

### Prerequisites
- JDK 17 or newer
- Maven (or use `./mvnw`)
- SonarCloud token (optional for local Sonar run)
- `uv`/`uvx` (optional, for supplementary local Semgrep execution)
- Docker (for local OWASP ZAP baseline scan)

### Environment variables
- `JWT_SECRET` (recommended in production)
- `JWT_EXPIRATION` (optional, default `86400000`)
- `LIBRARIAN_BOOTSTRAP_TOKEN` (needed for first librarian bootstrap)
- `DB_USERNAME` (optional, default `sa`)
- `DB_PASSWORD` (optional, default empty)
- `H2_CONSOLE_ENABLED` (optional, default `false`)
- `SONAR_TOKEN` (optional, only needed if running SonarCloud scan locally)

Example environment configuration is provided in `.env.example` for safe local setup without exposing secrets.
Environment configuration template:
JWT_SECRET=change_me
JWT_EXPIRATION=86400000
LIBRARIAN_BOOTSTRAP_TOKEN=change_me
DB_USERNAME=sa
DB_PASSWORD=
H2_CONSOLE_ENABLED=false

### Start the app

```bash
cd /Users/dtbao4597/esilv/secure-sofware-dev/bookwebpage-secure-software-development
export JWT_SECRET="$(openssl rand -base64 32)"
export LIBRARIAN_BOOTSTRAP_TOKEN="$(openssl rand -base64 32)"
./mvnw spring-boot:run
```

Swagger UI:
- `http://127.0.0.1:8080/swagger-ui/index.html`

## Accounts and Roles

No hardcoded default accounts are shipped.

### Create first librarian (one-time bootstrap)

```bash
curl -X POST http://127.0.0.1:8080/api/auth/register-librarian \
  -H "Content-Type: application/json" \
  -H "X-Setup-Token: $LIBRARIAN_BOOTSTRAP_TOKEN" \
  -d '{"username":"admin1","email":"admin1@lib.com","password":"StrongPass123"}'
```

### Create normal user

```bash
curl -X POST http://127.0.0.1:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","email":"user1@lib.com","password":"StrongPass123"}'
```

### Create additional librarians
After login as a librarian, call:
- `POST /api/auth/create-librarian` with `Authorization: Bearer <token>`

## Key API Endpoints

### Auth
- `POST /api/auth/register`
- `POST /api/auth/register-librarian`
- `POST /api/auth/create-librarian` (librarian only)
- `POST /api/auth/login`
- `GET /api/auth/me`

### Books
- `GET /api/books`
- `GET /api/books/{id}`
- `GET /api/books/search?title=...&category=...`
- `POST /api/books/add` (librarian)
- `PUT /api/books/update/{id}` (librarian)
- `DELETE /api/books/delete/{id}` (librarian)

### Borrow
- `POST /api/borrow/request/{bookId}`
- `GET /api/borrow/my-requests`
- `GET /api/borrow/pending` (librarian)
- `PUT /api/borrow/approve/{requestId}` (librarian)
- `PUT /api/borrow/reject/{requestId}` (librarian)

## Test and Build

```bash
./mvnw -q -DskipTests compile
./mvnw -q test
```

## Security Testing (SAST/DAST)

### SAST strategy
- Primary SAST: **SonarCloud** (enforced in CI via `.github/workflows/sast.yaml`)
- Supplementary local SAST: **Semgrep** (fast pre-check before push)

### SAST (SonarCloud) - CI required
SonarCloud is executed in CI and is used as the required SAST gate (`sonar.qualitygate.wait=true`).

### SAST (SonarCloud) - local run (optional)

```bash
cd /Users/dtbao4597/esilv/secure-sofware-dev/bookwebpage-secure-software-development
./mvnw sonar:sonar \
  -Dsonar.projectKey=gandhidev1113-glitch_bookwebpage-secure-software-development \
  -Dsonar.organization=gandhidev1113-glitch \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login="$SONAR_TOKEN"
```

### SAST (Semgrep) - supplementary local run

```bash
cd /Users/dtbao4597/esilv/secure-sofware-dev/bookwebpage-secure-software-development
uvx semgrep scan \
  --config p/owasp-top-ten \
  --config p/java \
  --config p/security-audit \
  --exclude zap-report.html \
  --exclude target \
  --exclude "*.db" \
  --json --output semgrep-report.json \
  src
```

### DAST (OWASP ZAP Baseline) - local run

```bash
cd /Users/dtbao4597/esilv/secure-sofware-dev/bookwebpage-secure-software-development
docker run --rm \
  -v "${PWD}:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py \
  -t http://host.docker.internal:8080 \
  -r zap-report.html \
  -J zap-report.json \
  -m 2
```

### Latest executed results (March 29, 2026)

### Security Testing Summary
Static (SAST) and dynamic (DAST) analysis were executed after implementing authentication, authorization, and validation controls to verify that no critical vulnerabilities remained in the application.

- SAST (primary, CI): SonarCloud quality gate is the required SAST gate in workflow
- SAST (supplementary, local Semgrep): `0 findings` (`semgrep-report.json`, `semgrep-after-custom.json`)
- DAST (OWASP ZAP Baseline): `1 informational alert`, `0 Low`, `0 Medium`, `0 High`
- Current remaining DAST alert context:
  - `Non-Storable Content` (informational)

### Security scan artifacts

- `semgrep-report.json`
- `semgrep-after-custom.json`
- `zap-report.json`
- `zap-report.html`
- `report_json.json`
- `report_md.md`
- `report_html.html`

## CI/CD Security Pipelines (Implemented)

Workflow files:
- `.github/workflows/sast.yaml`
- `.github/workflows/dast.yaml`

### SAST workflow (`sast.yaml`)
1. Trigger on `push` / `pull_request` to `main` and team branch
2. Build and unit tests (`./mvnw clean install -DskipTests`, then `./mvnw -B test`)
3. SonarCloud analysis with quality gate wait enabled
4. Optional Trivy filesystem scan (`CRITICAL,HIGH`) with artifact upload
5. SAST gate decision: required SAST job must pass; Trivy remains warning-only

### DAST workflow (`dast.yaml`)
1. Trigger on `push` / `pull_request` to `main` and team branch
2. Build and unit tests
3. Start app and check readiness (`/swagger-ui/index.html`)
4. Run OWASP ZAP baseline scan in Docker
5. Parse JSON results and fail if any `Medium`/`High` alert is found
6. Upload ZAP artifacts and stop app

### Security gate principle
Any failed required security stage should block the merge or release candidate until the issue is reviewed and resolved.

This DevSecOps approach ensures that security is checked continuously instead of being postponed until the end of development.

## Production Security Notes

This project uses H2 and local HTTP for development/demo simplicity. In a production deployment, the following controls are required:

- Enforce HTTPS/TLS for all client-server communication
- Use a production-grade database such as PostgreSQL
- Store secrets in environment variables or a secrets manager
- Disable development-only features such as the H2 console
- Apply dependency and container scanning in CI/CD
- Add rate limiting and monitoring/alerting for sensitive endpoints
- Restrict CORS and review security headers for deployment context

## Scope Note

This repository reflects the latest code and security-report update.
Presentation slides and live demo materials may reference an earlier project snapshot.

## Contributors
- Devkumar Parikshit GANDHI
- Thai Bao DUONG
- Sofyen FENICH
- Arthur Amuda

## License
Academic and educational use.
