package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticateUser.Parameters
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.assign
import org.gradle.process.ExecOperations
import javax.inject.Inject

internal abstract class AuthenticateUser : ValueSource<Unit, Parameters> {

    interface Parameters : ValueSourceParameters {
        val cliPath: Property<String>
    }

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun obtain(): Unit? = execOperations
        .exec {
            commandLineMultiplatform(parameters.cliPath.get(), "auth", "login")
            isIgnoreExitValue = true
        }.let { execResult ->
            if (execResult.exitValue != 0)
                throw GradleException("Failed to login the gcloud cli")
        }

    @Suppress("FunctionName")
    companion object {
        fun Parent.AuthenticateUser(cliPath: Provider<String>): Provider<Unit> = providers
            .of(AuthenticateUser::class.java) { parameters.cliPath = cliPath }
    }
}
