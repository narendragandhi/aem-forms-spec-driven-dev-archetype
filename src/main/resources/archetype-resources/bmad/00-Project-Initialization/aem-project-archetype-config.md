# AEM Project Archetype Configuration

This document records the parameters used to generate the AEM project using the official AEM Project Archetype, updated for AEM Forms BMAD v6.

## Archetype Version

- **Archetype Version**: 56 (October 2025)

## Archetype Parameters

- **groupId**: com.example
- **artifactId**: aem-forms-bmad-archetype
- **version**: 1.0.0-SNAPSHOT
- **appName**: aem-forms-bmad
- **appTitle**: AEM Forms BMAD Project
- **package**: com.example.aem.forms.bmad
- **singleCountry**: n
- **includeExamples**: y
- **includeErrorHandler**: y
- **includeDispatcherConfig**: y
- **frontendModule**: react
- **includeFormsenrollment**: y
- **aemVersion**: cloud
- **sdkVersion**: 2026.2.24678.20260226T154829Z-260200 *(as originally generated — see note below)*

> **Note (2026-09-05):** `pom.xml`'s pinned versions have since been
> manually updated past what's shown above — `aem.sdk.api` to
> `2026.8.27673.20260811T193135Z-260700`, `core.wcm.components.version`
> to `2.28.0`, and `aem.forms.sdk.api` to `2026.08.12.00-260700` (matching
> the `-2607` suffix convention, confirmed against Maven Central). This
> record is left as-is below since it documents the actual original
> `archetype:generate` invocation — not something to rewrite retroactively.

## Command Used

```bash
mvn -B archetype:generate \
 -D archetypeGroupId=com.adobe.aem \
 -D archetypeArtifactId=aem-project-archetype \
 -D archetypeVersion=56 \
 -D aemVersion=cloud \
 -D appTitle="AEM Forms BMAD Project" \
 -D appId="aem-forms-bmad" \
 -D groupId="com.example" \
 -D frontendModule="react" \
 -D includeExamples=y \
 -D includeFormsenrollment=y
```

## Edge Delivery Services (EDS) Integration

For BMAD v6 projects targeting Edge Delivery Services, use the AEM Forms Boilerplate as an additional resource:

- **Repository**: [https://github.com/adobe-rnd/aem-boilerplate-forms](https://github.com/adobe-rnd/aem-boilerplate-forms)
- **Integration**: Link your AEM authoring environment to the EDS repository using `fstab.yaml`.
