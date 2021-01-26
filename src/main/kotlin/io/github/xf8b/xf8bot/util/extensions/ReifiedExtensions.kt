package io.github.xf8b.xf8bot.util.extensions

import net.jodah.typetools.TypeResolver
import org.reflections.Reflections
import reactor.core.publisher.Mono
import java.util.function.Function

// reflection
inline fun <reified T> Reflections.getSubTypesOf(): Set<Class<out T>> = getSubTypesOf(T::class.java)
inline fun <reified T> Class<out T>.resolveRawArgument(): Class<*> =
    TypeResolver.resolveRawArgument(T::class.java, this)

// mono error
inline fun <reified E : Throwable, T> Mono<T>.onErrorResume(fallback: Function<in E, out Mono<out T>>): Mono<T> =
    onErrorResume(E::class.java, fallback)

// collection cast
inline fun <reified T> Iterable<*>.cast() = this.map { element -> element as T }
inline fun <reified T> Array<*>.cast() = this.map { element -> element as T }.toTypedArray()