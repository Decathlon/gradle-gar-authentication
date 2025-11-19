package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.Plugin
import org.slf4j.LoggerFactory

public abstract class AuthenticatedGarCliPlugin : Plugin<Any> {

    private val logger by lazy { LoggerFactory.getLogger(AuthenticatedGarCliPlugin::class.java) }

    override fun apply(target: Any): Unit = with(Host(target)) {
        if (authenticatedGarCliExtension == null)
            registerAuthenticatedGarCliExtension(logger)
    }
}
