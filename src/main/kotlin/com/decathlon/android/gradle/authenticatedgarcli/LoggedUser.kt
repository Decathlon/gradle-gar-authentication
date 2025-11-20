package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticatedGarCliPlugin.Companion.logger
import com.decathlon.android.gradle.authenticatedgarcli.LoggedUser.Parameters
import java.io.ByteArrayOutputStream
import java.util.Optional
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.assign
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException

internal abstract class LoggedUser : ValueSource<Optional<String>, Parameters> {

    interface Parameters : ValueSourceParameters {
        val cliPath: Property<String>
    }

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun obtain(): Optional<String> = ByteArrayOutputStream()
        .use { normalOutput ->
            val errorOutput = ByteArrayOutputStream()
            execOperations.exec {
                commandLineMultiplatform(parameters.cliPath.get(), "auth", "list")
                args("--format", "json")
                isIgnoreExitValue = true
                standardOutput = normalOutput
                setErrorOutput(errorOutput)
            }.also { execResult ->
                errorOutput.use { errorOutput ->
                    if (execResult.exitValue != 0)
                        throw ExecException("Standard output: $normalOutput\nError output: $errorOutput")
                }
            }
            normalOutput.toString()
        }
        .let { Json.decodeFromString<JsonArray>(it) }
        .map { it.jsonObject }
        .firstOrNull { it["status"]!!.jsonPrimitive.content == "ACTIVE" }
        ?.get("account")
        ?.jsonPrimitive
        ?.content
        .also {
            if (it != null) logger.info("Logged user: $it")
            else logger.info("No user logged")
        }
        .let { Optional.ofNullable(it) }
}
