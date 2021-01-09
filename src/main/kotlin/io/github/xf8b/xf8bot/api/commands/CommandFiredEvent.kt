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

package io.github.xf8b.xf8bot.api.commands

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.xf8b.utils.optional.toNullable
import io.github.xf8b.utils.optional.toOptional
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.util.toMono
import reactor.core.publisher.Mono
import java.util.*

class CommandFiredEvent(
    event: MessageCreateEvent,
    val xf8bot: Xf8bot,
    private val flagToValueMap: Map<Flag<*>, Any>,
    private val argumentToValueMap: Map<Argument<*>, Any>
) : MessageCreateEvent(
    event.client,
    event.shardInfo,
    event.message,
    event.guildId.map(Snowflake::asLong).toNullable(),
    event.member.toNullable()
) {
    val prefix: Mono<String> get() = guildId.toMono().flatMap { xf8bot.prefixCache.get(it) }
    val channel: Mono<MessageChannel> get() = message.channel
    val author: Optional<User> get() = message.author

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfFlagNullable(flag: Flag<T>) = flagToValueMap[flag] as T?

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfFlag(flag: Flag<T>) = getValueOfFlagNullable(flag).toOptional()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfArgumentNullable(argument: Argument<T>) = argumentToValueMap[argument] as T?

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfArgument(argument: Argument<T>) = getValueOfArgumentNullable(argument).toOptional()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandFiredEvent

        if (client != other.client) return false
        if (xf8bot != other.xf8bot) return false
        if (message != other.message) return false
        if (guildId != other.guildId) return false
        if (member != other.member) return false
        if (flagToValueMap != other.flagToValueMap) return false
        if (argumentToValueMap != other.argumentToValueMap) return false
        if (prefix != other.prefix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = client.hashCode()

        result = 31 * result + xf8bot.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + guildId.hashCode()
        result = 31 * result + member.hashCode()
        result = 31 * result + flagToValueMap.hashCode()
        result = 31 * result + argumentToValueMap.hashCode()
        result = 31 * result + prefix.hashCode()

        return result
    }

    override fun toString() = "CommandFiredContext(message=$message, guildId=$guildId, member=$member)"
}