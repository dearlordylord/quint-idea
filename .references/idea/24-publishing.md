# Publishing Plugins

Sources:
- https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
- https://plugins.jetbrains.com/docs/intellij/plugin-signing.html

## Prerequisites

- Plugin follows UX recommendations
- Plugin is properly signed
- JetBrains Marketplace account created at https://account.jetbrains.com

## First-Time Upload

Must be done manually:
1. Log into JetBrains Marketplace (https://plugins.jetbrains.com/author/me)
2. Click "Add new plugin"
3. Complete form and upload JAR/ZIP

## Gradle Publishing

### Build Distribution

```bash
./gradlew buildPlugin     # Creates ZIP in build/distributions/
./gradlew signPlugin       # For signed distributions
```

### Personal Access Token

1. Go to https://plugins.jetbrains.com/author/me/tokens
2. Generate token (displayed only once)
3. Store securely

### Configure Token

```bash
# Environment variable
export ORG_GRADLE_PROJECT_intellijPlatformPublishingToken='YOUR_TOKEN'

# Or via Gradle parameter
-PintellijPlatformPublishingToken=YOUR_TOKEN
```

### Publish

```bash
./gradlew publishPlugin
```

### Release Channels

```kotlin
intellijPlatform {
    publishing {
        channels.set(listOf("beta"))
    }
}
```

| Channel | URL | Access |
|---------|-----|--------|
| (default/empty) | Standard marketplace | All users |
| alpha | `.../plugins/alpha/list` | Manual repo add |
| beta | `.../plugins/beta/list` | Manual repo add |
| eap | `.../plugins/eap/list` | Manual repo add |

Non-default channels require users to add custom repository in IDE settings.

## Plugin Signing

### Why Sign?

- Ensures plugins remain unmodified through the pipeline
- Required since 2021.2
- Warning dialog shown for unsigned plugins

### Generate Keys

```bash
# Generate encrypted private key
openssl genpkey \
    -aes-256-cbc \
    -algorithm RSA \
    -out private_encrypted.pem \
    -pkeyopt rsa_keygen_bits:4096

# Convert to RSA format
openssl rsa \
    -in private_encrypted.pem \
    -out private.pem

# Generate certificate (valid 365 days)
openssl req \
    -key private.pem \
    -new \
    -x509 \
    -days 365 \
    -out chain.crt
```

**Never commit credentials to version control.**

### Gradle Signing Configuration

```kotlin
signPlugin {
    certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
}

publishPlugin {
    token.set(providers.environmentVariable("PUBLISH_TOKEN"))
}
```

File-based:

```kotlin
signPlugin {
    certificateChainFile.set(file("certificate/chain.crt"))
    privateKeyFile.set(file("certificate/private.pem"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
}
```

### Signing Flow

1. Author signs plugin with private key
2. Marketplace verifies with author's public key
3. Marketplace re-signs with JetBrains CA
4. IDE verifies both signatures on install

### Verify Signature

```bash
./gradlew verifyPluginSignature
```

### CLI Signing (Non-Gradle)

```bash
# Sign
java -jar marketplace-zip-signer-cli.jar sign \
    -in "unsigned.zip" \
    -out "signed.zip" \
    -cert-file "/path/to/chain.crt" \
    -key-file "/path/to/private.pem" \
    -key-pass "PRIVATE_KEY_PASSWORD"

# Verify
java -jar marketplace-zip-signer-cli.jar verify \
    -in "signed.zip" \
    -cert "/path/to/chain.crt"
```

## Versioning

- Marketplace rejects duplicate versions
- Use semantic versioning (MAJOR.MINOR.PATCH)
- Update `<version>` in plugin.xml or configure via Gradle

## Post-Deployment

- Users with installed plugin receive update notifications
- JetBrains verifies new versions before distribution
- Plugin updates appear in IDE settings

## CI/CD Integration

The IntelliJ Platform Plugin Template (https://github.com/JetBrains/intellij-platform-plugin-template) includes:
- GitHub Actions workflow for building, testing, signing, publishing
- Automated changelog management
- Release draft creation
- Plugin verification

### Example GitHub Actions Step

```yaml
- name: Publish Plugin
  env:
    PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
    CERTIFICATE_CHAIN: ${{ secrets.CERTIFICATE_CHAIN }}
    PRIVATE_KEY: ${{ secrets.PRIVATE_KEY }}
    PRIVATE_KEY_PASSWORD: ${{ secrets.PRIVATE_KEY_PASSWORD }}
  run: ./gradlew publishPlugin
```
