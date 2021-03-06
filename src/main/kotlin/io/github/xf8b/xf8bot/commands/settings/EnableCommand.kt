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

package io.github.xf8b.xf8bot.commands.settings

import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.database.actions.delete.RemoveDisabledCommandAction
import io.github.xf8b.xf8bot.database.actions.find.FindDisabledCommandAction
import io.github.xf8b.xf8bot.util.extensions.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.extensions.updatedRows
import reactor.core.publisher.Mono

// TODO: add embed for all the disabled commands
class EnableCommand : Command(
    name = "\${prefix}enable",
    description = "Enables the command specified. Requires level 4.",
    commandType = CommandType.SETTINGS,
    arguments = COMMAND.toSingletonImmutableList(),
    administratorLevelRequired = 4
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val command = event.xf8bot.commandRegistry.find { command ->
            command.rawName == event[COMMAND]!! || command.aliases.any { it.removePrefix("\${prefix}") == event[COMMAND]!! }
        }

        return event.channel.flatMap { channel ->
            if (command == null) {
                channel.createMessage("Command not found!").then()
            } else {
                event.xf8bot.botDatabase
                    .execute(FindDisabledCommandAction(event.guildId.get(), command))
                    .singleOrEmpty()
                    .filterWhen { result -> result.updatedRows }
                    .flatMap {
                        event.xf8bot.botDatabase
                            .execute(RemoveDisabledCommandAction(event.guildId.get(), command))
                            .then(channel.createMessage("Successfully enabled `${command.rawName}`!"))
                    }
                    .switchIfEmpty(channel.createMessage("`${command.rawName}` is not disabled!"))
                    .then()
            }
        }
    }

    companion object {
        private val COMMAND = StringArgument(
            name = "command to enable",
            index = Range.singleton(0)
        )
    }
}