## Package Overview

The Amp Observability Extension is one of the tracing extensions of the<a target="_blank" href="https://ballerina.io/"> Ballerina</a> language.

It provides an implementation for tracing and publishing traces to a WSO2 AI Agent Platform using OpenTelemetry Protobuf HTTP endpoint.

## Enabling Amp Extension

To enable the AMP extension in a Ballerina program, follow the below steps.

1. Create a program with an AI agent eg: https://ballerina.io/learn/by-example/chat-agents/

2. Add the following import to your program.

```ballerina
import ballerinax/amp as _;
```

3. Add the following to the `Ballerina.toml` when building your program.

```toml
[package]
org = "my_org"
name = "my_package"
version = "1.0.0"

[build-options]
observabilityIncluded=true
```

4. Add the following to the `Config.toml` when running your program.

```toml
[ballerina.observe]
tracingEnabled=true
tracingProvider="amp"

[ballerinax.amp]
# OpenTelemetry endpoint for Amp
otelEndpoint="http://localhost:21893"  # Optional. Default: http://localhost:21893

# Amp authentication and identification (optional)
# If passed empty string (default value) these will not be added.
apiKey=""          # API key for authentication send via Authorization header
serviceName=""     # Name of the service send as a resource attribute
orgUid=""          # Organization UID send as a resource attribute
projectUid=""      # Project UID send as a resource attribute
componentUid=""    # Component UID send as a resource attribute
environmentUid=""  # Environment UID send as a resource attribute
```

5. Use `Try It` feature in Ballerina plugin or AI Chat view in `BI` plugin to send a message to the agent. This will result in a trace being published to the WSO2 AI Agent Platform.
