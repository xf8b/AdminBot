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
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.music.GuildMusicHandler
import reactor.core.publisher.Mono

class SkipCommand : AbstractCommand(
    name = "\${prefix}skip",
    description = "Skips the current music playing.",
    commandType = CommandType.MUSIC,
    aliases = ImmutableList.of("\${prefix}stop"),
    arguments = ImmutableList.of(AMOUNT_TO_SKIP)
) {
    companion object {
        private val AMOUNT_TO_SKIP = IntegerArgument(
            name = "amount to skip",
            index = Range.singleton(1),
            required = false
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val guildId = context.guildId.get()
        val guildMusicHandler = GuildMusicHandler.get(
            guildId,
            context.xf8bot.audioPlayerManager,
            context.channel.block()!!
        )
        val amountToSkip = context.getValueOfArgument(AMOUNT_TO_SKIP).orElse(1)
        return context.client.voiceConnectionRegistry.getVoiceConnection(guildId)
            .flatMap {
                guildMusicHandler
                    .skip(amountToSkip)
                    .then(context.channel.flatMap {
                        it.createMessage("Successfully skipped the current video!")
                    })
            }
            .switchIfEmpty(context.channel.flatMap { it.createMessage("I am not connected to a VC!") })
            .then()
    }
}