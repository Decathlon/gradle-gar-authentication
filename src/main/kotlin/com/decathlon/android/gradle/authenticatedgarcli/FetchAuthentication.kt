package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Data
import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.assign
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

internal abstract class FetchAuthentication : ValueSource<Data, Parameters> {

    interface Parameters : ValueSourceParameters {
        val cliPath: Property<String>
        val user: Property<String>
    }

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun obtain(): Data = ByteArrayOutputStream()
        .use { output ->
            execOperations.exec {
                commandLineMultiplatform(parameters.cliPath.get(), "auth", "describe", parameters.user.get())
                args("--format", "json")
                standardOutput = output
            }
            output.toString()
        }
        .let { Json.decodeFromString<JsonObject>(it) }
        .let(::Data)

    data class Data(val token: String, val valid: Boolean, val expired: Boolean) {
        // `source` contains much more data than `valid`, `expired` or `token` but only theses
        // three are required for the plugin
        // Do not use `source` as a field property for `Data` because each time
        // `gcloud auth describe` is called the returned json is different from the previous.
        // As consequence for this, the returned value for the method `obtain()` will be different
        // for every build which breaks configuration cache and slow down the build process.
        constructor(source: JsonObject) : this(
            source["token"]!!.jsonPrimitive.toString(),
            source["valid"]!!.jsonPrimitive.boolean,
            source["expired"]!!.jsonPrimitive.boolean,
        )
    }

    @Suppress("FunctionName")
    companion object {
        fun Parent.FetchAuthentication(
            cliPath: Provider<String>,
            loggedUser: Provider<String>,
        ): Provider<Data> = providers.of(FetchAuthentication::class.java) {
            parameters {
                this.cliPath = cliPath
                this.user = loggedUser
            }
        }

        fun Parent.FetchAuthentication(
            cliPath: Provider<String>,
            loggedUser: String,
        ): Provider<Data> = providers.of(FetchAuthentication::class.java) {
            parameters {
                this.cliPath = cliPath
                this.user = loggedUser
            }
        }
    }
}
