package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticatedGarCliPlugin.Companion.logger
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.Optional
import javax.inject.Inject

internal abstract class CliPath : ValueSource<Optional<String>, ValueSourceParameters.None> {

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun obtain(): Optional<String> = ByteArrayOutputStream()
        .use {
            execOperations.exec {
                isIgnoreExitValue = true
                commandLineMultiplatform(
                    if (getCurrentOperatingSystem().isWindows) "where" else "which",
                    "gcloud"
                )
                standardOutput = it
            }.also { execResult ->
                if (execResult.exitValue != 0)
                    return Optional.empty()
            }
            it.toString()
        }
        .trimIndent()
        .takeIf { it.isNotBlank() }
        .let { paths ->
            // Windows "where" method returns multiple paths, only the one which ends with .cmd
            // seems to work
            if (getCurrentOperatingSystem().isWindows)
                paths?.split("\n")
                    ?.singleOrNull { it.endsWith(".cmd") }
            else
                paths
        }
        .also {
            if (it != null) {
                logger.info("\"gcloud\" CLI path: \"$it\"")
            } else {
                logger.info("\"gcloud CLI path not found\"")
            }
        }
        .let { Optional.ofNullable(it) }
}
