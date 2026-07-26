# AEM Forms BMAD Archetype: Summary

> **Read this alongside the repository's `README.md#implementation-status`
> table, which is the authoritative, kept-current source of truth for what's
> real versus aspirational.** This document describes the project's
> direction and what's been verified; it is not a "production ready"
> sign-off.

## 1. Project Vision

This project explores spec-driven, AI-assisted AEM Forms development: a
Maven archetype scaffolding a modern stack (Core Components Adaptive
Forms, a React headless renderer, Java Sling Models), plus a
`SpecToCodeGenerator` that turns a JSON Schema spec into real generated
code and content.

## 2. What's Actually Verified (not aspirational)

- **Complete Adaptive Form generation** (`SpecToCodeGenerator.generateForm()`)
  — real JCR content, a real submit action, live-verified end to end
  including an actual accepted submission (`HTTP 200`).
- **Document of Record generation** (`SignToDoRProcess`) — calls the real
  `DoRService`, live-verified, with real documented prerequisites (DAM
  metadata, an `xdpRef` template, the native rendering SDK).
- **Form submission dispatch** (`FormSubmissionService` /
  `HeadlessSubmitServlet`) — a real HTTP POST to a configurable external
  endpoint, live-verified for both success and failure.
- **Adobe Sign integration** (`AdobeSignOrchestratorImpl`) — real REST API
  v6 calls, verified against Adobe's own docs and live-deployed for OSGi
  activation, but **not yet proven against a real Adobe Sign account**.
- **Interactive Communications** (`InteractiveCommunicationServiceImpl`) —
  calls the real `PrintChannelRenderService`, but that service was
  `unsatisfied` (feature-toggle-gated) on the instance this was built
  against — **check your own instance before relying on it**.

## 3. What's Aspirational (design sketches, not implemented)

Several docs under `06-Integrations/` (`enterprise-hardening-guide.md`,
`omnichannel-architecture.md`, `interactive-communications-guide.md`)
describe a broader vision — Document Rights Management, Sensei-based
legacy form conversion, FDM-as-middleware, multi-channel IC beyond Print
— **none of which exist as real code in this archetype today**. Read them
as a roadmap, not documentation of working features. Building any of them
for real would need the same ground-truth API research every real feature
above got before writing code.

## 4. Real, Open Gaps

See `README.md`'s **Next Steps** section for the current, maintained list
— it changes as work lands, so it's kept in the README rather than
duplicated here.

## 5. Adopting This Archetype

1. Run `mvn archetype:generate` to scaffold a new project (see README
   Quick Start).
2. Read `README.md#implementation-status` first — know what's real before
   you build on it.
3. Use `bmad/gastown/agents/` (`aem-component-coder`, `aem-code-reviewer`)
   as a starting point for AI-assisted development conventions, not as a
   guarantee of a particular quality bar.
