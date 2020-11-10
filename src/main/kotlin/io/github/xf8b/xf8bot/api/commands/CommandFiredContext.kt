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
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.gateway.ShardInfo
import io.github.xf8b.utils.optional.toOptional
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.util.toMono
import reactor.core.publisher.Mono
import java.util.*

class CommandFiredContext(
    val client: GatewayDiscordClient,
    val shardInfo: ShardInfo,
    val xf8bot: Xf8bot,
    val message: Message,
    val guildId: Optional<Snowflake>,
    val member: Optional<Member>,
    private val flagMap: Map<Flag<*>, Any>,
    private val argumentsMap: Map<Argument<*>, Any>
) {
    companion object {
        fun of(
            xf8bot: Xf8bot,
            event: MessageCreateEvent,
            flagMap: Map<Flag<*>, Any>,
            argumentsMap: Map<Argument<*>, Any>
        ) = CommandFiredContext(
            event.client,
            event.shardInfo,
            xf8bot,
            event.message,
            event.guildId,
            event.member,
            flagMap,
            argumentsMap
        )
    }

    val prefix: Mono<String> = guildId.let {
        if (it.isEmpty) Mono.empty()
        else xf8bot.prefixCache.get(it.get())
    }
    val channel: Mono<MessageChannel>
        get() = message.channel
    val author: Optional<User>
        get() = message.author
    val guild: Mono<Guild>
        get() = guildId.toValueOrNull()
            .toMono()
            .flatMap(client::getGuildById)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfFlag(flag: Flag<T>): Optional<T> = (flagMap[flag] as T?).toOptional()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValueOfArgument(argument: Argument<T>): Optional<T> = (argumentsMap[argument] as T?).toOptional()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandFiredContext

        if (client != other.client) return false
        if (xf8bot != other.xf8bot) return false
        if (message != other.message) return false
        if (guildId != other.guildId) return false
        if (member != other.member) return false
        if (flagMap != other.flagMap) return false
        if (argumentsMap != other.argumentsMap) return false
        if (prefix != other.prefix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = client.hashCode()
        result = 31 * result + xf8bot.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + guildId.hashCode()
        result = 31 * result + member.hashCode()
        result = 31 * result + flagMap.hashCode()
        result = 31 * result + argumentsMap.hashCode()
        result = 31 * result + prefix.hashCode()
        return result
    }

    override fun toString(): String = "CommandFiredContext(" +
            "message=$message, " +
            "guildId=$guildId, " +
            "member=$member, " +
            "flagMap=$flagMap, " +
            "argumentsMap=$argumentsMap, " +
            "prefix=$prefix" +
            ")"
}