plugins {
    id("gradlebuild.distribution.api-java")
}

description = "Contains the Java plugin, and its supporting classes.  This plugin is used as the basis for building a Java library or application by more specific plugins, and is sometimes applied by other JVM language projects."

dependencies {
    api(projects.coreApi)
    api(projects.languageJava)
    api(projects.pluginsJvmTestSuite)
    api(projects.publish)

    api(libs.inject)

    implementation(projects.baseServices)
    implementation(projects.core)
    implementation(projects.ivy)
    implementation(projects.languageJvm)
    implementation(projects.logging)
    implementation(projects.maven)
    implementation(projects.modelCore)
    implementation(projects.platformBase)
    implementation(projects.platformJvm)
    implementation(projects.pluginsJavaBase)
    implementation(projects.softwareDiagnostics)
    implementation(projects.stdlibJavaExtensions)
    implementation(projects.testSuitesBase)
    implementation(projects.testingJvm)

    runtimeOnly(projects.dependencyManagement)
    runtimeOnly(projects.testingBase)
    runtimeOnly(projects.toolchainsJvm)

    runtimeOnly(libs.groovy)

    testImplementation(testFixtures(projects.core))

    integTestImplementation(projects.javadoc)

    integTestImplementation(testFixtures(projects.messaging))
    integTestImplementation(testFixtures(projects.enterpriseOperations))
    integTestImplementation(testFixtures(projects.languageJava))
    integTestImplementation(testFixtures(projects.languageJvm))
    integTestImplementation(testFixtures(projects.pluginsJavaBase))
    integTestImplementation(testFixtures(projects.pluginsJvmTestFixtures))
    integTestImplementation(testFixtures(projects.workers))

    integTestDistributionRuntimeOnly(projects.distributionsJvm)
    crossVersionTestDistributionRuntimeOnly(projects.distributionsFull)
}

packageCycles {
    excludePatterns.add("org/gradle/api/plugins/**")
}
