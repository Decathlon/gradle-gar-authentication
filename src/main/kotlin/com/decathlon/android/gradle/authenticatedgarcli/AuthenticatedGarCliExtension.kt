package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Data
import org.gradle.api.artifacts.repositories.AuthenticationSupported
import org.gradle.api.provider.Provider

public abstract class AuthenticatedGarCliExtension internal constructor(
    private val authData: Provider<Data>,
) {
    public val token: Provider<String>
        get() = authData.map { it.token }

    public fun garCliCredentials(parent: AuthenticationSupported): Unit = with(parent) {
        credentials {
            username = "oauth2accesstoken"
            password = token.get()
        }
    }
}
