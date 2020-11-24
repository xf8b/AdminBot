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

package io.github.xf8b.xf8bot.api.commands

import io.github.xf8b.xf8bot.api.commands.AbstractCommand.CommandType
import java.util.*
import java.util.stream.Collectors

/**
 * Used to find commands when they are fired.
 *
 * @author xf8b
 */
class CommandRegistry : Registry<AbstractCommand>() {
    /**
     * Registers the passed in [AbstractCommand].
     *
     * @param t the [AbstractCommand] to be registered
     */
    override fun register(t: AbstractCommand) {
        if (registered.any { it.name == t.name }) {
            throw IllegalArgumentException("Cannot register two commands with the same name!")
        }
        super.register(t)
    }

    fun getCommandsWithCommandType(commandType: CommandType): List<AbstractCommand> = LinkedList(
        registered.stream()
            .filter { it.commandType === commandType }
            .collect(Collectors.toUnmodifiableList())
    )
}