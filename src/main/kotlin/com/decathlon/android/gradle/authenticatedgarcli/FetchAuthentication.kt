package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.AuthenticatedGarCliPlugin.Companion.logger
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Data
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.Optional
import javax.inject.Inject

internal abstract class FetchAuthentication : ValueSource<Optional<Data>, Parameters> {

    interface Parameters : ValueSourceParameters {
        val cliPath: Property<String>
        val user: Property<String>
    }

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun obtain(): Optional<Data> = ByteArrayOutputStream()
        .use { output ->
            execOperations.exec {
                commandLineMultiplatform(
                    parameters.cliPath.get(),
                    "auth",
                    "describe",
                    parameters.user.get()
                )
                args("--format", "json")
                isIgnoreExitValue = true
                standardOutput = output
            }.also { result ->
                if (result.exitValue != 0) {
                    logger.info("Failed to run auth describe command")
                    return Optional.empty()
                }
            }
            output.toString()
        }
        .let { Json.decodeFromString<JsonObject>(it) }
        .let(::Data)
        .also { logger.info("Authentification data fetch: $it") }
        .let(Optional<Data>::of)

    class Data(val token: String, val valid: Boolean, val expired: Boolean) {
        // `source` contains much more data than `valid`, `expired` or `token` but only these
        // three are required for the plugin.
        // `token` is intentionally excluded from `equals` and `hashCode`: OAuth access tokens
        // rotate periodically (and on every CI runner), which would otherwise invalidate the
        // Gradle configuration cache on every build even when authentication remains valid.
        constructor(source: JsonObject) : this(
            source["token"]!!.jsonPrimitive.content,
            source["valid"]!!.jsonPrimitive.boolean,
            source["expired"]!!.jsonPrimitive.boolean,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Data) return false
            return valid == other.valid && expired == other.expired
        }

        override fun hashCode(): Int = 31 * valid.hashCode() + expired.hashCode()

        override fun toString(): String = "Data(valid=$valid, expired=$expired)"
    }
}
