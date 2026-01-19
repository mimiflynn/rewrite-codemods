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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.emptyList;

/**
 * A generic CLI-based recipe that can execute any command-line tool to modify source files.
 * <p>
 * This recipe supports any executable (Python, Ruby, Go, Bash, etc.) and provides a flexible way
 * to integrate external tools into OpenRewrite's recipe system.
 * <p>
 * Example usage:
 * <pre>
 * new ApplyCliTool()
 *   .setCommand("black")
 *   .setArgs(new ArrayList&lt;&gt;() {{ add("${repoDir}"); }})
 * </pre>
 * <p>
 * This recipe can be chained with other recipes, and subsequent recipes will see the modifications
 * made by the CLI tool because the modified files are reloaded into the OpenRewrite tree.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ApplyCliTool extends CliBasedRecipe {

    @Option(displayName = "CLI command",
            description = "The executable or command to run (e.g., 'python', 'black', 'ruby', 'bash')",
            example = "black"
    )
    @Nullable
    String command;

    @Option(displayName = "Command arguments",
            description = "Arguments to pass to the CLI tool. Supports variables like ${repoDir} and ${workDir}.",
            example = "${repoDir}",
            required = false)
    @Nullable
    List<String> args;

    @Option(displayName = "Working directory",
            description = "Optional environment variable to set as the working directory for the tool.",
            example = "WORK_DIR",
            required = false)
    @Nullable
    String workDirEnvVar;

    @Option(displayName = "Additional environment variables",
            description = "Key-value pairs for environment variables (format: KEY=VALUE)",
            example = "PYTHONPATH=/custom/path",
            required = false)
    @Nullable
    List<String> envVars;

    @Override
    public String getDisplayName() {
        return "Apply CLI tool to source files";
    }

    @Override
    public String getDescription() {
        return "Applies an external CLI tool (e.g., formatter, linter, custom tool) to all source files.";
    }

    @Override
    protected List<String> getCommand(Accumulator acc, ExecutionContext ctx) {
        if (command == null || command.isEmpty()) {
            return emptyList();
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(command);

        if (args != null) {
            cmd.addAll(args);
        }

        return cmd;
    }

    @Override
    protected java.util.Map<String, String> getCommandEnvironment(Accumulator acc, ExecutionContext ctx) {
        java.util.Map<String, String> env = new java.util.HashMap<>();

        if (workDirEnvVar != null && !workDirEnvVar.isEmpty()) {
            env.put(workDirEnvVar, acc.getDirectory().toString());
        }

        if (envVars != null) {
            for (String envVar : envVars) {
                int idx = envVar.indexOf('=');
                if (idx > 0) {
                    String key = envVar.substring(0, idx);
                    String value = envVar.substring(idx + 1);
                    env.put(key, value);
                } else {
                    throw new IllegalArgumentException("Invalid environment variable format: '" + envVar
                            + "'. Expected format is KEY=VALUE.");
                }
            }
        }

        return env;
    }
}
