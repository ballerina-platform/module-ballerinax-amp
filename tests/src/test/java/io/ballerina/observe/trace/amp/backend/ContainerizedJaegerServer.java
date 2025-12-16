/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.trace.amp.backend;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Container based Amp Server.
 * <p>
 * This is a Amp server implementation based on a linux Amp container.
 */
public class ContainerizedJaegerServer implements JaegerServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerizedJaegerServer.class);
    private static final String AMP_IMAGE = "amptracing/opentelemetry-all-in-one:latest";
    private static final int API_PORT = 16686;

    private DockerClient dockerClient;
    private String ampContainerId;

    public ContainerizedJaegerServer() throws Exception {
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        dockerClient = DockerClientImpl.getInstance(dockerClientConfig,
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(dockerClientConfig.getDockerHost())
                        .sslConfig(dockerClientConfig.getSSLConfig())
                        .build());
        dockerClient.pullImageCmd(AMP_IMAGE)
                .start()
                .awaitCompletion(20, TimeUnit.SECONDS);
    }

    @Override
    public void startServer(String interfaceIP, int udpBindPort, JaegerServerProtocol protocol) {
        if (ampContainerId != null) {
            throw new IllegalStateException("Amp server already started");
        }
        if (dockerClient == null) {
            throw new IllegalStateException("Containerized Amp server cannot be started after " +
                    "cleaning up docker client");
        }
        int targetPort;
        switch (protocol) {
            case OTL_GRPC:
                targetPort = 55680;
                break;
            default:
                throw new IllegalArgumentException("Unknown Amp Protocol type " + protocol);
        }
        ampContainerId = dockerClient.createContainerCmd(AMP_IMAGE)
                .withName("ballerina-test-amp-" + System.currentTimeMillis())
                .withExposedPorts(new ExposedPort(API_PORT), new ExposedPort(targetPort))
                .withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(PortBinding.parse(interfaceIP + ":" + API_PORT + ":" + API_PORT + "/tcp"),
                                PortBinding.parse(interfaceIP + ":" + udpBindPort + ":" + targetPort + "/tcp")))
                .exec()
                .getId();
        dockerClient.startContainerCmd(ampContainerId).exec();
        dockerClient.logContainerCmd(ampContainerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new ContainerLogReader());
        LOGGER.info("Started Amp container with container ID " + ampContainerId);
    }

    @Override
    public void stopServer() {
        if (ampContainerId != null) {
            dockerClient.stopContainerCmd(ampContainerId).exec();
            LOGGER.info("Stopped Amp container with container ID " + ampContainerId);
            dockerClient.removeContainerCmd(ampContainerId).exec();
            ampContainerId = null;
        }
    }

    @Override
    public void cleanUp() throws IOException {
        dockerClient.close();
        dockerClient = null;
    }
}
