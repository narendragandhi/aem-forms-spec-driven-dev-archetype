# SOC 2 Readiness Plan

This repository provides a SOC 2 readiness kit. It does not assert SOC 2
compliance or certification. A service organization must define its system
boundary, operate the controls, retain evidence, and complete an independent
examination.

The default scope is Security. Availability, Processing Integrity,
Confidentiality, and Privacy become in-scope when selected in the system
description and customer commitments.

## Readiness gates

1. Define the system boundary, services, environments, data classes, vendors,
   sub-processors, and control owners.
2. Approve policies for access, change, vulnerability, incident, backup,
   retention, vendor risk, and business continuity.
3. Operate controls for a defined observation period and collect dated evidence.
4. Test control design and operating effectiveness; record exceptions and
   remediation owners.
5. Have an independent CPA firm perform the appropriate SOC examination.

## Repository evidence

- `SOC2-CONTROLS.csv`: control objectives, implementation status, owners, and
  expected evidence.
- `EVIDENCE-REGISTER.md`: dated evidence index and retention expectations.
- `ACCESS-REVIEW.md`: quarterly access-review procedure.
- `CHANGE-MANAGEMENT.md`: pull-request and deployment control procedure.
- `INCIDENT-RESPONSE.md`: incident classification, escalation, and exercise log.
- `VENDOR-RISK.md`: provider and subprocessor review procedure.

## Current honest status

The generated application has useful technical controls such as correlation IDs,
redacted logging, CSRF integration points, health checks, and test seams. It does
not provide organizational evidence, continuous operation history, independent
review, or a SOC report by itself.
