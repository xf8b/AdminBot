package io.github.xf8b.adminbot.api.commands

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
    val channel: Mono<MessageChannel>
        get() = message.channel
    val author: Optional<User>
        get() = message.author

    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOfFlag(flag: Flag<T>): Optional<T> = Optional.ofNullable(flagMap[flag] as T?)

    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOfArgument(argument: Argument<T>): Optional<T> = Optional.ofNullable(argumentsMap[argument] as T?)

    override fun toString(): String {
        return "CommandFiredEvent(adminBot=$adminBot, flagMap=$flagMap, argumentsMap=$argumentsMap, message=$message, guildId=$guildId, member=$member)"
    }
}
