plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    id("com.github.node-gradle.node") version "latest.release"
    id("org.openrewrite.build.moderne-source-available-license") version "latest.release"
}

// Set as appropriate for your organization
group = "org.openrewrite.recipe"
description = "Apply external CLI tools (codemods, linters, formatters, etc.) via OpenRewrite recipes"

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:$rewriteVersion"))

    implementation("org.openrewrite:rewrite-core")

    testImplementation("org.openrewrite:rewrite-test")
}

license {
    exclude("**/package.json")
    exclude("**/package-lock.json")
}

node {
    nodeProjectDir.set(file("build/resources/main/codemods"))
    download.set(true)
    version.set("20.18.1")
}

tasks.named("npmInstall") {
    dependsOn(tasks.named("processResources"))
}

tasks.named("classes") {
    dependsOn(tasks.named("npmInstall"))
}
