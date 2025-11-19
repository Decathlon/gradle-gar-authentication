package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticateUser.Companion.AuthenticateUser
import com.decathlon.android.gradle.authenticatedgarcli.CliPath.Companion.CliPath
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Companion.FetchAuthentication
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Data
import com.decathlon.android.gradle.authenticatedgarcli.LoggedUser.Companion.LoggedUser
import org.gradle.api.GradleException
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.slf4j.Logger

/**
 * [Host] contains methods which exist in [org.gradle.api.initialization.Settings] and
 * [org.gradle.api.Project]. Some methods are common between the 2 types, to simplify the access to
 * theses methods, [Host] implements them and redirect to the right instance of
 * [org.gradle.api.initialization.Settings] or [org.gradle.api.Project] depending on the current
 * implementation used.
 *
 * Officially, only [org.gradle.api.initialization.Settings] type is supported but
 * [org.gradle.api.Project] is also supported to run test. Gradle provides an API for Project
 * testing but no Settings testing API was provided yet.
 */
internal sealed interface Host : PluginAware, ExtensionAware {

    // This method also exists in gradle's Settings.java and Project.java, pattern matching is used
    // here
    fun getProviders(): ProviderFactory

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getProvidersKt")
    val providers: ProviderFactory
        get() = getProviders()

    @JvmInline
    value class Settings(val source: org.gradle.api.initialization.Settings) : Host,
        org.gradle.api.initialization.Settings by source {
        override fun include(vararg projectPaths: String?) = source.include(*projectPaths)
        override fun includeFlat(vararg projectNames: String?) = source.includeFlat(*projectNames)
    }

    @JvmInline
    value class Project(val source: org.gradle.api.Project) : Host,
        org.gradle.api.Project by source

    val authenticatedGarCliExtension: AuthenticatedGarCliExtension?
        get() = extensions.findByType<AuthenticatedGarCliExtension>()

    fun registerAuthenticatedGarCliExtension(logger: Logger) {
        val cliPath = CliPath().unwrap {
            val installationCmd = with(getCurrentOperatingSystem()) {
                when {
                    isWindows -> "winget install Google.CloudSDK"
                    isMacOsX -> "brew install google-cloud-sdk"
                    isLinux -> "apt-get install google-cloud-cli"
                    else -> null
                }
            }
            StringBuilder("The current machine doesn't have the gcloud CLI installed, ")
                .apply {
                    if (installationCmd != null)
                        append("please run \"$installationCmd\" and")
                }
                .append(" check the CLI is available in your IDE path")
                .toString()
        }
        logger.info("\"gcloud\" CLI path: \"${cliPath.get()}\"")

        val isCI by providers.environmentVariable("CI").orElse("false").map { it == "true" }

        val authData by lazy {
            fun authenticate(): Data =
                if (isCI)
                    throw GradleException("The gcloud cli is not logged, please run \"gcloud auth login\" and follow the instructions. If running on a C.I. you can use \"gcloud auth activate-service-account --key-file=/path/to/service-account.json\"")
                else {
                    AuthenticateUser(cliPath).get()
                    val newUser by LoggedUser(cliPath).unwrap()
                    FetchAuthentication(cliPath, newUser).get().also {
                        logger.info("Authentication successful for user \"${newUser}\"")
                    }
                }

            val currentUser by LoggedUser(cliPath)
            if (currentUser != null) {
                val currentAuthData by FetchAuthentication(cliPath, currentUser!!)
                if (currentAuthData.valid && currentAuthData.expired.not()) {
                    logger.info("Connects to GAR using user \"${currentUser!!}\"")
                    currentAuthData
                } else
                    authenticate()
            } else
                authenticate()
        }

        extensions.create<AuthenticatedGarCliExtension>(
            "AuthenticatedGarCliExtension",
            authData,
        )
    }

    companion object {
        operator fun invoke(target: Any): Host = when (target) {
            is org.gradle.api.Project -> Project(target.rootProject)
            is org.gradle.api.initialization.Settings -> Settings(target)
            else -> throw GradleException("Cannot handle this gradle type: $target. Only \"Project\" and \"Settings\" are supported")
        }
    }
}