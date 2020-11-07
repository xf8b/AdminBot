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

package io.github.xf8b.xf8bot.commands.settings

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import reactor.core.publisher.Mono

class PrefixCommand : AbstractCommand(
    name = "\${prefix}prefix",
    description = "Sets the prefix to the specified prefix.",
    commandType = CommandType.SETTINGS,
    minimumAmountOfArgs = 1,
    arguments = ImmutableList.of(NEW_PREFIX),
    administratorLevelRequired = 4
) {
    companion object {
        private val NEW_PREFIX = StringArgument(
            name = "prefix",
            index = Range.atLeast(1),
            required = false
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val channelMono: Mono<MessageChannel> = context.channel
        val guildId = context.guildId.orElseThrow { ThisShouldNotHaveBeenThrownException() }
        val previousPrefix = context.prefix.block()!!
        val newPrefix = context.getValueOfArgument(NEW_PREFIX)
        val xf8bot = context.xf8bot
        return when {
            // reset prefix
            newPrefix.isEmpty -> xf8bot.prefixCache.set(guildId, Xf8bot.DEFAULT_PREFIX).then(channelMono.flatMap {
                it.createMessage("Successfully reset prefix.")
            }).then()

            previousPrefix == newPrefix.get() -> channelMono.flatMap {
                it.createMessage("You can't set the prefix to the same thing, silly.")
            }.then()

            // set prefix
            else -> xf8bot.prefixCache.set(guildId, newPrefix.get()).then(channelMono.flatMap {
                it.createMessage("Successfully set prefix from $previousPrefix to ${newPrefix.get()}.")
            }).then()
        }
    }
}