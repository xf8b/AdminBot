package io.github.xf8b.xf8bot.commands

import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import reactor.core.publisher.Mono

class SomeoneCommand : AbstractCommand(
        name = "\${prefix}someone",
        description = "Pings a random person.",
        commandType = CommandType.OTHER
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.guild.flatMap { guild ->
        guild.requestMembers()
                .collectList()
                .map {
                    it.shuffle()
                    it[0]
                }
                .flatMap { member -> event.channel.flatMap { it.createMessage(member.nicknameMention) } }
                .then()
    }
}
