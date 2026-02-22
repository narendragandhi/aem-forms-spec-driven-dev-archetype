# AEM Project Archetype Configuration

This document records the parameters used to generate the AEM project using the official AEM Project Archetype.

## Archetype Version

- **Archetype Version**: 35

## Archetype Parameters

- **groupId**: com.example
- **artifactId**: aem-bmad-showcase
- **version**: 1.0.0-SNAPSHOT
- **appName**: aem-bmad-showcase
- **appTitle**: AEM BMAD Showcase
- **package**: com.example.aem.bmad
- **singleCountry**: n
- **includeExamples**: y
- **includeErrorHandler**: y
- **includeDispatcherConfig**: y
- **frontendModule**: react
- **sdkVersion**: 2023.10.14643.20231013T222839Z-230900

## Command Used

```bash
mvn -B archetype:generate \
 -D archetypeGroupId=com.adobe.aem \
 -D archetypeArtifactId=aem-project-archetype \
 -D archetypeVersion=35 \
 -D aemVersion=cloud \
 -D appTitle="AEM BMAD Showcase" \
 -D appId="aem-bmad-showcase" \
 -D groupId="com.example" \
 -D frontendModule="react" \
 -D includeExamples=y
```

