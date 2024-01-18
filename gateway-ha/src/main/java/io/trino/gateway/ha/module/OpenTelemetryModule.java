/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.gateway.ha.module;

import static java.util.Objects.requireNonNull;

public class OpenTelemetryModule
        extends io.trino.gateway.baseapp.AppModule<io.trino.gateway.ha.config.HaGatewayConfiguration, io.dropwizard.core.setup.Environment>
{
    private io.opentelemetry.api.OpenTelemetry openTelemetry;

    public OpenTelemetryModule(io.trino.gateway.ha.config.HaGatewayConfiguration config, io.dropwizard.core.setup.Environment env)
    {
        super(config, env);
        io.opentelemetry.sdk.trace.export.SpanExporter otlpExporter = null;
        String protocol = config.getOtelConfiguration().otelCollectorOtlpProtocol();
        if (protocol.equals("http")) {
            otlpExporter = io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter.builder()
                        .setEndpoint(config.getOtelConfiguration().otelExporterOtlpEndpoint())
                        .build();
        }
        else if (protocol.equals("grpc")) {
            otlpExporter = io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter.builder()
                        .setEndpoint(config.getOtelConfiguration().otelExporterOtlpEndpoint())
                        .build();
        }

        requireNonNull(otlpExporter, "otlpExporter is null");

        io.opentelemetry.sdk.resources.Resource serviceNameResource =
                io.opentelemetry.sdk.resources.Resource.create(io.opentelemetry.api.common.Attributes.of(io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME, "trino-gateway", io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAMESPACE, "datalakeTrinoGatewayPrestodev"));

        io.opentelemetry.sdk.trace.SdkTracerProvider sdkTracerProvider = io.opentelemetry.sdk.trace.SdkTracerProvider.builder()
                    .addSpanProcessor(io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(otlpExporter).build())
                    .addSpanProcessor(io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(io.opentelemetry.exporter.logging.LoggingSpanExporter.create()).build())
                    .setResource(serviceNameResource)
                    .build();

        openTelemetry = io.opentelemetry.sdk.OpenTelemetrySdk.builder()
                    .setTracerProvider(sdkTracerProvider)
                    .build();
    }

    @com.google.inject.Provides
    @com.google.inject.Singleton
    public io.opentelemetry.api.OpenTelemetry getOpentelemetry()
    {
        return this.openTelemetry;
    }
}
