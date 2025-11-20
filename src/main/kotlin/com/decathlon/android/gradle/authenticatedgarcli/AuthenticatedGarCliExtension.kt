package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.artifacts.repositories.AuthenticationSupported
import org.gradle.api.provider.Provider

public abstract class AuthenticatedGarCliExtension internal constructor(
    token: Provider<String>,
) {
    public val token: String by token

    public fun garCliCredentials(authenticationSupported: AuthenticationSupported): Unit =
        authenticationSupported.credentials {
            username = "oauth2accesstoken"
            password = token
        }
}
