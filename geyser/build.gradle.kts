plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":common"))
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.geyser.api)
    compileOnly(libs.geyser.core) {
        isTransitive = false
    }
    compileOnly(libs.mcprotocollib)
}

// The fat jar is the distributable Geyser extension, not the Maven artifact.
shadow {
    addShadowVariantIntoJavaComponent = false
}

val commitHash = indraGit.commit().map { it.abbreviate(7).name() }.orElse("unknown")
val commitMessage = providers.exec {
    commandLine("git", "log", "-1", "--pretty=%B")
}.standardOutput.asText.map { it.trim() }

modrinth {
    token = System.getenv("MODRINTH_TOKEN")
    versionName = commitHash
    versionNumber = commitHash.map { "${project.version}-$it" }
    changelog = commitMessage
    projectId = "boar"
    versionType = "alpha"
    uploadFile.set(tasks.shadowJar)

    // Don't comment on this :)
    gameVersions = listOf("1.8","1.8.1","1.8.2","1.8.3","1.8.4","1.8.5","1.8.6","1.8.7","1.8.8","1.8.9","1.9","1.9.1","1.9.2","1.9.3","1.9.4","1.10","1.10.1","1.10.2","1.11","1.11.1","1.11.2","1.12","1.12.1","1.12.2","1.13","1.13.1","1.13.2","1.14","1.14.1","1.14.2","1.14.3","1.14.4","1.15","1.15.1","1.15.2","1.16","1.16.1","1.16.2","1.16.3","1.16.4","1.16.5","1.17","1.17.1","1.18","1.18.1","1.18.2","1.19","1.19.1","1.19.2","1.19.3","1.19.4","1.20","1.20.1","1.20.2","1.20.3","1.20.4","1.20.5","1.20.6","1.21","1.21.1","1.21.2","1.21.3","1.21.4","1.21.5","1.21.6","1.21.7","1.21.8","1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2");
    loaders = listOf("geyser")
}

tasks {
    shadowJar {
        archiveFileName = "boar-geyser.jar"

        relocate("it.unimi.dsi.fastutil", "ac.boar.shaded.fastutil")
        relocate("com.fasterxml.jackson", "ac.boar.shaded.jackson")
        relocate("org.yaml.snakeyaml", "ac.boar.shaded.snakeyaml")
    }
}
