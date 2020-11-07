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

import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import reactor.core.publisher.Mono

class StopCommand : AbstractCommand(
    name = "\${prefix}stop",
    description = "Stops the music in the current VC.",
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
                guildMusicHandler.stop().then(context.channel.flatMap {
                    it.createMessage("Successfully stopped the current music!")
                })
            }
            .switchIfEmpty(context.channel.flatMap { it.createMessage("I am not in a VC!") })
            .then()
    }
}