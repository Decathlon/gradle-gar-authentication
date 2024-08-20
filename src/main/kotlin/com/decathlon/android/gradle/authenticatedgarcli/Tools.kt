package com.decathlon.android.gradle.authenticatedgarcli

import java.util.Optional
import kotlin.reflect.KProperty
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.gradle.process.ExecSpec

// System specific which matches this doc https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html
internal fun ExecSpec.commandLineMultiplatform(vararg args: Any) {
    if (getCurrentOperatingSystem().isWindows) commandLine(*listOf("cmd", "/c").plus(args).toTypedArray())
    else commandLine(*args)
}

internal inline fun <reified T : Any> Provider<Optional<T>>.unwrap(crossinline lazyMessage: () -> Any): Provider<T> = map {
    it.orElseThrow { GradleException(lazyMessage().toString()) }
}

internal inline fun <reified T : Any> Provider<Optional<T>>.unwrap(): Provider<T> = map {
    it.orElseThrow { throw GradleException() }
}

internal inline operator fun <reified T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()

@JvmName("getValueOptional")
internal inline operator fun <reified T> Provider<Optional<T>>.getValue(thisRef: Any?, property: KProperty<*>): T? = get().orElse(null)