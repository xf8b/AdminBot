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

package io.github.xf8b.xf8bot.commands.settings

import com.google.common.collect.Range
import io.github.xf8b.utils.optional.toNullable
import io.github.xf8b.utils.optional.toOptional
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.database.actions.add.AddDisabledCommandAction
import io.github.xf8b.xf8bot.database.actions.find.FindDisabledCommandAction
import io.github.xf8b.xf8bot.util.hasUpdatedRows
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import reactor.core.publisher.Mono

class DisableCommand : AbstractCommand(
    name = "\${prefix}disable",
    description = "Disables the command specified. Requires level 4.",
    commandType = CommandType.SETTINGS,
    arguments = COMMAND.toSingletonImmutableList(),
    administratorLevelRequired = 4
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val command = event.getValueOfArgument(COMMAND)
            .flatMap { command ->
                event.xf8bot.commandRegistry
                    .find { command1 ->
                        command1.name == "\${prefix}$command"
                                || command1.aliases.any { it == "\${prefix}$command" }
                    }
                    .toOptional()
            }
            .toNullable()

        return event.channel.flatMap { channel ->
            when {
                command == null -> channel.createMessage("Command not found!").then()
                command.botAdministratorOnly -> channel.createMessage("You can't disable bot administrator only commands!")
                    .then()
                command.commandType == CommandType.SETTINGS -> channel.createMessage("You can't disable settings commands!")
                    .then()

                else -> event.xf8bot.botDatabase
                    .execute(FindDisabledCommandAction(event.guildId.get(), command))
                    .filter { it.isNotEmpty() }
                    .filterWhen { it[0].hasUpdatedRows }
                    .flatMap { channel.createMessage("`${command.rawName}` is already disabled!") }
                    .switchIfEmpty(
                        event.xf8bot.botDatabase
                            .execute(AddDisabledCommandAction(event.guildId.get(), command))
                            .then(channel.createMessage("Successfully disabled `${command.rawName}`!"))
                    )
                    .then()
            }
        }
    }

    companion object {
        private val COMMAND = StringArgument(
            name = "command to disable",
            index = Range.singleton(1)
        )
    }
}