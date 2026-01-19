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
/**
 * OpenRewrite recipes for integrating external CLI tools.
 * <p>
 * This package provides a framework for wrapping any command-line tool as an OpenRewrite recipe.
 * Supports Node.js tools (via npx), Python tools, Ruby tools, or any other CLI utility.
 * <p>
 * Key classes:
 * <ul>
 *   <li>{@link org.openrewrite.cli.CliBasedRecipe} - Base class for any CLI tool</li>
 *   <li>{@link org.openrewrite.cli.NodeBasedRecipe} - Node.js-specific base class</li>
 *   <li>{@link org.openrewrite.cli.ApplyCliTool} - Generic CLI wrapper recipe</li>
 * </ul>
 */
@NullMarked
@NonNullFields
package org.openrewrite.cli;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
