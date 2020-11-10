package io.github.xf8b.xf8bot.commands.music

import discord4j.rest.util.Color
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import io.github.xf8b.xf8bot.util.toImmutableList
import org.apache.commons.lang3.time.DurationFormatUtils
import reactor.core.publisher.Mono

class CurrentlyPlayingCommand : AbstractCommand(
    name = "\${prefix}currentlyplaying",
    description = "Gets the currently playing music.",
    commandType = CommandType.MUSIC,
    aliases = ("\${prefix}np" to "\${prefix}nowplaying" and "\${prefix}playing").toImmutableList()
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val guildId = context.guildId.get()
        return context.channel.flatMap { channel ->
            val guildMusicHandler = GuildMusicHandler.get(
                guildId,
                context.xf8bot.audioPlayerManager,
                channel
            )

            guildMusicHandler.currentlyPlaying.flatMap { currentlyPlaying ->
                channel.createEmbed {
                    it.setTitle("Currently Playing Song")
                        .addField("Song", currentlyPlaying.info.title, true)
                        .addField(
                            "Duration",
                            DurationFormatUtils.formatDurationHMS(currentlyPlaying.info.length),
                            false
                        )
                        .addField(
                            "Current Position",
                            DurationFormatUtils.formatDurationHMS(currentlyPlaying.position),
                            true
                        )
                        .setColor(Color.BLUE)
                        .setUrl(currentlyPlaying.info.uri)
                }
            }.switchIfEmpty(channel.createMessage("No songs are currently playing."))
        }.then()
    }
}