plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
    alias(libs.plugins.version.catalog.update)
}

group = "io.hexlet"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("io.hexlet.flightbooking.App")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.javalin)
    implementation(libs.jacksonDatabind)
    implementation(libs.hikariCp)
    implementation(libs.postgresql)
    implementation(libs.slf4jSimple)

    testImplementation(libs.javalinTesttools)
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testImplementation(libs.assertjCore)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.shadowJar {
    archiveFileName.set("app.jar")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        googleJavaFormat().aosp()
        endWithNewline()
    }
}

versionCatalogUpdate {
    sortByKey = false
}
