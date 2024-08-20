package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.testfixtures.ProjectBuilder
import java.io.BufferedReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Theses test were designed to run on the C.I. but you can run them on your local machine. When
 * running on your local machine, follow the instruction sent into the standard output.
 */
internal class AuthenticatedGarCliPluginTest {

    private fun test() = with(ProjectBuilder.builder().build()) {
        plugins.apply("com.decathlon.gradle.authenticated-gar-cli")
        repositories {
            // This repo doesn't require authentication, it doesn't matter because the most
            // important in this test is to authenticate and assert the authentication data is
            // correctly sent to the server, no matter if the authentication data is used by the
            // server itself
            maven("https://repo1.maven.org/maven2/") {
                garCliCredentials(this)
            }
        }
        // Forces to query "https://repo1.maven.org/maven2/" by downloading a dependency
        configurations
            .create("download")
            .also { dependencies.add(it.name, "org.slf4j:slf4j-api:2.0.13@jar") }
            .singleFile
        this
    }

    @Test
    fun `apply plugin`(): Unit = with(ProjectBuilder.builder().build()) {
        apply(plugin = "com.decathlon.gradle.authenticated-gar-cli")
        assert(plugins.hasPlugin("com.decathlon.gradle.authenticated-gar-cli"))
        extensions.getByType<AuthenticatedGarCliExtension>()
    }

    @Test
    fun `use credentials`() {
        revokeAuth()
        if (System.getenv("CI") == "true") {
            authenticateWithServiceAccount()
        } else {
            println("Login with your google account to pass this test")
        }
        test()
    }

    @Test
    fun `no credentials`() {
        revokeAuth()
        if (System.getenv("CI") != "true") {
            println("Do NOT log with your google account, click \"cancel\" button when asking")
        }
        assertFailsWith<GradleException> {
            test()
        }
    }

    private fun revokeAuth() {
        ProcessBuilder("gcloud", "auth", "revoke", "--all").start().waitFor()
    }

    private fun authenticateWithServiceAccount() {
        lateinit var errorReader: BufferedReader
        File.createTempFile("service-account", "json")
            .apply { writeText(System.getenv("SERVICE_ACCOUNT_JSON")) }
            .let { serviceAccount ->
                ProcessBuilder(
                    "gcloud",
                    "auth",
                    "activate-service-account",
                    "--key-file=$serviceAccount"
                )
            }
            .start()
            .apply { errorReader = errorReader() }
            .waitFor()
            .also {
                if (it != 0)
                    throw RuntimeException("Failed to authenticate with file, error logs: ${errorReader.readText()}")
            }
    }

}