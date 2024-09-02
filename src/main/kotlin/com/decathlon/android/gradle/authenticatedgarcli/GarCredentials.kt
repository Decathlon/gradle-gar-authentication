package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.artifacts.repositories.AuthenticationSupported
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.the

public fun ExtensionAware.garCliCredentials(parent: AuthenticationSupported): Unit =
    the<AuthenticatedGarCliExtension>().garCliCredentials(parent)
