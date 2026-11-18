// Copyright (c) 2026 WSO2 LLC (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;

# A listener that serves `ChatService`s over HTTP.
#
# The listener attaches each service directly to its underlying `http:Listener`, without a service
# in between. As a result, the user's service remains the entrypoint of the traces it produces.
#
# ```ballerina
# service amp:ChatService / on new amp:Listener(8000) {
#     // ...
# }
# ```
public class Listener {
    private http:Listener httpListener;

    # Initializes the listener.
    #
    # Pass a port to start a new HTTP listener on it, or pass an existing `http:Listener` to serve
    # chat services on the same port as the program's other HTTP services.
    #
    # + listenOn - The port to listen on, or the `http:Listener` to attach services to. Defaults to 8000,
    #              the port Choreo Agent Manager calls
    # + return - An error if a new `http:Listener` could not be created on the given port
    public function init(int|http:Listener listenOn = 8000) returns error? {
        if listenOn is http:Listener {
            self.httpListener = listenOn;
        } else {
            self.httpListener = check new (listenOn);
        }
    }

    # Attaches a chat service to the listener.
    #
    # + chatService - The service to attach
    # + name - The base path of the service, as one string or as path segments. If `()`, the
    #          service is served at the root path
    # + return - An error if the service could not be attached
    public isolated function attach(ChatService chatService, string[]|string? name = ()) returns error? {
        check self.httpListener.attach(chatService, name);
    }

    # Detaches a chat service from the listener, so it no longer receives requests.
    #
    # + chatService - The service to detach
    # + return - An error if the service could not be detached
    public isolated function detach(ChatService chatService) returns error? {
        check self.httpListener.detach(chatService);
    }

    # Starts the listener, so attached services begin receiving requests.
    #
    # + return - An error if the listener could not be started
    public isolated function 'start() returns error? {
        check self.httpListener.start();
    }

    # Stops the listener after in-flight requests complete.
    #
    # + return - An error if the listener could not be stopped
    public isolated function gracefulStop() returns error? {
        check self.httpListener.gracefulStop();
    }

    # Stops the listener immediately, without waiting for in-flight requests.
    #
    # + return - An error if the listener could not be stopped
    public isolated function immediateStop() returns error? {
        check self.httpListener.immediateStop();
    }
}
