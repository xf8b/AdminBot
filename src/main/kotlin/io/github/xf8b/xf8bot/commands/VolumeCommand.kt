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

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.Member
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.audio.GuildMusicHandler
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import reactor.core.publisher.Mono

class VolumeCommand : AbstractCommand(
        name = "\${prefix}volume",
        description = "Changes the volume of the music in the current VC.",
        commandType = CommandType.MUSIC,
        minimumAmountOfArgs = 1,
        arguments = ImmutableList.of(VOLUME)
) {
    companion object {
        private val VOLUME = IntegerArgument.builder()
                .setName("volume")
                .setIndex(Range.singleton(1))
                .setValidityPredicate { value ->
                    try {
                        val level = value.toInt()
                        level in 0..400
                    } catch (exception: NumberFormatException) {
                        false
                    }
                }
                .setInvalidValueErrorMessageFunction { invalidValue ->
                    try {
                        val level = invalidValue.toInt()
                        when {
                            level > 400 -> "The maximum volume is 400!"
                            level < 0 -> "The minimum volume is 1!"
                            else -> throw ThisShouldNotHaveBeenThrownException()
                        }
                    } catch (exception: NumberFormatException) {
                        Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                    }
                }
                .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = Mono.defer {
        val guildId = event.guild.map { it.id }.block()!!
        val guildMusicHandler = GuildMusicHandler.getMusicHandler(
                guildId,
                event.xf8bot.audioPlayerManager,
                event.channel.block()!!
        )
        val volume = event.getValueOfArgument(VOLUME).get()
        event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
                .flatMap {
                    guildMusicHandler.setVolume(volume).then(event.channel.flatMap {
                        it.createMessage("Successfully set volume to $volume!")
                    })
                }
                .switchIfEmpty(Mono.justOrEmpty(event.member)
                        .flatMap(Member::getVoiceState)
                        .flatMap(VoiceState::getChannel)
                        .flatMap {
                            event.channel.flatMap { it.createMessage("I am not in a VC!") }
                        }
                        .switchIfEmpty(event.channel.flatMap {
                            it.createMessage("You are not in a VC!")
                        }))
    }.then()
}