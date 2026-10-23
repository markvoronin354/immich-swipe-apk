@file:Suppress("MatchingDeclarationName")

package com.markvoronin.immichswipe.testutil

import android.content.ContextWrapper
import com.markvoronin.immichswipe.core.SessionManager
import java.lang.reflect.Proxy

/** One recorded call on a fake. For suspend functions the trailing continuation is not included. */
class RecordedCall(val name: String, val args: List<Any?>)

/**
 * Builds a recording stand-in for an interface. Every call is added to [calls]. [handler] decides the
 * result and may throw to simulate a failure. The default result is `Unit`.
 */
inline fun <reified T : Any> fake(
    calls: MutableList<RecordedCall> = mutableListOf(),
    crossinline handler: (name: String, args: List<Any?>) -> Any? = { _, _ -> }
): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, rawArgs ->
    val args = (rawArgs ?: emptyArray()).toList()
    val real = if (args.lastOrNull() is kotlin.coroutines.Continuation<*>) args.dropLast(1) else args
    calls += RecordedCall(method.name, real)
    handler(method.name, real)
} as T

/** A session manager that works in plain JVM tests (no real context, no network). */
fun testSessionManager(): SessionManager = SessionManager(ContextWrapper(null))
