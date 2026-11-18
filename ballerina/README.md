## Overview

The `ballerinax/amp` package connects Ballerina AI agents to Choreo Agent Manager. It provides:

- **Tracing**: an observability extension that publishes an agent's traces to Agent Manager through the OpenTelemetry Protobuf HTTP endpoint.
- **Chat service**: `amp:Listener` and `amp:ChatService`, which expose an agent through the `POST /chat` endpoint that the Agent Manager console's Try It page calls.

### Key Features

- Publish traces to Choreo Agent Manager via OpenTelemetry
- API key-based authentication for publishing traces
- Configurable organization, project, component, and environment identifiers
- A chat service and listener that match the Agent Manager console's chat API

## Publishing Traces

To publish an agent's traces to Agent Manager, follow the steps below.

1. Create a program with an AI agent, e.g. https://ballerina.io/learn/by-example/chat-agents/

2. Import the package. If you use it only for tracing, import it with the `_` alias:

```ballerina
import ballerinax/amp as _;
```

If you also use its chat service (see [Exposing an Agent to the Agent Manager Console](#exposing-an-agent-to-the-agent-manager-console)), import it without an alias; tracing works the same way:

```ballerina
import ballerinax/amp;
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
otelEndpoint="http://localhost:22893/otel"  # Optional. Default: http://localhost:22893/otel

# Amp authentication and identification (optional)
# If passed empty string (default value) these will not be added.
apiKey=""          # API key for authentication send via Authorization header
orgUid=""          # Organization UID send as a resource attribute
projectUid=""      # Project UID send as a resource attribute
componentUid=""    # Component UID send as a resource attribute
environmentUid=""  # Environment UID send as a resource attribute
```

5. Send a message to the agent. Each run publishes a trace to Agent Manager. For an agent exposed with `ai:Listener`, use the `Try It` feature in the Ballerina plugin or the AI Chat view in the `BI` plugin. For one exposed with `amp:ChatService`, use the Agent Manager console's Try It page, or send a request to its `/chat` endpoint.

## Exposing an Agent to the Agent Manager Console

`amp:Listener` and `amp:ChatService` expose an agent through the `POST /chat` endpoint that the Agent Manager console's Try It page calls. The service is served directly by an `http:Listener`, so it remains the entrypoint of the traces it produces.

```ballerina
import ballerina/http;
import ballerinax/amp;

service amp:ChatService / on new amp:Listener(8000) {
    resource function post chat(@http:Payload amp:ChatRequest request) returns amp:ChatResponse|error {
        string reply = check agent.run(request.message, request.sessionId);
        return {response: reply};
    }
}
```

- The request parameter must be annotated with `@http:Payload`.
- `amp:Listener` takes a port (default `8000`, the port Agent Manager calls) or an existing `http:Listener`.

The endpoint accepts and returns JSON:

```sh
curl -s localhost:8000/chat -H 'Content-Type: application/json' \
  -d '{"session_id": "session-123", "message": "Hi, how can you help me?", "context": {}}'
```

```json
{"response": "Hello! How can I help?"}
```

| Field | Type | Description |
|---|---|---|
| `session_id` | `string` | Conversation identifier (`ChatRequest.sessionId`); reuse it for follow-up messages |
| `message` | `string` | The user's message |
| `context` | `object` | Optional context; the console sends an empty object |
| `response` | `string` | The agent's reply (`ChatResponse.response`) |

Additional fields in the request are kept as rest fields of `ChatRequest`, and additional fields in a `ChatResponse` are sent as-is.
