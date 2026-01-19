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

import java.util.List;

/**
 * @deprecated Use {@link org.openrewrite.cli.ApplyCodemod} instead. This class will be removed in a future release.
 */
@Deprecated
@Value
@EqualsAndHashCode(callSuper = true)
public class ApplyCodemod extends org.openrewrite.cli.ApplyCodemod {

    public ApplyCodemod(
            String transform,
            @Nullable String executable,
            @Nullable String fileFilter,
            @Nullable List<String> codemodArgs) {
        super(transform, executable, fileFilter, codemodArgs);
    }
}
