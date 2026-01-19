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
package org.openrewrite.codemods;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.quark.Quark;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;

/**
 * Base class for recipes that apply external CLI tools (node, python, bash, etc.) to modify source files.
 * <p>
 * This recipe operates in three phases:
 * <ul>
 *   <li><b>Scanning Phase:</b> Serializes all source files to a temporary directory on disk</li>
 *   <li><b>Generate Phase:</b> Executes the CLI tool and detects which files were modified</li>
 *   <li><b>Edit Phase:</b> Reloads modified files and returns updated PlainText sources</li>
 * </ul>
 * <p>
 * Multiple CLI-based recipes can be chained together, with each recipe's output becoming the input to the next.
 * Additionally, subsequent non-CLI recipes can now process the modified files because the modified content
 * is reloaded back into the OpenRewrite tree during the edit phase.
 */
public abstract class CliBasedRecipe extends ScanningRecipe<CliBasedRecipe.Accumulator> {
    private static final String FIRST_RECIPE = CliBasedRecipe.class.getName() + ".FIRST_RECIPE";
    private static final String PREVIOUS_RECIPE = CliBasedRecipe.class.getName() + ".PREVIOUS_RECIPE";
    private static final String INIT_REPO_DIR = CliBasedRecipe.class.getName() + ".INIT_REPO_DIR";

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Path directory = createDirectory(ctx, "repo");
        if (ctx.getMessage(INIT_REPO_DIR) == null) {
            ctx.putMessage(INIT_REPO_DIR, directory);
            ctx.putMessage(FIRST_RECIPE, ctx.getCycleDetails().getRecipePosition());
        }
        return new Accumulator(directory);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && !(tree instanceof Quark) && !(tree instanceof ParseError) &&
                        !"org.openrewrite.java.tree.J$CompilationUnit".equals(tree.getClass().getName())) {
                    SourceFile sourceFile = (SourceFile) tree;
                    String fileName = sourceFile.getSourcePath().getFileName().toString();
                    if (fileName.indexOf('.') > 0) {
                        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                        acc.extensionCounts.computeIfAbsent(extension, e -> new AtomicInteger(0)).incrementAndGet();
                    }

                    // only extract initial source files for first CLI recipe
                    if (Objects.equals(ctx.getMessage(FIRST_RECIPE), ctx.getCycleDetails().getRecipePosition())) {
                        acc.writeSource(sourceFile);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        Path previous = ctx.getMessage(PREVIOUS_RECIPE);
        if (previous != null && !Objects.equals(ctx.getMessage(FIRST_RECIPE), ctx.getCycleDetails().getRecipePosition())) {
            acc.copyFromPrevious(previous);
        }

        runCommand(acc, ctx);
        ctx.putMessage(PREVIOUS_RECIPE, acc.getDirectory());

        return emptyList();
    }

    /**
     * Execute the CLI command. This method:
     * 1. Builds the command from {@link #getCommand(Accumulator, ExecutionContext)}
     * 2. Performs variable substitution on the command
     * 3. Executes the command as a process
     * 4. Detects which files were modified by comparing timestamps
     * 5. Calls {@link #processOutput(Path, Accumulator, ExecutionContext)} for output processing
     */
    protected void runCommand(Accumulator acc, ExecutionContext ctx) {
        Path dir = acc.getDirectory();

        List<String> command = getCommand(acc, ctx);
        if (command == null || command.isEmpty()) {
            return;
        }

        // Perform variable substitution
        List<String> expandedCommand = new ArrayList<>();
        for (String part : command) {
            expandedCommand.add(expandVariables(part, acc, ctx));
        }

        Map<String, String> env = getCommandEnvironment(acc, ctx);

        Path out = null;
        Path err = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(expandedCommand);
            builder.directory(dir.toFile());

            // Set environment variables
            env.forEach(builder.environment()::put);

            // Redirect output and error
            out = Files.createTempFile(WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(), "cli-tool", null);
            err = Files.createTempFile(WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(), "cli-tool", null);
            builder.redirectOutput(ProcessBuilder.Redirect.to(out.toFile()));
            builder.redirectError(ProcessBuilder.Redirect.to(err.toFile()));

            Process process = builder.start();
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                throw new RuntimeException(String.format("Command '%s' timed out after 5 minutes", String.join(" ", expandedCommand)));
            }
            if (process.exitValue() != 0) {
                String error = "Command failed: " + String.join(" ", expandedCommand);
                if (Files.exists(err)) {
                    error += "\n" + new String(Files.readAllBytes(err));
                }
                throw new RuntimeException(error);
            }

            // Detect modified files by checking modification timestamps
            for (Map.Entry<Path, Long> entry : acc.beforeModificationTimestamps.entrySet()) {
                Path path = entry.getKey();
                if (!Files.exists(path) || Files.getLastModifiedTime(path).toMillis() > entry.getValue()) {
                    acc.modified(path);
                }
            }

            processOutput(out, acc, ctx);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                //noinspection ResultOfMethodCallIgnored
                out.toFile().delete();
            }
            if (err != null) {
                //noinspection ResultOfMethodCallIgnored
                err.toFile().delete();
            }
        }
    }

    /**
     * Perform variable substitution on a command string.
     * Subclasses can override to support additional variables.
     */
    protected String expandVariables(String str, Accumulator acc, ExecutionContext ctx) {
        return str
                .replace("${repoDir}", ".")
                .replace("${workDir}", acc.getDirectory().toString());
    }

    /**
     * Get the command to execute. Must be implemented by subclasses.
     * The returned list should contain the executable followed by its arguments.
     * Common variables that can be used in the command:
     * - ${repoDir}: Current working directory (repository root)
     * - ${workDir}: Full path to the working directory
     *
     * @return List of command parts (executable and arguments), or null/empty if command should not run
     */
    protected abstract List<String> getCommand(Accumulator acc, ExecutionContext ctx);

    /**
     * Provide additional environment variables for the CLI tool execution.
     * Override this method to set tool-specific environment variables.
     */
    protected Map<String, String> getCommandEnvironment(Accumulator acc, ExecutionContext ctx) {
        return new HashMap<>();
    }

    /**
     * Process the output from the CLI tool (stdout).
     * Override this method to parse and handle tool-specific output.
     */
    protected void processOutput(Path out, Accumulator acc, ExecutionContext ctx) {
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    return createAfter(sourceFile, acc, ctx);
                }
                return tree;
            }
        };
    }

    protected SourceFile createAfter(SourceFile before, Accumulator acc, ExecutionContext ctx) {
        if (!acc.wasModified(before)) {
            return before;
        }
        return new PlainText(
                before.getId(),
                before.getSourcePath(),
                before.getMarkers(),
                before.getCharset() != null ? before.getCharset().name() : null,
                before.isCharsetBomMarked(),
                before.getFileAttributes(),
                null,
                acc.content(before),
                emptyList()
        );
    }

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class Accumulator {
        @Getter
        final Path directory;

        final Map<Path, Long> beforeModificationTimestamps = new HashMap<>();
        final Set<Path> modified = new LinkedHashSet<>();
        final Map<String, AtomicInteger> extensionCounts = new HashMap<>();
        final Map<String, Object> data = new HashMap<>();

        public void copyFromPrevious(Path previous) {
            try {
                Files.walkFileTree(previous, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path target = directory.resolve(previous.relativize(dir));
                        if (!target.equals(directory)) {
                            Files.createDirectory(target);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            Path target = directory.resolve(previous.relativize(file));
                            Files.copy(file, target);
                            beforeModificationTimestamps.put(target, Files.getLastModifiedTime(target).toMillis());
                        } catch (NoSuchFileException ignore) {
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public String parser() {
            if (extensionCounts.containsKey("tsx")) {
                return "tsx";
            }
            if (extensionCounts.containsKey("ts")) {
                return "ts";
            }
            return "babel";
        }

        public void writeSource(SourceFile tree) {
            try {
                Path path = resolvedPath(tree);
                Files.createDirectories(path.getParent());
                PrintOutputCapture.MarkerPrinter markerPrinter = PrintOutputCapture.MarkerPrinter.SANITIZED;
                Path written = Files.write(path, tree.printAll(new PrintOutputCapture<>(0, markerPrinter)).getBytes(tree.getCharset() != null ? tree.getCharset() : StandardCharsets.UTF_8));
                beforeModificationTimestamps.put(written, Files.getLastModifiedTime(written).toMillis());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void modified(Path path) {
            modified.add(path);
        }

        public boolean wasModified(SourceFile tree) {
            return modified.contains(resolvedPath(tree));
        }

        public String content(SourceFile tree) {
            try {
                Path path = resolvedPath(tree);
                return tree.getCharset() != null ? new String(Files.readAllBytes(path), tree.getCharset()) :
                        new String(Files.readAllBytes(path));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public Path resolvedPath(SourceFile tree) {
            return directory.resolve(tree.getSourcePath());
        }

        public <T> void putData(String key, T value) {
            data.put(key, value);
        }

        public <T> @Nullable T getData(String key) {
            //noinspection unchecked
            return (T) data.get(key);
        }
    }

    protected static Path createDirectory(ExecutionContext ctx, String prefix) {
        WorkingDirectoryExecutionContextView view = WorkingDirectoryExecutionContextView.view(ctx);
        return Optional.of(view.getWorkingDirectory())
                .map(d -> d.resolve(prefix))
                .map(d -> {
                    try {
                        return Files.createDirectory(d).toRealPath();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Failed to create working directory for " + prefix));
    }
}
