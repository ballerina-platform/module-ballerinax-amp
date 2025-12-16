/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.observe.trace.jaeger;

import io.ballerina.observe.trace.jaeger.sampler.RateLimitingSampler;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.tracer.spi.TracerProvider;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

/**
 * This is the Jaeger tracing extension class for {@link TracerProvider}.
 */
public class JaegerTracerProvider implements TracerProvider {
    private static final String TRACER_NAME = "jaeger";
    private static final PrintStream console = System.out;

    static SdkTracerProviderBuilder tracerProviderBuilder;
    static SdkTracerProvider sdkTracerProvider;
    static String serviceName;
    static String orgUid;
    static String projectUid;
    static String componentUid;
    static String environmentUid;

    @Override
    public String getName() {
        return TRACER_NAME;
    }

    @Override
    public void init() {    // Do Nothing
    }

    public static void initializeConfigurations(BString otelEndpoint, BString samplerType,
                                                BDecimal samplerParam, int reporterFlushInterval,
                                                int reporterBufferSize, BString apiKey, BString serviceName,
                                                BString orgUid, BString projectUid, BString componentUid,
                                                BString environmentUid) {
        initializeConfigurationsForInternal(
                otelEndpoint.toString(),
                samplerType.toString(),
                samplerParam.value().doubleValue(),
                reporterFlushInterval,
                reporterBufferSize,
                apiKey.toString(),
                serviceName.toString(),
                orgUid.toString(),
                projectUid.toString(),
                componentUid.toString(),
                environmentUid.toString());
    }

    /**
     * Initialize configurations with plain Java types (for testing without Ballerina runtime).
     */
    public static void initializeConfigurationsForInternal(String otelEndpoint, String samplerType,
                                                           double samplerParam, int reporterFlushInterval,
                                                           int reporterBufferSize, String apiKey, String serviceName,
                                                           String orgUid, String projectUid, String componentUid,
                                                           String environmentUid) {
        JaegerTracerProvider.serviceName = serviceName;
        JaegerTracerProvider.orgUid = orgUid;
        JaegerTracerProvider.projectUid = projectUid;
        JaegerTracerProvider.componentUid = componentUid;
        JaegerTracerProvider.environmentUid = environmentUid;

        String reporterEndpoint = otelEndpoint + "/v1/traces";

        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
                .setEndpoint(reporterEndpoint);

        if (!apiKey.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }

        OtlpHttpSpanExporter exporter = builder.build();


        tracerProviderBuilder = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor
                        .builder(exporter)
                        .setMaxExportBatchSize(reporterBufferSize)
                        .setExporterTimeout(reporterFlushInterval, TimeUnit.MILLISECONDS)
                        .build());

        tracerProviderBuilder.setSampler(selectSampler(samplerType, samplerParam));

        console.println("ballerina: started publishing traces to Jaeger on " + reporterEndpoint);
    }

    private static Sampler selectSampler(String samplerType, double samplerParam) {
        switch (samplerType) {
            default:
            case "const":
                if ((int) samplerParam == 0) {
                    return Sampler.alwaysOff();
                } else {
                    return Sampler.alwaysOn();
                }
            case "probabilistic":
                return Sampler.traceIdRatioBased(samplerParam);
            case RateLimitingSampler.TYPE:
                return new RateLimitingSampler((int) samplerParam);
        }
    }

    @Override
    public Tracer getTracer(String serviceName) {
        return getTracerInternal(serviceName);
    }

    private static Tracer getTracerInternal(String serviceName) {
        AttributesBuilder builder = Attributes.builder();
        if (!JaegerTracerProvider.serviceName.isEmpty()) {
            builder.put(SERVICE_NAME, JaegerTracerProvider.serviceName);
        } else {
            builder.put(SERVICE_NAME, serviceName);
        }
        if (!JaegerTracerProvider.orgUid.isEmpty()) {
            builder.put("openchoreo.dev/org-uid", JaegerTracerProvider.orgUid);
        }
        if (!JaegerTracerProvider.projectUid.isEmpty()) {
            builder.put("openchoreo.dev/project-uid", JaegerTracerProvider.projectUid);
        }
        if (!JaegerTracerProvider.componentUid.isEmpty()) {
            builder.put("openchoreo.dev/component-uid", JaegerTracerProvider.componentUid);
        }
        if (!JaegerTracerProvider.environmentUid.isEmpty()) {
            builder.put("openchoreo.dev/environment-uid", JaegerTracerProvider.environmentUid);
        }
        sdkTracerProvider = tracerProviderBuilder
                .setResource(Resource.create(builder.build()))
                .build();
        return sdkTracerProvider.get("jaeger");
    }

    @Override
    public ContextPropagators getPropagators() {

        return ContextPropagators.create(W3CTraceContextPropagator.getInstance());
    }

    /**
     * Shutdown the tracer provider and flush all pending spans.
     */
    public static void shutdown() {
        if (sdkTracerProvider != null) {
            sdkTracerProvider.shutdown();
        }
    }
}
