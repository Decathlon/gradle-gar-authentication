package com.decathlon.android.gradle.authenticatedgarcli

import org.gradle.api.GradleException
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.gradle.api.provider.ProviderFactory

/**
 * [Host] contains methods which exist in [org.gradle.api.initialization.Settings] and
 * [org.gradle.api.Project]. Some methods are common between the 2 types, to simplify the access to
 * theses methods, [Host] implements them and redirect to the right instance of
 * [org.gradle.api.initialization.Settings] or [org.gradle.api.Project] depending on the current
 * implementation used.
 *
 * Officially, only [org.gradle.api.initialization.Settings] type is supported but
 * [org.gradle.api.Project] is also supported to run test. Gradle provides an API for Project
 * testing but no Settings testing API was provided yet.
 */
internal sealed interface Host : PluginAware, ExtensionAware {

    // This method also exists in gradle's Settings.java and Project.java, pattern matching is used
    // here
    fun getProviders(): ProviderFactory
    fun getGradle(): Gradle

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getProvidersKt")
    val providers: ProviderFactory
        get() = getProviders()

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getGradleKt")
    val gradle: Gradle
        get() = getGradle()

    @JvmInline
    value class Settings(val source: org.gradle.api.initialization.Settings) : Host,
        org.gradle.api.initialization.Settings by source {
        override fun include(vararg projectPaths: String) = source.include(*projectPaths)
        override fun includeFlat(vararg projectNames: String) = source.includeFlat(*projectNames)
    }

    @JvmInline
    value class Project(val source: org.gradle.api.Project) : Host,
        org.gradle.api.Project by source

    companion object {
        operator fun invoke(target: Any): Host = when (target) {
            is org.gradle.api.Project -> Project(target.rootProject)
            is org.gradle.api.initialization.Settings -> Settings(target)
            else -> throw GradleException("Cannot handle this gradle type: $target. Only \"Project\" and \"Settings\" are supported")
        }
    }
}