package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticateUser.Companion.AuthenticateUser
import com.decathlon.android.gradle.authenticatedgarcli.CliPath.Companion.CliPath
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Companion.FetchAuthentication
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Data
import com.decathlon.android.gradle.authenticatedgarcli.LoggedUser.Companion.LoggedUser
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.kotlin.dsl.create
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public abstract class AuthenticatedGarCliPlugin : Plugin<Any> {

    private val logger by lazy { LoggerFactory.getLogger(AuthenticatedGarCliPlugin::class.java) }

    override fun apply(target: Any): Unit = with(Parent(target)) {
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

        val autoLogin = providers.provider { (System.getenv("CI") == "true").not() }

        // `locks` avoids multiples login with concurrency
        val lock = ReentrantLock()

        extensions.create<AuthenticatedGarCliExtension>(
            "AuthenticatedGarCliExtension",
            providers.provider {
                fun authenticate(): Data =
                    if (autoLogin.get().not())
                        throw GradleException("The gcloud cli is not logged, please run \"gcloud auth login\" and follow the instructions. If running on a C.I. you can use \"gcloud auth activate-service-account --key-file=/path/to/service-account.json\"")
                    else {
                        AuthenticateUser(cliPath).get()
                        val newUser by LoggedUser(cliPath).unwrap()
                        FetchAuthentication(cliPath, newUser).get().also {
                            logger.info("Authentication successful for user \"${newUser}\"")
                        }
                    }
                lock.withLock {
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
            },
        )
    }
}
