package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class ConfigurationCacheTest {

    @Test
    fun `Data equality ignores rotating token when validity and expiration match`() {
        val first = FetchAuthentication.Data(token = "token-1", valid = true, expired = false)
        val second = FetchAuthentication.Data(token = "token-2", valid = true, expired = false)
        val expired = FetchAuthentication.Data(token = "token-2", valid = true, expired = true)
        val invalid = FetchAuthentication.Data(token = "token-2", valid = false, expired = false)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, expired)
        assertNotEquals(first, invalid)
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun `reuses configuration cache when oauth token rotates but invalidates when expired`(
        @TempDir projectDir: File,
    ) {
        val binDir = File(projectDir, "fake-bin").apply { mkdirs() }
        val stateFile = File(projectDir, "auth-state.json")
        val fakeGcloud = File(binDir, "gcloud").apply {
            writeText(
                """
                #!/usr/bin/env sh
                if [ "$1" = "auth" ] && [ "$2" = "list" ]; then
                  echo '[{"account":"ci@example.com","status":"ACTIVE"}]'
                  exit 0
                fi
                if [ "$1" = "auth" ] && [ "$2" = "describe" ]; then
                  cat "${stateFile.absolutePath}"
                  exit 0
                fi
                exit 1
                """.trimIndent(),
            )
            setExecutable(true)
        }
        check(fakeGcloud.canExecute())

        File(projectDir, "settings.gradle.kts").writeText(
            """
            import com.decathlon.android.gradle.authenticatedgarcli.garCliCredentials

            plugins {
                id("com.decathlon.gradle.authenticated-gar-cli")
            }

            dependencyResolutionManagement {
                repositories {
                    maven("https://europe-west1-maven.pkg.dev/example/repo") {
                        garCliCredentials(this)
                    }
                }
            }

            rootProject.name = "cc-test"
            """.trimIndent(),
        )
        File(projectDir, "build.gradle.kts").writeText("")

        val baseEnv = System.getenv().toMutableMap().apply {
            put("CI", "true")
            put("PATH", "${binDir.absolutePath}${File.pathSeparator}${get("PATH").orEmpty()}")
        }

        fun runHelp() = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withEnvironment(baseEnv)
            .withArguments("help", "--configuration-cache")
            .build()

        // 1. First build stores the configuration cache with token-1
        stateFile.writeText("""{"token":"token-1","valid":true,"expired":false}""")
        val firstRun = runHelp()
        assertContains(firstRun.output, "Configuration cache entry stored.")

        // 2. Second build rotates the token to token-2 -> configuration cache MUST be reused
        stateFile.writeText("""{"token":"token-2","valid":true,"expired":false}""")
        val secondRun = runHelp()
        assertContains(secondRun.output, "Reusing configuration cache.")

        // 3. Third build marks the token as expired -> configuration cache MUST be invalidated (and fail in CI)
        stateFile.writeText("""{"token":"token-3","valid":true,"expired":true}""")
        val thirdRun = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withEnvironment(baseEnv)
            .withArguments("help", "--configuration-cache")
            .buildAndFail()
        assertContains(thirdRun.output, "because a build logic input of type 'FetchAuthentication' has changed")
    }
}
