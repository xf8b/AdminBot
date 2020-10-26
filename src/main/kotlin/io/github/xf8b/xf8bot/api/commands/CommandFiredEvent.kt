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

package io.github.xf8b.xf8bot.api.commands

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.xf8b.utils.optional.toOptional
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import reactor.core.publisher.Mono
import java.util.*

class CommandFiredEvent(
    val xf8bot: Xf8bot,
    private val flagMap: Map<Flag<*>, Any>,
    private val argumentsMap: Map<Argument<*>, Any>,
    event: MessageCreateEvent
) : MessageCreateEvent(
    event.client,
    event.shardInfo,
    event.message,
    event.guildId.map(Snowflake::asLong).toValueOrNull(),
    event.member.toValueOrNull()
) {
    val prefix: Mono<String> = if (guildId.isEmpty) {
        Mono.empty()
    } else {
        xf8bot.prefixCache.getPrefix(guildId.get())
    }
    val channel: Mono<MessageChannel>
        get() = message.channel
    val author: Optional<User>
        get() = message.author

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfFlag(flag: Flag<T>): Optional<T> = (flagMap[flag] as T?).toOptional()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfArgument(argument: Argument<T>): Optional<T> = (argumentsMap[argument] as T?).toOptional()

    override fun toString(): String {
        return "CommandFiredEvent(" +
                "xf8bot=$xf8bot, " +
                "flagMap=$flagMap, " +
                "argumentsMap=$argumentsMap, " +
                "message=$message, " +
                "guildId=$guildId, " +
                "member=$member" +
                ")"
    }
}
