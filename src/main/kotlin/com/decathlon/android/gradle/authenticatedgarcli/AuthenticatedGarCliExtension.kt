package com.decathlon.android.gradle.authenticatedgarcli

import com.decathlon.android.gradle.authenticatedgarcli.FetchAuthentication.Data
import org.gradle.api.artifacts.repositories.AuthenticationSupported

public abstract class AuthenticatedGarCliExtension internal constructor(
    private val authData: Data,
) {
    public val token: String
        get() = authData.token

    public fun garCliCredentials(authenticationSupported: AuthenticationSupported): Unit =
        authenticationSupported.credentials {
            username = "oauth2accesstoken"
            password = token
        }
}
