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

package io.github.xf8b.xf8bot.commands

import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.Member
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.audio.GuildMusicHandler
import reactor.core.publisher.Mono

class JoinCommand : AbstractCommand(
    name = "\${prefix}join",
    description = "Joins your current VC.",
    commandType = CommandType.MUSIC
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId = event.guildId.get()
        val guildMusicHandler = GuildMusicHandler.getMusicHandler(
            guildId,
            event.xf8bot.audioPlayerManager,
            event.channel.block()!!
        )
        event.guild.flatMap<Void> {
            it.voiceStates
            Mono.empty()
        }
        return event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .flatMap {
                event.channel.flatMap { it.createMessage("I am already connected to a VC!") }
            }
            .switchIfEmpty(Mono.justOrEmpty(event.member)
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap { voiceChannel ->
                    voiceChannel.join { spec ->
                        spec.setProvider(guildMusicHandler.lavaPlayerAudioProvider)
                    }.then(event.channel.flatMap {
                        it.createMessage("Successfully connected to your VC!")
                    })
                }
                .switchIfEmpty(event.channel.flatMap { it.createMessage("You are not in a VC!") })
            )
            .then()
    }
}