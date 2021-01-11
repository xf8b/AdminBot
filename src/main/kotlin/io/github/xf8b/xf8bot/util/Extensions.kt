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

package io.github.xf8b.xf8bot.util

import com.google.common.collect.ImmutableList
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import io.r2dbc.spi.Result
import org.reflections.Reflections
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.util.function.*
import java.time.Instant
import java.util.*
import java.util.function.Function

// extension fields
val User.isNotBot get() = !isBot

// to functions
// to snowflake
fun String.toSnowflake(): Snowflake = Snowflake.of(this)

fun Long.toSnowflake(): Snowflake = Snowflake.of(this)

fun Instant.toSnowflake(): Snowflake = Snowflake.of(this)

// to mono
@JvmName("optionalToMono")
fun <T> Optional<T>.toMono(): Mono<T> = Mono.justOrEmpty(this)

// to collection
fun Permission.toSingletonPermissionSet(): PermissionSet = PermissionSet.of(this)

fun <T> T.toSingletonImmutableList(): ImmutableList<T> = ImmutableList.of(this)

fun <T> Double<T, T>.toImmutableList(): ImmutableList<T> = ImmutableList.of(first, second)

fun <T> Triple<T, T, T>.toImmutableList(): ImmutableList<T> = ImmutableList.of(first, second, third)

// to uuid
fun String.toUuid(): UUID = UUID.fromString(this)

// functions purely for using reified type parameters
inline fun <reified T> Reflections.getSubTypesOf(): Set<Class<out T>> = getSubTypesOf(T::class.java)
inline fun <reified E : Throwable, T> Mono<T>.onErrorResume(fallback: Function<in E, out Mono<out T>>): Mono<T> =
    onErrorResume(E::class.java, fallback)

// reduce boilerplate
fun <I, R> functionReturning(returnedValue: R): Function<I, R> = Function { returnedValue }

val Result.updatedRows: Mono<Boolean> get() = this.rowsUpdated.toMono().map { it != 0 }

inline fun <reified T> Iterable<*>.cast() = this.map { element -> element as T }

inline fun <reified T> Array<*>.cast() = this.map { element -> element as T }.toTypedArray()

val Long.Companion.JAVA_TYPE get() = java.lang.Long::class.java

// increase clarity
operator fun <A, B> Tuple2<A, B>.component1(): A = this.t1
operator fun <A, B> Tuple2<A, B>.component2(): B = this.t2
operator fun <A, B, C> Tuple3<A, B, C>.component3(): C = this.t3
operator fun <A, B, C, D> Tuple4<A, B, C, D>.component4(): D = this.t4
operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component5(): E = this.t5
operator fun <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F>.component6(): F = this.t6
operator fun <A, B, C, D, E, F, G> Tuple7<A, B, C, D, E, F, G>.component7(): G = this.t7
operator fun <A, B, C, D, E, F, G, H> Tuple8<A, B, C, D, E, F, G, H>.component8(): H = this.t8