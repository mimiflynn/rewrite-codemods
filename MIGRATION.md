# Migration Guide

This document describes how to migrate from the old `org.openrewrite.codemods` package to the new `org.openrewrite.cli` package.

## Automated Migration

The easiest way to migrate is to run our automated migration recipe:

```bash
# Using Maven
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-codemods:LATEST \
  -Drewrite.activeRecipes=org.openrewrite.cli.migrate.UpgradeToCliPackage

# Using Gradle
./gradlew rewriteRun -Drewrite.activeRecipe=org.openrewrite.cli.migrate.UpgradeToCliPackage
```

This will automatically:

- Update all YAML recipe references from `org.openrewrite.codemods.*` to `org.openrewrite.cli.*`
- Update Java imports if you have custom recipes extending the codemods classes

## Overview

The package has been renamed and restructured to support **any CLI tool**, not just Node.js-based codemods. The new architecture is more generic and flexible.

| Old Package                | New Package           |
| -------------------------- | --------------------- |
| `org.openrewrite.codemods` | `org.openrewrite.cli` |

## Breaking Changes

### 1. Package Rename

All recipe classes have been moved from `org.openrewrite.codemods` to `org.openrewrite.cli`:

| Old Recipe Name                         | New Recipe Name                    |
| --------------------------------------- | ---------------------------------- |
| `org.openrewrite.codemods.ESLint`       | `org.openrewrite.cli.ESLint`       |
| `org.openrewrite.codemods.Biome`        | `org.openrewrite.cli.Biome`        |
| `org.openrewrite.codemods.Putout`       | `org.openrewrite.cli.Putout`       |
| `org.openrewrite.codemods.ApplyCodemod` | `org.openrewrite.cli.ApplyCodemod` |
| `org.openrewrite.codemods.ReactI18Next` | `org.openrewrite.cli.ReactI18Next` |
| `org.openrewrite.codemods.UI5`          | `org.openrewrite.cli.UI5`          |

### 2. NPX-Based Execution

Node.js recipes now use **npx** to execute packages on-demand instead of bundling dependencies in the JAR:

**Before:**

- ~500MB JAR with bundled `node_modules`
- Works offline after download
- `${nodeModules}` variable available

**After:**

- <5MB JAR (no bundled dependencies)
- Requires internet on first run (packages are cached)
- `${nodeModules}` variable removed
- Requires npm/npx installed on the system

### 3. New Prerequisites

The following must be installed on machines running these recipes:

- **Node.js** (v18 or later recommended)
- **npm/npx** (comes with Node.js)
- **Internet access** (first run only, packages are cached)

## Backwards Compatibility

### Temporary Aliases

To ease migration, the old recipe names still work via declarative YAML aliases:

```yaml
# This still works (but is deprecated)
- org.openrewrite.codemods.ESLint

# But you should update to:
- org.openrewrite.cli.ESLint
```

The old names will continue to work but will be **removed in a future release**.

### Declarative Recipe Migration

For YAML-based recipes, update the recipe names:

**Before:**

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.example.MyLintRecipe
recipeList:
  - org.openrewrite.codemods.ESLint:
      fix: true
      rules:
        - 'no-unused-vars: error'
```

**After:**

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.example.MyLintRecipe
recipeList:
  - org.openrewrite.cli.ESLint:
      fix: true
      rules:
        - 'no-unused-vars: error'
```

### Java Recipe Migration

For Java-based recipes extending `NodeBasedRecipe`:

**Before:**

```java
import org.openrewrite.codemods.NodeBasedRecipe;

public class MyCodemod extends NodeBasedRecipe {
    @Override
    protected List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx) {
        return Arrays.asList(
            "node",
            "${nodeModules}/.bin/my-tool",
            "${repoDir}"
        );
    }
}
```

**After:**

```java
import org.openrewrite.cli.NodeBasedRecipe;

public class MyCodemod extends NodeBasedRecipe {
    @Override
    protected List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx) {
        return Arrays.asList(
            "npx", "-y",
            "my-tool@1.0.0",  // Pinned version
            "${repoDir}"
        );
    }
}
```

Key changes:

1. Import from `org.openrewrite.cli` instead of `org.openrewrite.codemods`
2. Use `npx -y package@version` instead of `${nodeModules}/.bin/package`
3. Pin package versions for reproducibility

## New Features

The new architecture includes several improvements:

### Generic CLI Tool Support

You can now run **any CLI tool**, not just Node.js tools:

```yaml
- org.openrewrite.cli.ApplyCliTool:
    displayName: Format Python with Black
    command: black
    args:
      - '${repoDir}'
```

### Configurable Timeout

```yaml
- org.openrewrite.cli.ApplyCliTool:
    command: slow-tool
    args: ['${repoDir}']
    timeoutMinutes: 30 # Default is 5 minutes
```

### Configurable Exit Codes

Some tools use non-zero exit codes for warnings:

```yaml
- org.openrewrite.cli.ApplyCliTool:
    command: my-linter
    args: ['${repoDir}']
    acceptableExitCodes: [0, 1] # Accept 0 (success) and 1 (warnings)
```

## Troubleshooting

### "npx: command not found"

Install Node.js which includes npx:

```bash
# macOS
brew install node

# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Windows
choco install nodejs
```

### "Network error" on first run

Recipes download npm packages on first execution. Ensure internet access, or pre-install packages:

```bash
# Pre-install packages for offline use
npm install -g eslint@8.56.0
npm install -g @biomejs/biome@1.9.4
npm install -g jscodeshift@0.16.1
```

### Recipes don't find changes made by previous CLI recipes

This issue has been **fixed** in this release. CLI-based recipes now properly chain together, with each recipe seeing changes made by previous recipes.

## Timeline

| Version    | Status                                  |
| ---------- | --------------------------------------- |
| Current    | Old names work via aliases (deprecated) |
| Next Major | Old names removed, migration required   |

We recommend updating your recipes to use the new `org.openrewrite.cli` package names as soon as possible.
