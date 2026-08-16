# Sensitive Data
- Phone/ID/bank/tax fields encrypted using application envelope encryption (e.g. AES-GCM) with key references in KMS/SecretManager.
- Searchable phone uses HMAC-SHA256(normalizedPhone), not plaintext index.
- Payment/Invoice credentials store only secret references.
- Logs/audit snapshots must filter tokens, secrets, bank data and raw provider credentials.
- File download uses short-lived PreSigned URL after tenant + permission + file ACL re-check.
