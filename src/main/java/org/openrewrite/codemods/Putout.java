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

import java.util.Set;

/**
 * @deprecated Use {@link org.openrewrite.cli.Putout} instead. This class will be removed in a future release.
 */
@Deprecated
@Value
@EqualsAndHashCode(callSuper = true)
public class Putout extends org.openrewrite.cli.Putout {

    public Putout(@Nullable Set<String> rules, @Nullable String printer) {
        super(rules, printer);
    }
}
