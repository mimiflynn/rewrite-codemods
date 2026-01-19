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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for recipes that apply Node.js/JavaScript-based codemods (jscodeshift, ESLint, Biome, etc.).
 * <p>
 * This class extends {@link CliBasedRecipe} and provides Node.js-specific utilities including:
 * <ul>
 *   <li>Uses npx to execute Node.js packages on-demand (no bundled dependencies)</li>
 *   <li>Parser detection based on file extensions</li>
 *   <li>Node-specific variable substitution</li>
 * </ul>
 */
public abstract class NodeBasedRecipe extends CliBasedRecipe {

    @Override
    protected String expandVariables(String str, Accumulator acc, ExecutionContext ctx) {
        return super.expandVariables(str, acc, ctx)
                .replace("${parser}", acc.parser());
    }

    @Override
    protected final List<String> getCommand(Accumulator acc, ExecutionContext ctx) {
        return getNpmCommand(acc, ctx);
    }

    /**
     * Get the npm/Node.js command to execute. Must be implemented by subclasses.
     * <p>
     * Commands should use npx to execute packages, e.g.:
     * <pre>
     * Arrays.asList("npx", "-y", "eslint@8.56.0", "${repoDir}", "--fix")
     * </pre>
     * <p>
     * Available variables:
     * - ${repoDir}: Current working directory (repository root)
     * - ${parser}: Auto-detected parser based on file extensions (tsx, ts, or babel)
     *
     * @return List of command parts (executable and arguments), or empty/null if command should not run
     */
    protected abstract List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx);

    @Override
    protected Map<String, String> getCommandEnvironment(Accumulator acc, ExecutionContext ctx) {
        Map<String, String> env = new HashMap<>(getNodeCommandEnvironment(acc, ctx));
        env.put("TERM", "dumb");
        return env;
    }

    /**
     * Provide additional environment variables for the Node.js process.
     * Override this method to set tool-specific environment variables beyond the defaults.
     */
    protected Map<String, String> getNodeCommandEnvironment(Accumulator acc, ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    protected void processOutput(Path out, Accumulator acc, ExecutionContext ctx) {
    }
}
