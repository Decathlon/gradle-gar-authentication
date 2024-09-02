@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    signing
    alias(libs.plugins.gradle.publish)
}

// Replaces the default configuration applied by KotlinDslCompilerPlugins.kt
kotlinDslPluginOptions {
    tasks.withType<KotlinCompile> {
        compilerOptions {
            // Uses the embedded kotlin api and language version instead of the default language
            // version from the KotlinDSL plugin
            apiVersion = KotlinVersion.DEFAULT
            languageVersion = KotlinVersion.DEFAULT
            freeCompilerArgs.addAll(
                "-Xexplicit-api=strict",
                "-opt-in=kotlin.RequiresOptIn",
                // "-Xcontext-receivers", On 19/06/2024 context receivers still work bad with gradle, do not use
            )
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

group = "com.decathlon.gradle"
version = "0.1.0-alpha01"
val releaseVersion = version.toString().endsWith("-SNAPSHOT").not()

java {
    withJavadocJar()
    withSourcesJar()
}

gradlePlugin {
    website = "https://github.com/Decathlon/gradle-gar-authentication"
    vcsUrl = "https://github.com/Decathlon/gradle-gar-authentication.git"
    plugins.create("authenticated-gar-cli") {
        id = "com.decathlon.gradle.authenticated-gar-cli"
        displayName = "Authenticated Gar Cli"
        description =
            "Gradle plugin to connect to a private Google Artifacts Registry using gcloud CLI"
        implementationClass =
            "com.decathlon.android.gradle.authenticatedgarcli.AuthenticatedGarCliPlugin"
        tags = listOf("gar", "gcloud", "google", "artifacts", "registry")
    }
}

signing {
    setRequired {
        // signing is required if this is a release version and the artifacts are to be published
        // do not use hasTask() as this require realization of the tasks that maybe are not necessary
        releaseVersion && gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
    }
    val signingKeyId: String? = System.getenv("GPG_KEY_ID")
    val signingKey: String? = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword: String? = System.getenv("GPG_PASSPHRASE")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(tasks["jar"])
}
