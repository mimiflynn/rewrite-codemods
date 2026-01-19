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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class ApplyCliToolTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.typeValidationOptions(TypeValidation.all().immutableExecutionContext(false));
    }

    @Test
    void displayNameAndDescriptionDefaults() {
        ApplyCliTool recipe = new ApplyCliTool(null, null, "echo", null, null, null, null, null);
        assertThat(recipe.getDisplayName()).isEqualTo("Apply CLI tool");
        assertThat(recipe.getDescription()).contains("Applies an external CLI tool");
    }

    @Test
    void displayNameAndDescriptionCustom() {
        ApplyCliTool recipe = new ApplyCliTool(
                "Custom Name",
                "Custom description here.",
                "echo",
                null, null, null, null, null
        );
        assertThat(recipe.getDisplayName()).isEqualTo("Custom Name");
        assertThat(recipe.getDescription()).isEqualTo("Custom description here.");
    }

    @Test
    void timeoutConfiguration() {
        ApplyCliTool defaultTimeout = new ApplyCliTool(null, null, "echo", null, null, null, null, null);
        assertThat(defaultTimeout.getTimeoutMinutes()).isEqualTo(5);

        ApplyCliTool customTimeout = new ApplyCliTool(null, null, "echo", null, null, null, 15, null);
        assertThat(customTimeout.getTimeoutMinutes()).isEqualTo(15);
    }

    @Test
    void acceptableExitCodesConfiguration() {
        ApplyCliTool defaultCodes = new ApplyCliTool(null, null, "echo", null, null, null, null, null);
        assertThat(defaultCodes.getAcceptableExitCodes()).containsExactly(0);

        ApplyCliTool customCodes = new ApplyCliTool(null, null, "echo", null, null, null, null, Arrays.asList(0, 1, 2));
        assertThat(customCodes.getAcceptableExitCodes()).containsExactly(0, 1, 2);
    }

    @DocumentExample
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void sedReplaceInFile() {
        rewriteRun(
                spec -> spec.recipe(new ApplyCliTool(
                        "Replace foo with bar",
                        "Uses sed to replace foo with bar in all files.",
                        "sed",
                        Arrays.asList("-i", "s/foo/bar/g", "${repoDir}/test.txt"),
                        null, null, null, null
                )).cycles(1).expectedCyclesThatMakeChanges(1),
                text(
                        "Hello foo world foo!",
                        "Hello bar world bar!",
                        spec -> spec.path("test.txt")
                )
        );
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void bashScriptExecution() {
        rewriteRun(
                spec -> spec.recipe(new ApplyCliTool(
                        null, null,
                        "bash",
                        Arrays.asList("-c", "printf 'modified' > ${repoDir}/test.txt"),
                        null, null, null, null
                )).cycles(1).expectedCyclesThatMakeChanges(1),
                text(
                        "original content",
                        "modified",
                        spec -> spec.path("test.txt")
                )
        );
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void environmentVariables() {
        rewriteRun(
                spec -> spec.recipe(new ApplyCliTool(
                        null, null,
                        "bash",
                        Arrays.asList("-c", "printf '%s' \"$MY_VAR\" > ${repoDir}/test.txt"),
                        null,
                        Collections.singletonList("MY_VAR=hello_world"),
                        null, null
                )).cycles(1).expectedCyclesThatMakeChanges(1),
                text(
                        "original",
                        "hello_world",
                        spec -> spec.path("test.txt")
                )
        );
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void acceptNonZeroExitCode() {
        // grep returns 1 when no matches found, but we want to accept that
        rewriteRun(
                spec -> spec.recipe(new ApplyCliTool(
                        null, null,
                        "bash",
                        Arrays.asList("-c", "grep 'notfound' ${repoDir}/test.txt || printf 'searched' > ${repoDir}/test.txt"),
                        null, null, null,
                        Arrays.asList(0, 1)  // Accept both 0 and 1
                )).cycles(1).expectedCyclesThatMakeChanges(1),
                text(
                        "some content",
                        "searched",
                        spec -> spec.path("test.txt")
                )
        );
    }
}
