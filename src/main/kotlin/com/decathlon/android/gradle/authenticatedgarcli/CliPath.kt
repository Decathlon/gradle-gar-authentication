package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticatedGarCliPlugin.Companion.logger
import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

internal abstract class CliPath : ValueSource<String, ValueSourceParameters.None> {

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun obtain(): String = ByteArrayOutputStream()
        .use { stdOut ->
            execOperations.exec {
                commandLineMultiplatform(
                    if (getCurrentOperatingSystem().isWindows) "where" else "which",
                    "gcloud"
                )
                isIgnoreExitValue = true
                standardOutput = stdOut
            }.let { result ->
                if (result.exitValue != 0) null
                else stdOut.toString()
            }
        }
        ?.trimIndent()
        ?.takeIf { it.isNotBlank() }
        ?.let { paths ->
            // Windows "where" method returns multiple paths, only the one which ends with .cmd
            // seems to work
            if (getCurrentOperatingSystem().isWindows)
                paths.split("\n")
                    .singleOrNull { it.endsWith(".cmd") }
            else
                paths
        }
        ?.also { logger.info("\"gcloud\" CLI path: \"$it\"") }
        ?: throw GradleException(
            with(getCurrentOperatingSystem()) {
                when {
                    isWindows -> "winget install Google.CloudSDK"
                    isMacOsX -> "brew install google-cloud-sdk"
                    isLinux -> "apt-get install google-cloud-cli"
                    else -> null
                }
            }.let { installationCmd ->
                StringBuilder("The current machine doesn't have the gcloud CLI installed, ")
                    .apply {
                        if (installationCmd != null)
                            append("please run \"$installationCmd\" and")
                    }
                    .append(" check the CLI is available in your IDE path")
                    .toString()
            }
        )
}
