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

package io.github.xf8b.xf8bot.api.commands

import io.github.xf8b.xf8bot.api.commands.AbstractCommand.CommandType
import io.github.xf8b.xf8bot.util.LoggerDelegate
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.slf4j.Logger
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.stream.Collectors

/**
 * Used to find commands when they are fired.
 *
 * @author xf8b
 */
class CommandRegistry : AbstractList<AbstractCommand>() {
    override val size: Int
        get() = commandHandlers.size
    private val logger: Logger by LoggerDelegate()
    private val commandHandlers: MutableList<AbstractCommand> = LinkedList()
    var locked = false

    /**
     * Registers the passed in [AbstractCommand].
     *
     * @param command The command to be registered.
     */
    private fun registerCommandHandler(command: AbstractCommand) {
        if (locked) throw UnsupportedOperationException("Registry is currently locked!")
        commandHandlers.add(command)
    }

    fun slurpCommandHandlers(packagePrefix: String) {
        val reflections = Reflections(packagePrefix, SubTypesScanner())
        reflections.getSubTypesOf(AbstractCommand::class.java).forEach {
            try {
                registerCommandHandler(it.getConstructor().newInstance())
            } catch (exception: InstantiationException) {
                logger.error("An error happened while trying to slurp command handlers!", exception)
            } catch (exception: InvocationTargetException) {
                logger.error("An error happened while trying to slurp command handlers!", exception)
            } catch (exception: IllegalAccessException) {
                logger.error("An error happened while trying to slurp command handlers!", exception)
            } catch (exception: NoSuchMethodException) {
                logger.error("An error happened while trying to slurp command handlers!", exception)
            }
        }
        locked = true
    }

    override fun get(index: Int): AbstractCommand {
        return commandHandlers[index]
    }

    override fun iterator(): MutableIterator<AbstractCommand> {
        return commandHandlers.iterator()
    }

    fun <T : AbstractCommand> getCommandHandler(clazz: Class<out T>): T {
        return clazz.cast(commandHandlers.stream()
                .filter { it.javaClass == clazz }
                .findFirst()
                .orElseThrow { IllegalArgumentException("No command matches the class inputted!") })
    }

    fun getCommandHandlersWithCommandType(commandType: CommandType): List<AbstractCommand> {
        return LinkedList(commandHandlers.stream()
                .filter { it.commandType === commandType }
                .collect(Collectors.toUnmodifiableList()))
    }
}