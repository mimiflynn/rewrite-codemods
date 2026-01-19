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
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ApplyCodemod extends NodeBasedRecipe {
    @Option(displayName = "Codemod package",
            description = "NPM package containing the codemod (with optional version), e.g., 'jscodeshift@0.16.1' or '@next/codemod@14.0.4'",
            example = "jscodeshift@0.16.1"
    )
    @Nullable
    String codemodPackage;

    @Option(displayName = "Codemod transform",
            description = "Path to the transform file within the package or local file system.",
            example = "transform.js"
    )
    @Nullable
    String transform;

    @Option(displayName = "File filter",
            description = "Optional glob pattern to filter files to apply the codemod to. Defaults to all files. Note: not all codemods support file glob filtering.",
            example = "**/*.(j|t)sx"
    )
    @Nullable
    String fileFilter;

    @Option(displayName = "Codemod command arguments",
            description = "Arguments which get passed to the codemod command.",
            example = "--force --parser=${parser}",
            required = false)
    @Nullable
    List<String> codemodArgs;

    String displayName = "Applies a codemod to all source files";

    String description = "Applies a codemod using npx to all source files.";

    @Override
    protected List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx) {
        List<String> command = new ArrayList<>();
        command.add("npx");
        command.add("-y");

        // Add the package (defaults to jscodeshift if not specified)
        String pkg = codemodPackage != null ? codemodPackage : "jscodeshift@0.16.1";
        command.add(pkg);

        // Add transform if specified
        if (transform != null) {
            command.add("-t");
            command.add(transform);
        }

        // Add target directory with optional file filter
        String target = "${repoDir}" + (fileFilter != null ? "/" + fileFilter : "");
        command.add(target);

        // Add parser
        command.add("--parser=${parser}");

        // Add additional arguments
        if (codemodArgs != null) {
            command.addAll(codemodArgs);
        }

        return command;
    }
}
