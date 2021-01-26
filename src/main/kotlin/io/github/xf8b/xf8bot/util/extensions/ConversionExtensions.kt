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

import com.google.common.collect.ImmutableList
import discord4j.common.util.Snowflake
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

// to snowflake
fun String.toSnowflake(): Snowflake = Snowflake.of(this)

fun Long.toSnowflake(): Snowflake = Snowflake.of(this)

fun Instant.toSnowflake(): Snowflake = Snowflake.of(this)

// to mono
fun <T> Optional<T>.toMono(): Mono<T> = Mono.justOrEmpty(this)

fun (() -> Unit).toMono(): Mono<Void> = Mono.fromRunnable(this)

fun Runnable.toMono(): Mono<Void> = Mono.fromRunnable(this)

// to collection
fun Permission.toSingletonPermissionSet(): PermissionSet = PermissionSet.of(this)

fun <T> T.toSingletonImmutableList(): ImmutableList<T> = ImmutableList.of(this)

fun <T> Pair<T, T>.toImmutableList(): ImmutableList<T> = ImmutableList.of(first, second)

fun <T> Triple<T, T, T>.toImmutableList(): ImmutableList<T> = ImmutableList.of(first, second, third)

// to uuid
fun String.toUuid(): UUID = UUID.fromString(this)