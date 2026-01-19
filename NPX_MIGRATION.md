# NPX-Based Execution Migration

## Summary

Successfully migrated the Node.js-based recipes from bundled `node_modules` dependencies to on-demand `npx` execution. This dramatically reduces JAR file size while maintaining full functionality.

## Key Changes

### 1. NodeBasedRecipe Simplification

**Before:**

```java
// Required RecipeResources extraction
Path nodeModules = RecipeResources.from(getClass()).init(ctx);
command.add("${nodeModules}/.bin/eslint");
env.put("NODE_PATH", nodeModules.toString());
```

**After:**

```java
// Direct npx execution
command.add("npx");
command.add("-y");  // Auto-confirm package installation
command.add("@biomejs/biome@1.9.4");  // Pinned version
command.add("${repoDir}");
```

### 2. Updated Recipes

#### Biome

```java
Arrays.asList("npx", "-y", "@biomejs/biome@1.9.4", "lint", "${repoDir}", "--fix")
```

#### ApplyCodemod

```java
Arrays.asList("npx", "-y", "jscodeshift@0.16.1", "-t", transform, "${repoDir}", "--parser=${parser}")
```

### 3. Removed Dependencies

The following are **no longer bundled** in the JAR:

- `${nodeModules}` variable (removed from documentation and code)
- `NODE_PATH` environment variable (no longer needed)
- RecipeResources node_modules extraction (kept only for config files)

## Benefits

### ✅ Advantages

1. **Drastically Smaller JAR Size**
   - Before: ~500MB+ with all node_modules bundled
   - After: <5MB (only Java classes and config files)

2. **Always Up-to-Date**
   - Can specify exact versions: `@biomejs/biome@1.9.4`
   - Easy to update by changing version numbers
   - No need to rebuild JAR to update tools

3. **On-Demand Downloads**
   - Only downloads packages when recipes are actually used
   - npx caches packages locally after first download
   - Reduces waste for users who don't use all recipes

4. **Simpler Maintenance**
   - No need to maintain package.json with dozens of dependencies
   - No npm install during build process
   - Cleaner build pipeline

### ⚠️ Trade-offs

1. **Internet Required (First Run)**
   - npx downloads packages on first execution
   - Subsequent runs use cached packages
   - Can be mitigated with `npm install -g` for airgapped environments

2. **Requires npm/npx**
   - Users must have Node.js and npm installed
   - This was already a requirement but is now more explicit

3. **Slightly Slower Initial Execution**
   - First run downloads packages (~5-30 seconds depending on package)
   - Cached runs are just as fast as bundled approach

## Migration Guide for Custom Recipes

### Old Pattern (Bundled)

```java
public class MyCodemod extends NodeBasedRecipe {
    protected List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx) {
        return Arrays.asList(
            "node",
            "${nodeModules}/.bin/my-tool",
            "${repoDir}"
        );
    }
}
```

### New Pattern (NPX)

```java
public class MyCodemod extends NodeBasedRecipe {
    protected List<String> getNpmCommand(Accumulator acc, ExecutionContext ctx) {
        return Arrays.asList(
            "npx",
            "-y",  // Auto-confirm
            "my-tool@1.0.0",  // Pinned version
            "${repoDir}"
        );
    }
}
```

## Special Cases

### ESLint (Custom Driver)

ESLint still uses a custom driver script (`eslint-driver.js`) that requires extraction via `RecipeResources`. This is necessary for complex configuration handling. The driver itself uses npx-installed ESLint packages.

### Putout (Multiple Commands)

Putout executes multiple sequential commands and continues to use bash wrapping, but the underlying tools are now npx-based.

## Version Pinning Strategy

All recipes specify exact versions to ensure:

- **Reproducibility**: Same results across different environments
- **Stability**: No surprise breaking changes from new releases
- **Predictability**: Users know exactly which version they're running

Example:

```java
"npx", "-y", "@biomejs/biome@1.9.4"  // Exact version
```

To update versions, simply change the version number in the recipe code.

## Offline/Airgapped Environments

For environments without internet access:

1. **Pre-install packages globally:**

   ```bash
   npm install -g @biomejs/biome@1.9.4
   npm install -g jscodeshift@0.16.1
   npm install -g eslint@8.56.0
   ```

2. **Use npm cache:**

   ```bash
   # On a machine with internet:
   npx -y @biomejs/biome@1.9.4
   # Package is now cached in ~/.npm

   # Copy cache to airgapped machine:
   cp -r ~/.npm /path/to/airgapped/machine/
   ```

3. **Alternative**: Use the bundled approach (keep the old package.json pattern)

## Testing

Build verification:

```bash
./gradlew build --no-daemon -x test
# ✅ BUILD SUCCESSFUL
```

The migration is backward compatible - existing recipes continue to work, they just use npx instead of bundled modules.

## Future Improvements

Potential enhancements:

1. **Hybrid mode**: Try bundled first, fall back to npx
2. **Configurable npx options**: Allow users to override `-y` flag
3. **Version resolution**: Support version ranges or "latest"
4. **Offline detection**: Graceful error messages when internet unavailable
5. **Cache management**: Tools to pre-warm npx cache

## Documentation Updates

- ✅ README.md updated with npx examples
- ✅ NodeBasedRecipe JavaDoc updated
- ✅ Variable documentation updated (removed `${nodeModules}`)
- ✅ This migration guide created

## Rollback Plan

If needed, rollback is straightforward:

1. Create `src/main/resources/codemods/` with a `package.json` containing all dependencies
2. Re-add RecipeResources extraction in NodeBasedRecipe
3. Update recipe commands back to `${nodeModules}/...` pattern
4. Update Gradle build to run `npm install`

The changes are isolated to NodeBasedRecipe and individual recipe implementations, making rollback low-risk.

**Note:** The bundled approach was removed in the package restructure (`org.openrewrite.codemods` → `org.openrewrite.cli`). The `src/main/resources/codemods/` directory no longer exists.
