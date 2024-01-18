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
package io.trino.gateway.containers;

import io.airlift.units.DataSize;

import static io.airlift.units.DataSize.Unit.GIGABYTE;

public class OpenTracingCollector
        extends BaseTestContainer
{
    private static final int GRPC_COLLECTOR_PORT = 4317;
    private static final int HTTP_COLLECTOR_PORT = 4318;
    private static final int FRONTEND_PORT = 16686;

    private final java.nio.file.Path storageDirectory;

    public OpenTracingCollector()
    {
        super(
                "jaegertracing/all-in-one:latest",
                "opentracing-collector",
                java.util.Set.of(GRPC_COLLECTOR_PORT, HTTP_COLLECTOR_PORT, FRONTEND_PORT),
                java.util.Map.of(),
                java.util.Map.of(
                    "COLLECTOR_OTLP_ENABLED", "true",
//                    "SPAN_STORAGE_TYPE", "badger", // KV that stores spans to the disk
                    "GOMAXPROCS", "2"), // limit number of threads used for goroutines
                java.util.Optional.empty(),
                1);
//        withRunCommand(java.util.List.of(
//                "--badger.ephemeral=false",
//                "--badger.span-store-ttl=15m",
//                "--badger.directory-key=/badger/data",
//                "--badger.directory-value=/badger/data",
//                "--badger.maintenance-interval=30s"));

        withCreateContainerModifier(command -> command.getHostConfig()
                .withMemory(DataSize.of(1, GIGABYTE).toBytes()));

        try {
            this.storageDirectory = java.nio.file.Files.createTempDirectory("tracing-collector");
            mountDirectory(storageDirectory.toString(), "/badger");
        }
        catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override
    public void close()
    {
        super.close();
        try (java.util.stream.Stream<java.io.File> files = java.nio.file.Files.walk(storageDirectory)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)) {
            files.forEach(java.io.File::delete);
        }
        catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
