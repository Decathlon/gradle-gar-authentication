package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.gradle.api.provider.ProviderFactory
import java.io.File

/**
 * [Parent] contains methods which exist in [org.gradle.api.initialization.Settings] and
 * [org.gradle.api.Project]. Some methods are common between the 2 types, to simplify the access to
 * theses methods, [Parent] implements them and redirect to the right instance of
 * [org.gradle.api.initialization.Settings] or [org.gradle.api.Project] depending on the current
 * implementation used.
 *
 * Officially, only [org.gradle.api.initialization.Settings] type is supported but
 * [org.gradle.api.Project] is also supported to run test. Gradle provides an API for Project
 * testing but no Settings testing API was provided yet.
 */
internal sealed interface Parent : PluginAware, ExtensionAware {

    // This method also exists in gradle's Settings.java and Project.java, pattern matching is used
    // here
    fun getProviders(): ProviderFactory

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getProvidersKt")
    val providers: ProviderFactory
        get() = getProviders()

    @JvmInline
    private value class Settings(private val settings: org.gradle.api.initialization.Settings) :
        Parent, org.gradle.api.initialization.Settings by settings

    @JvmInline
    private value class Project(private val project: org.gradle.api.Project) : Parent,
        org.gradle.api.Project by project

    companion object {
        operator fun invoke(target: Any): Parent = when (target) {
            // Support for Project type were added to run test, officially, only Settings are supported
            is org.gradle.api.Project -> Project(target)
            is org.gradle.api.initialization.Settings -> Settings(target)
            else -> throw GradleException("Cannot handle this gradle type: $target. Only \"Project\" and \"Settings\" are supported")
        }
    }
}