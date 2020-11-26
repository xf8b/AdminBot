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

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.VoiceState
import discord4j.core.`object`.entity.Member
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import reactor.core.publisher.Mono

class VolumeCommand : AbstractCommand(
    name = "\${prefix}volume",
    description = "Changes the volume of the music in the current VC.",
    commandType = CommandType.MUSIC,
    minimumAmountOfArgs = 1,
    arguments = ImmutableList.of(VOLUME)
) {
    companion object {
        private val VOLUME = IntegerArgument(
            name = "volume",
            index = Range.singleton(1),
            validityPredicate = { value ->
                try {
                    val level = value.toInt()
                    level in 0..400
                } catch (exception: NumberFormatException) {
                    false
                }
            },
            errorMessageFunction = { invalidValue ->
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
            },
            required = false
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.channel.flatMap { channel ->
        val guildId = event.guildId.get()
        val guildMusicHandler = GuildMusicHandler.get(
            guildId,
            event.xf8bot.audioPlayerManager,
            channel
        )
        val volume = event.getValueOfArgument(VOLUME).toValueOrNull()

        if (volume == null) {
            event.channel.flatMap {
                it.createMessage("The current volume is ${guildMusicHandler.volume}.")
            }
        } else {
            event.client.voiceConnectionRegistry.getVoiceConnection(guildId)
                .flatMap {
                    guildMusicHandler.volume = volume

                    event.channel.flatMap {
                        it.createMessage("Successfully set volume to $volume!")
                    }
                }
                .switchIfEmpty(Mono.justOrEmpty(event.member)
                    .flatMap(Member::getVoiceState)
                    .flatMap(VoiceState::getChannel)
                    .flatMap {
                        event.channel.flatMap { it.createMessage("I am not in a VC!") }
                    }
                    .switchIfEmpty(event.channel.flatMap {
                        it.createMessage("You are not in a VC!")
                    })
                )
        }
    }.then()
}