# Deployment
- Java 21 non-root minimal runtime image.
- Secrets from Vault/Kubernetes Secret/enterprise secret manager, never Git/Nacos plaintext.
- Flyway per service schema. Zero-downtime DB change uses Expand -> Migrate -> Contract.
- Main pipeline: compile -> unit/domain -> ArchUnit -> static analysis -> Testcontainers integration -> OpenAPI/schema validation -> image scan -> staging E2E -> deploy.
