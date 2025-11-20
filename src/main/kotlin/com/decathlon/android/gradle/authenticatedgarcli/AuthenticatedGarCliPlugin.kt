package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.Plugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.registerIfAbsent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public abstract class AuthenticatedGarCliPlugin : Plugin<Any> {

    override fun apply(target: Any): Unit = with(Host(target)) {
        if (extensions.findByType<AuthenticatedGarCliExtension>() == null) {
            extensions.create<AuthenticatedGarCliExtension>(
                "AuthenticatedGarCliExtension",
                gradle
                    .sharedServices
                    .registerIfAbsent("TokenService", TokenService::class) {}
                    .map { it.token }
            )
        }
    }

    internal companion object {
        val logger: Logger by lazy { LoggerFactory.getLogger(AuthenticatedGarCliPlugin::class.java) }
    }
}
