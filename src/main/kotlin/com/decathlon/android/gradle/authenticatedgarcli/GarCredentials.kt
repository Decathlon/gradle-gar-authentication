package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.AuthenticationSupported
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware

public fun Project.garCliCredentials(authenticationSupported: AuthenticationSupported): Unit =
    Host(this).garCliCredentials(authenticationSupported)

public fun Settings.garCliCredentials(authenticationSupported: AuthenticationSupported): Unit =
    Host(this).garCliCredentials(authenticationSupported)

private fun Host.garCliCredentials(authenticationSupported: AuthenticationSupported): Unit =
    authenticatedGarCliExtension
        ?.garCliCredentials(authenticationSupported)
        ?: throw GradleException("Cannot fetch credentials for gar-cli, gradle extension is missing. Apply the plugin to your project or settings file before using this method")

@Deprecated("This method is deprecated, replace by extensions for types \"Project\" and \"Settings\"")
public fun ExtensionAware.garCliCredentials(authenticationSupported: AuthenticationSupported): Unit =
    Host(this)
        .authenticatedGarCliExtension
        ?.garCliCredentials(authenticationSupported)
        ?: throw GradleException("Cannot fetch credentials for gar-cli, gradle extension is missing. Did you apply the plugin ?")

