# Dev

## Build & Test
```bash
./gradlew test
./gradlew runIde        # launch IDE sandbox
```

## Publish
Bump `pluginVersion` in `gradle.properties`, then:
```bash
source .env && export PUBLISH_TOKEN && ./gradlew publishPlugin
```
