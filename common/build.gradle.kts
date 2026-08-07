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
    val buildNumber = System.getenv("BUILD_NUMBER") ?: "local"
    val isSnapshot = version.toString().endsWith("-SNAPSHOT")
    configurePublications {
        if (branchName !in arrayOf("master", "local/dev")) {
            version = isSnapshot.let { "$version-$buildNumber" }.takeIf { !isSnapshot } ?: version.toString()
        }
    }

    publishSnapshotsTo("boar", "https://repo.opencollab.dev/maven-snapshots")
    publishReleasesTo("boar", "https://repo.opencollab.dev/maven-releases")
}

tasks.requireTagged {
    isEnabled = false
}