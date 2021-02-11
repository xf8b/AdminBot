/*
 * Copyright (c) 2020, 2021 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

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