package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.GradleException
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.assign
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import javax.inject.Inject
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

internal abstract class TokenService : BuildService<BuildServiceParameters.None> {

    @get:Inject
    abstract val providers: ProviderFactory

    private val isCI by providers.environmentVariable("CI").orElse("false").map { it == "true" }
    private val cliPath = providers.of(CliPath::class.java) {}.map {
        it.getOrNull()
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

    //
    val token = providers
        .of(LoggedUser::class.java) { parameters.cliPath = cliPath }
        .get()
        .getOrNull()
        ?.let { loggedUser ->
            providers.of(FetchAuthentication::class.java) {
                parameters {
                    this.cliPath = this@TokenService.cliPath
                    this.user = loggedUser
                }
            }
        }
        ?.get()
        ?.takeIf { it.valid && it.expired.not() }
        ?.token
        ?: if (isCI) {
            throw GradleException("The gcloud cli is not logged, please run \"gcloud auth login\" and follow the instructions. If running on a C.I. you can use \"gcloud auth activate-service-account --key-file=/path/to/service-account.json\"")
        } else {
            providers
                .of(AuthenticateUser::class.java) {
                    parameters.cliPath = this@TokenService.cliPath
                }
                .get()
            val newUser = providers
                .of(LoggedUser::class.java) { parameters.cliPath = this@TokenService.cliPath }
                .get()
                .getOrElse { throw GradleException() }
            providers
                .of(FetchAuthentication::class.java) {
                    parameters {
                        this.cliPath = this@TokenService.cliPath
                        this.user = newUser
                    }
                }
                .get()
                .token
        }
}