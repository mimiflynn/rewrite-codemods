![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)

## Apply External CLI Tools via OpenRewrite Recipes

This repository provides a framework for wrapping external CLI tools (codemods, linters, formatters, etc.) as OpenRewrite recipes. These can be applied from the command line or using the [Moderne platform](https://app.moderne.io/).

The framework supports any executable tool—Node.js-based codemods (jscodeshift, ESLint, Biome, etc.), Python tools (Black, Ruff, etc.), Ruby tools, or any other CLI utility that modifies files.

## Package Structure

The codebase is organized under `org.openrewrite.cli`:

| Class             | Description                                 |
| ----------------- | ------------------------------------------- |
| `CliBasedRecipe`  | Base class for any CLI tool integration     |
| `NodeBasedRecipe` | Node.js-specific base class (npx execution) |
| `ApplyCliTool`    | Generic CLI wrapper (no custom code needed) |
| `ApplyCodemod`    | jscodeshift codemod execution               |
| `ESLint`          | ESLint linter integration                   |
| `Biome`           | Biome formatter/linter integration          |
| `Putout`          | Putout transformation tool                  |

## Architecture

The codebase is organized around a flexible CLI-based recipe framework:

### CliBasedRecipe (Base Class)

A generic base class that supports any command-line tool:

- **Scanning phase**: Serializes all source files to a temporary directory, recreating the repository structure
- **Generate phase**: Executes the CLI command and detects which files were modified (via timestamp comparison)
- **Edit phase**: Reloads modified file content and returns updated PlainText sources

### NodeBasedRecipe (Node.js Specialization)

Extends `CliBasedRecipe` with Node.js-specific features:

- Uses `npx` to execute packages on-demand (no bundled node_modules)
- Parser auto-detection based on file extensions
- Node-specific variable substitution

### ApplyCliTool (Generic CLI Wrapper)

A flexible recipe for executing any CLI tool without writing custom code:

```java
new ApplyCliTool()
    .setCommand("python")
    .setArgs(Arrays.asList("formatter.py", "${repoDir}"))
```

## Implementation Notes

All recipes extending `CliBasedRecipe` operate using a three-phase scanning recipe pattern:

1. **Scanning Phase**: All source files are serialized to disk, preserving the repository structure
2. **Generate Phase**: The CLI tool executes against this directory tree. File modifications are detected by comparing timestamps before and after execution.
3. **Edit Phase**: Modified files are reloaded from disk and returned as PlainText sources

### NPX-Based Execution (Node.js)

**🎯 No bundled dependencies!** Node.js recipes use `npx -y` to execute packages on-demand:

- ✅ Small JAR file (no bundled node_modules)
- ✅ Always use specific pinned versions
- ✅ Automatic package download on first use
- ⚠️ Requires internet connection on first run
- ⚠️ Requires npm/npx installed on the system

Example:

```java
// Executes: npx -y @biomejs/biome@1.9.4 lint ${repoDir} --fix
Arrays.asList("npx", "-y", "@biomejs/biome@1.9.4", "lint", "${repoDir}", "--fix")
```

### Chaining Multiple Recipes

Multiple CLI-based recipes can be chained in a single recipe run:

- Each recipe's output becomes the input to the next
- The generate phase copies the previous recipe's directory before executing
- This works for any combination of CLI tools

### Compatibility with Other Recipes

**✅ FIXED**: CLI-based recipes can now be followed by other recipes in the same run!

Previously, modifications from CLI tools were invisible to subsequent recipes because they occurred in the generate phase. This is now resolved because:

- Modified files are reloaded into the OpenRewrite tree during the edit phase
- Subsequent recipes see the actual modified content (as PlainText)
- This allows seamless integration with other OpenRewrite recipes

### Example: Chaining Tools

```yaml
recipes:
  - org.openrewrite.cli.ESLint:
      patterns: ['**/*.ts']
  - org.openrewrite.cli.Biome # Sees ESLint changes
  - org.openrewrite.java.format.FormattingRecipe # Sees Biome changes
```

## Creating Custom CLI-Based Recipes

### For Node.js Tools

Extend `NodeBasedRecipe` and implement `getNpmCommand()`:

```java
public class MyCodemod extends NodeBasedRecipe {
    protected List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx) {
        return Arrays.asList("npx", "-y", "my-tool@1.0.0", "${repoDir}", "--fix");
    }
}
```

### For Any CLI Tool

Extend `CliBasedRecipe` and implement `getCommand()`:

```java
public class MyTool extends CliBasedRecipe {
    protected List<String> getCommand(Accumulator acc, ExecutionContext ctx) {
        return Arrays.asList("python", "tool.py", "${repoDir}");
    }
}
```

### Supported Variables

- `${repoDir}`: Current working directory (set to ".")
- `${workDir}`: Full path to the temporary working directory
- `${parser}`: (Node.js only) Auto-detected parser (tsx, ts, or babel)

## Supported Tools

### JavaScript/TypeScript

- [jscodeshift](https://github.com/facebook/jscodeshift) - Transform framework
- [ESLint](https://eslint.org/) - JavaScript linter
- [Biome](https://biomejs.dev/) - Formatter and linter
- [Putout](https://github.com/coderaiser/putout) - JavaScript transformation tool
- [@next/codemod](https://www.npmjs.com/package/@next/codemod) - Next.js codemods
- [@mui/codemod](https://github.com/mui/material-ui/tree/master/packages/mui-codemod) - Material-UI codemods

See our documentation on [creating recipes that run ESLint plugins](https://docs.openrewrite.org/authoring-recipes/recipe-with-npm-dependency) for a step-by-step guide.

## Licensing

For more information about licensing, please visit our [licensing page](https://docs.openrewrite.org/licensing/openrewrite-licensing).
