/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.util

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.Event
import discord4j.core.spec.EmbedCreateSpec
import org.reflections.Reflections
import reactor.core.publisher.Flux
import java.time.Instant

fun Member.getTagWithDisplayName(): String = this.displayName + "#" + this.discriminator

fun User.isNotBot() = !isBot

fun EmbedCreateSpec.setTimestampAsNow(): EmbedCreateSpec = this.setTimestamp(Instant.now())

inline fun <reified T> Reflections.getSubTypesOf(): Set<Class<out T>> = getSubTypesOf(T::class.java)

inline fun <reified E : Event> GatewayDiscordClient.on(): Flux<E> = on(E::class.java)

fun String.toSnowflake(): Snowflake = Snowflake.of(this)

fun Long.toSnowflake(): Snowflake = Snowflake.of(this)

fun Instant.toSnowflake(): Snowflake = Snowflake.of(this)

