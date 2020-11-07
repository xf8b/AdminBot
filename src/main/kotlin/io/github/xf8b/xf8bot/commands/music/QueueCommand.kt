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

package io.github.xf8b.xf8bot.commands.music

import discord4j.rest.util.Color
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import io.github.xf8b.xf8bot.util.setTimestampToNow
import reactor.core.publisher.Mono

class QueueCommand : AbstractCommand(
    name = "\${prefix}queue",
    description = "Gets the music queue.",
    commandType = CommandType.MUSIC
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val guildId = context.guildId.get()
        val guildMusicHandler = GuildMusicHandler.get(
            guildId,
            context.xf8bot.audioPlayerManager,
            context.channel.block()!!
        )
        return context.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .flatMap {
                context.channel.flatMap { channel ->
                    channel.createEmbed { spec ->
                        spec.setTitle("Queue")
                            .setColor(Color.BLUE)
                            .setTimestampToNow()
                        if (guildMusicHandler.musicTrackScheduler.queue.isEmpty()) {
                            spec.addField("Songs", "No songs", true)
                        } else {
                            spec.addField(
                                "Songs", guildMusicHandler.musicTrackScheduler
                                    .queue
                                    .take(6)
                                    .joinToString(separator = "\n", limit = 6) {
                                        "- ${it.info.title}"
                                    }, true
                            )
                        }
                    }
                }
            }
            .switchIfEmpty(context.channel.flatMap { it.createMessage("I am not connected to a VC!") })
            .then()
    }
}