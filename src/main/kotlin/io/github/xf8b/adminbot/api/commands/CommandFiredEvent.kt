/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.api.commands

import com.mongodb.client.model.Filters
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.xf8b.adminbot.AdminBot
import io.github.xf8b.adminbot.api.commands.arguments.Argument
import io.github.xf8b.adminbot.api.commands.flags.Flag
import reactor.core.publisher.Mono
import java.util.*

class CommandFiredEvent(
        val adminBot: AdminBot,
        private val flagMap: Map<Flag<*>, Any>,
        private val argumentsMap: Map<Argument<*>, Any>,
        event: MessageCreateEvent
) : MessageCreateEvent(
        event.client,
        event.shardInfo,
        event.message,
        event.guildId.map(Snowflake::asLong).orElse(null),
        event.member.orElse(null)
) {
    val prefix: Mono<String> = Mono.from(adminBot.mongoDatabase
            .getCollection("prefixes")
            .find(Filters.eq("guildId", guildId.map(Snowflake::asLong).orElse(null))))
            .map { it.get("prefix", String::class.java) }
    val channel: Mono<MessageChannel>
        get() = message.channel
    val author: Optional<User>
        get() = message.author

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfFlag(flag: Flag<T>): Optional<T> = Optional.ofNullable(flagMap[flag] as T?)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfArgument(argument: Argument<T>): Optional<T> = Optional.ofNullable(argumentsMap[argument] as T?)

    override fun toString(): String {
        return "CommandFiredEvent(" +
                "adminBot=$adminBot, " +
                "flagMap=$flagMap, " +
                "argumentsMap=$argumentsMap, " +
                "message=$message, " +
                "guildId=$guildId, " +
                "member=$member" +
                ")"
    }
}
