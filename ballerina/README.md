## Package Overview

The Amp Observability Extension is one of the tracing extensions of the<a target="_blank" href="https://ballerina.io/"> Ballerina</a> language.

It provides an implementation for tracing and publishing traces to a Amp Agent.

## Enabling Amp Extension

To package the Amp extension into the Jar, follow the following steps.

1. Add the following import to your program.

```ballerina
import ballerinax/amp as _;
```

2. Add the following to the `Ballerina.toml` when building your program.

```toml
[package]
org = "my_org"
name = "my_package"
version = "1.0.0"

[build-options]
observabilityIncluded=true
```

To enable the extension and publish traces to Amp, add the following to the `Config.toml` when running your program.

```toml
[ballerina.observe]
tracingEnabled=true
tracingProvider="amp"

[ballerinax.amp]
# OpenTelemetry endpoint for Amp
otelEndpoint="http://localhost:21893"  # Optional. Default: http://localhost:21893

# Amp authentication and identification
# If passed empty string these will not be added at all
apiKey=""                              # Optional. API key for authentication send as header
serviceName=""                         # Optional. Name of the service send as resource attribute
orgUid=""                              # Optional. Organization UID send as resource attribute
projectUid=""                          # Optional. Project UID send as resource attribute
componentUid=""                        # Optional. Component UID send as resource attribute
environmentUid=""                      # Optional. Environment UID send as resource attribute
```
