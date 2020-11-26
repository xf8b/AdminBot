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
import io.github.xf8b.utils.optional.toOptional
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.database.actions.delete.RemoveDisabledCommandAction
import io.github.xf8b.xf8bot.database.actions.find.FindDisabledCommandAction
import io.github.xf8b.xf8bot.util.hasUpdatedRows
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.extra.bool.logicalAnd

// TODO: add embed for all the disabled commands
class EnableCommand : AbstractCommand(
    name = "\${prefix}enable",
    description = "Enables the command specified. Requires level 4.",
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
            .toValueOrNull()

        return event.channel.flatMap { channel ->
            if (command == null) {
                channel.createMessage("Command not found!").then()
            } else {
                event.xf8bot.botDatabase
                    .execute(FindDisabledCommandAction(event.guildId.get(), command))
                    .filterWhen { it.isNotEmpty().toMono().logicalAnd(it[0].hasUpdatedRows) }
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
            index = Range.singleton(1)
        )
    }
}