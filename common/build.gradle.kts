dependencies {
    api(project(":api"))
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnlyApi(libs.protocol)

    compileOnly(libs.guava)

    compileOnly(libs.gson)
    testImplementation(libs.gson)

    implementation(libs.fastutil)

    implementation(libs.jackson.yaml)
}
