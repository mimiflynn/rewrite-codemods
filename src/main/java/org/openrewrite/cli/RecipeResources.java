/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.cli;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

/**
 * Utility class for extracting bundled resources (config files, scripts) from the JAR.
 * Used by recipes that need configuration files like ESLint, Putout, and UI5.
 */
public final class RecipeResources {

    private RecipeResources() {
    }

    public static RecipeResources from(Class<? extends Recipe> recipeClass) {
        return new RecipeResources();
    }

    public Path extractResources(String resource, String dir, ExecutionContext ctx) {
        return extractResources(resource, () -> {
            try {
                WorkingDirectoryExecutionContextView view = WorkingDirectoryExecutionContextView.view(ctx);
                return Files.createDirectory(view.getWorkingDirectory().resolve(dir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static synchronized Path extractResources(String resource, Supplier<Path> dir) {
        try {
            URI uri = Objects.requireNonNull(RecipeResources.class.getClassLoader().getResource(resource)).toURI();
            if ("jar".equals(uri.getScheme())) {
                FileSystem fileSystem;
                try {
                    fileSystem = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    fileSystem = FileSystems.newFileSystem(uri, emptyMap(), null);
                }
                try (FileSystem localFileSystem = fileSystem) {
                    Path resourcePath = localFileSystem.getPath("/" + resource);
                    Path target = dir.get();
                    copyRecursively(resourcePath, target);
                    return target;
                }
            } else if ("file".equals(uri.getScheme())) {
                return Paths.get(uri);
            } else {
                throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyRecursively(Path sourceDir, Path targetDir) throws IOException {
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(source -> {
                try {
                    // IMPORTANT: `toString()` call here is required as paths have different file systems
                    Files.copy(source, targetDir.resolve(sourceDir.relativize(source).toString()), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            });
        }
    }
}
