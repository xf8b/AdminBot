/*
 * Copyright (c) 2020, 2021 xf8b.
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

package io.github.xf8b.xf8bot.commands.botadministrator

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import reactor.core.publisher.Mono

class SayCommand : AbstractCommand(
    name = "\${prefix}say",
    description = "Sends the passed in content to the current channel.",
    commandType = CommandType.BOT_ADMINISTRATOR,
    arguments = ImmutableList.of(CONTENT),
    minimumAmountOfArgs = 1,
    botAdministratorOnly = true
) {
    companion object {
        private val CONTENT = StringArgument(
            name = "content",
            index = Range.atLeast(1)
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.channel.flatMap {
        it.createMessage(event.getValueOfArgument(CONTENT).get())
    }.then(event.message.delete())
}