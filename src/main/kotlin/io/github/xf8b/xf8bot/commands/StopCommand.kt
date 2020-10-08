package io.github.xf8b.xf8bot.commands

import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.audio.GuildMusicHandler
import reactor.core.publisher.Mono

class StopCommand : AbstractCommand(
        name = "\${prefix}stop",
        description = "Stops the music in the current VC.",
        commandType = CommandType.MUSIC
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId = event.guildId.get()
        val guildMusicHandler = GuildMusicHandler.getMusicHandler(
                guildId,
                event.xf8bot.audioPlayerManager,
                event.channel.block()!!
        )
        return event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
                .flatMap {
                    guildMusicHandler.stop().then(event.channel.flatMap {
                        it.createMessage("Successfully stopped the current music!")
                    })
                }
                .switchIfEmpty(event.channel.flatMap { it.createMessage("I am not in a VC!") })
                .then()
    }
}