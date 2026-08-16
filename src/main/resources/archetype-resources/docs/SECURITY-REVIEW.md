# Security Review and Test Contract

This generated foundation is reviewed for common implementation risks, but it is
not a substitute for an organization’s threat model or penetration test.

## Controls included

- CSRF remains enforced by AEM Granite filters for browser submissions.
- Correlation IDs are validated and response bodies are not logged.
- Adobe Sign credentials use OSGi password fields; secrets must come from an
  environment secret store.
- Webhooks validate the Adobe Sign client ID and never log their payload.
- Provider errors omit response bodies to avoid leaking signer/document data.
- Read-only health and metrics endpoints expose counters, not form data.
- Circuit-breaker tests cover open, half-open, and recovery behavior.

## Required deployment review

Verify authentication and authorization for submit, status, webhook, Inbox, and
DAM paths; dispatcher allowlists; CORS; rate limits; payload size limits; webhook
replay protection; URL/SSRF policy for configurable providers; retention and
encryption of submissions and Documents of Record; and healthcare privacy
requirements before using real PHI.

## Evidence commands

```bash
mvn test
git grep -n -E 'LOG\\.(debug|info|warn|error).*body|LOG\\.(debug|info|warn|error).*payload'
```

The grep result must be reviewed manually; payload-size logs are allowed, raw
payload logs are not.
