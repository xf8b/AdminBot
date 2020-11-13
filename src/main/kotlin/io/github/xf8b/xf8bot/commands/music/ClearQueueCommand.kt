package io.github.xf8b.xf8bot.commands.music

import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import reactor.core.publisher.Mono

class ClearQueueCommand : AbstractCommand(
    name = "\${prefix}clearqueue",
    description = "Clears the queue of songs to play",
    commandType = CommandType.MUSIC
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val guildId = context.guildId.get()
        return context.channel.flatMap { channel ->
            val guildMusicHandler = GuildMusicHandler.get(
                guildId,
                context.xf8bot.audioPlayerManager,
                channel
            )
            context.client.voiceConnectionRegistry.getVoiceConnection(guildId)
                .flatMap {
                    Mono.fromRunnable<Void> {
                        for (i in 0..guildMusicHandler.musicTrackScheduler.queue.size) {
                            guildMusicHandler.musicTrackScheduler.queue.poll()
                        }
                    }.then(context.channel.flatMap {
                        it.createMessage("Successfully cleared the queue!")
                    })
                }
                .switchIfEmpty(context.channel.flatMap { it.createMessage("I am not connected to a VC!") })
                .then()
        }
    }
}