dependencies {
    api(project(":api"))
    compileOnlyApi(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnlyApi(libs.protocol)

    compileOnly(libs.fastutil)
    compileOnly(libs.guava)

    compileOnly(libs.gson)
    testImplementation(libs.gson)

    implementation(libs.fastutil)

    implementation(libs.jackson.yaml)
}

indra {
    val branchName = indraGit.branchName().orNull ?: System.getenv("BRANCH_NAME") ?: "local/dev"
    configurePublications {
        if (branchName !in arrayOf("master", "local/dev")) {
            val parts = branchName.split('/')
            val prefix = parts.getOrNull(0)?.replace(Regex("[^0-9A-Za-z-]"), "-") ?: branchName
            val versionNum = parts.getOrNull(1)?.replace(Regex("[^0-9A-Za-z.-]"), "-") ?: version.toString()
            version = "$prefix-$versionNum-SNAPSHOT"
        }
    }

    publishSnapshotsTo("boar", "https://repo.opencollab.dev/maven-snapshots")
    publishReleasesTo("boar", "https://repo.opencollab.dev/maven-releases")
}

tasks.requireTagged {
    isEnabled = false
}