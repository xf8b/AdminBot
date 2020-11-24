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
                        guildMusicHandler.musicTrackScheduler.queue.clear()
                    }.then(context.channel.flatMap {
                        it.createMessage("Successfully cleared the queue!")
                    })
                }
                .switchIfEmpty(context.channel.flatMap { it.createMessage("I am not connected to a VC!") })
                .then()
        }
    }
}