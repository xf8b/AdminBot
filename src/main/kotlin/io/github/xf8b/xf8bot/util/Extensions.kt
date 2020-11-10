/*
 * Copyright (c) 2020 xf8b.
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
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.Event
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import org.reflections.Reflections
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

// extension fields
val Member.tagWithDisplayName: String
    get() = "${this.displayName}#${this.discriminator}"

val User.isNotBot: Boolean
    get() = !isBot

// extension functions
fun EmbedCreateSpec.setTimestampToNow(): EmbedCreateSpec = this.setTimestamp(Instant.now())

// to functions
// to snowflake
fun String.toSnowflake(): Snowflake = Snowflake.of(this)

fun Long.toSnowflake(): Snowflake = Snowflake.of(this)

fun Instant.toSnowflake(): Snowflake = Snowflake.of(this)

// to mono
fun <T> T?.toMono(): Mono<T> = Mono.justOrEmpty(this)

// to collection
fun Permission.toSingletonPermissionSet(): PermissionSet = PermissionSet.of(this)

fun <T> T.toSingletonImmutableList(): ImmutableList<T> = ImmutableList.of(this)

fun <T> Double<T, T>.toImmutableList(): ImmutableList<T> = ImmutableList.of(first, second)

fun <T> Triple<T, T, T>.toImmutableList(): ImmutableList<T> = ImmutableList.of(first, second, third)

// functions purely for using reified type parameters
inline fun <reified T : AbstractCommand> CommandRegistry.getCommandWithType(): T = getCommand(T::class.java)

inline fun <reified T> Reflections.getSubTypesOf(): Set<Class<out T>> = getSubTypesOf(T::class.java)

inline fun <reified E : Event> GatewayDiscordClient.on(): Flux<E> = on(E::class.java)

