# PDF-to-Adaptive-Forms Migration

Use this playbook when replacing a PDF, scanned document, XFA form, or legacy
form workflow with an accessible, responsive Adaptive Form.

Migration is semantic and operational, not a pixel-for-pixel redraw. Create a
bead for each migration slice and keep the source version, target form version,
mapping register, validation evidence, and cutover decision linked.

## Required lifecycle

```text
inventory -> classify -> extract -> map -> rebuild -> validate
          -> pilot/coexist -> cut over -> archive/rollback -> evidence
```

Inventory the source PDF versions, owners, jurisdictions, languages, volume,
fields, calculations, validations, conditional sections, tables, signatures,
attachments, instructions, workflows, integrations, retention, downstream
consumers, and accessibility defects.

## Acceptance gates

- Every field, rule, option, output, attachment, signature, and integration is
  mapped or has an approved retirement decision.
- The Adaptive Form and Form Data Model preserve business meaning and data
  lineage; server-side calculations remain authoritative.
- Browser journeys cover representative and negative cases on supported
  devices, with keyboard and assistive-technology evidence.
- In-flight and historical submissions are reconciled before cutover.
- Old and new versions coexist safely, with routing, archive, audit linkage,
  and a tested rollback target.

Use Adaptive Form authoring for reusable content and rules, Form Data Models
for service contracts, custom components only where required, and Document of
Record configuration when a final artifact is needed. Do not store sensitive
source PDFs or migration evidence in public repository content.
