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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * A generic CLI-based recipe that can execute any command-line tool to modify source files.
 * <p>
 * This recipe supports any executable (Python, Ruby, Go, Bash, etc.) and provides a flexible way
 * to integrate external tools into OpenRewrite's recipe system.
 * <p>
 * Example usage in YAML:
 * <pre>
 * type: specs.openrewrite.org/v1beta/recipe
 * name: com.example.FormatWithBlack
 * displayName: Format Python with Black
 * recipeList:
 *   - org.openrewrite.cli.ApplyCliTool:
 *       displayName: Format Python files
 *       description: Applies Black formatter to all Python files
 *       command: black
 *       args:
 *         - "${repoDir}"
 *       timeoutMinutes: 10
 * </pre>
 * <p>
 * This recipe can be chained with other recipes, and subsequent recipes will see the modifications
 * made by the CLI tool because the modified files are reloaded into the OpenRewrite tree.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ApplyCliTool extends CliBasedRecipe {

    @Option(displayName = "Display name",
            description = "A human-readable name for this recipe instance.",
            example = "Format Python with Black",
            required = false)
    @Nullable
    String displayName;

    @Option(displayName = "Description",
            description = "A description of what this recipe instance does.",
            example = "Applies Black formatter to all Python files in the repository.",
            required = false)
    @Nullable
    String description;

    @Option(displayName = "CLI command",
            description = "The executable or command to run (e.g., 'python', 'black', 'ruby', 'bash')",
            example = "black")
    String command;

    @Option(displayName = "Command arguments",
            description = "Arguments to pass to the CLI tool. Supports variables: ${repoDir} (current directory), ${workDir} (full working directory path).",
            example = "${repoDir}",
            required = false)
    @Nullable
    List<String> args;

    @Option(displayName = "Working directory environment variable",
            description = "Name of an environment variable to set with the working directory path.",
            example = "WORK_DIR",
            required = false)
    @Nullable
    String workDirEnvVar;

    @Option(displayName = "Environment variables",
            description = "Additional environment variables as KEY=VALUE pairs.",
            example = "PYTHONPATH=/custom/path",
            required = false)
    @Nullable
    List<String> envVars;

    @Option(displayName = "Timeout (minutes)",
            description = "Maximum time to wait for the command to complete. Defaults to 5 minutes.",
            example = "10",
            required = false)
    @Nullable
    Integer timeoutMinutes;

    @Option(displayName = "Acceptable exit codes",
            description = "Exit codes that should be treated as success. Defaults to only 0. " +
                    "Some tools use non-zero codes for warnings.",
            example = "0,1",
            required = false)
    @Nullable
    List<Integer> acceptableExitCodes;

    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : "Apply CLI tool";
    }

    @Override
    public String getDescription() {
        return description != null ? description :
                "Applies an external CLI tool to modify source files. " +
                        "Supports any executable (Python, Ruby, Go, Bash, etc.).";
    }

    @Override
    protected int getTimeoutMinutes() {
        return timeoutMinutes != null ? timeoutMinutes : super.getTimeoutMinutes();
    }

    @Override
    protected List<Integer> getAcceptableExitCodes() {
        return acceptableExitCodes != null ? acceptableExitCodes : super.getAcceptableExitCodes();
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
    protected Map<String, String> getCommandEnvironment(Accumulator acc, ExecutionContext ctx) {
        Map<String, String> env = new HashMap<>();

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
