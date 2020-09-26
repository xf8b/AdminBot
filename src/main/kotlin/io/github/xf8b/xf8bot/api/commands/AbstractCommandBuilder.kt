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

import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag

//only here for java compatibility
class AbstractCommandBuilder(
        name: String? = null,
        usage: String? = null,
        description: String? = null,
        commandType: AbstractCommand.CommandType? = null,
        actions: MutableMap<String, String> = HashMap(),
        aliases: MutableList<String> = ArrayList(),
        minimumAmountOfArgs: Int = 0,
        flags: MutableList<Flag<*>> = ArrayList(),
        arguments: MutableList<Argument<*>> = ArrayList(),
        botRequiredPermissions: PermissionSet = PermissionSet.none(),
        administratorLevelRequired: Int = 0,
        isBotAdministratorOnly: Boolean = false,
) {
    var name: String? = name
        private set
    var usage: String? = usage
        private set
    var description: String? = description
        private set
    var commandType: AbstractCommand.CommandType? = commandType
        private set
    var actions: MutableMap<String, String> = actions
        private set
    var aliases: MutableList<String> = aliases
        private set
    var minimumAmountOfArgs: Int = minimumAmountOfArgs
        private set
    var flags: MutableList<Flag<*>> = flags
        private set
    var arguments: MutableList<Argument<*>> = arguments
        private set
    var botRequiredPermissions: PermissionSet = botRequiredPermissions
        private set
    var administratorLevelRequired: Int = administratorLevelRequired
        private set
    var isBotAdministratorOnly: Boolean = isBotAdministratorOnly
        private set

    fun setName(name: String): AbstractCommandBuilder = apply {
        this.name = name
    }

    @Deprecated("Use the automatically generated usage", ReplaceWith(""))
    fun setUsage(usage: String): AbstractCommandBuilder = apply {
        this.usage = usage
    }

    fun setDescription(description: String): AbstractCommandBuilder = apply {
        this.description = description
    }

    fun setCommandType(commandType: AbstractCommand.CommandType): AbstractCommandBuilder = apply {
        this.commandType = commandType
    }

    fun addAction(name: String, description: String): AbstractCommandBuilder = apply {
        actions[name] = description
    }

    fun setActions(actions: Map<String, String>): AbstractCommandBuilder = apply {
        this.actions = actions as MutableMap<String, String>
    }

    fun addAlias(alias: String): AbstractCommandBuilder = apply {
        aliases.add(alias)
    }

    fun setAliases(vararg aliases: String): AbstractCommandBuilder = apply {
        this.aliases = aliases.toMutableList()
    }

    fun setAliases(aliases: List<String>): AbstractCommandBuilder = apply {
        this.aliases = aliases as MutableList<String>
    }

    fun setMinimumAmountOfArgs(minimumAmountOfArgs: Int): AbstractCommandBuilder = apply {
        this.minimumAmountOfArgs = minimumAmountOfArgs
    }

    fun addFlag(flag: Flag<*>): AbstractCommandBuilder = apply {
        flags.add(flag)
    }

    fun setFlags(vararg flags: Flag<*>): AbstractCommandBuilder = apply {
        this.flags = flags.toMutableList()
    }

    fun setFlags(flags: List<Flag<*>>): AbstractCommandBuilder = apply {
        this.flags = flags as MutableList<Flag<*>>
    }

    fun addArgument(argument: Argument<*>): AbstractCommandBuilder = apply {
        arguments.add(argument)
    }

    fun setArguments(vararg arguments: Argument<*>): AbstractCommandBuilder = apply {
        this.arguments = arguments.toMutableList()
    }

    fun setArguments(arguments: List<Argument<*>>): AbstractCommandBuilder = apply {
        this.arguments = arguments as MutableList<Argument<*>>
    }

    fun setBotRequiredPermissions(vararg permissions: Permission): AbstractCommandBuilder = apply {
        this.botRequiredPermissions = PermissionSet.of(*permissions)
    }

    fun setBotRequiredPermissions(botRequiredPermissions: PermissionSet): AbstractCommandBuilder = apply {
        this.botRequiredPermissions = botRequiredPermissions
    }

    fun setAdministratorLevelRequired(administratorLevelRequired: Int): AbstractCommandBuilder = apply {
        this.administratorLevelRequired = administratorLevelRequired
    }

    fun setBotAdministratorOnly(): AbstractCommandBuilder = apply {
        isBotAdministratorOnly = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractCommandBuilder

        if (name != other.name) return false
        if (usage != other.usage) return false
        if (description != other.description) return false
        if (commandType != other.commandType) return false
        if (actions != other.actions) return false
        if (aliases != other.aliases) return false
        if (minimumAmountOfArgs != other.minimumAmountOfArgs) return false
        if (flags != other.flags) return false
        if (arguments != other.arguments) return false
        if (botRequiredPermissions != other.botRequiredPermissions) return false
        if (administratorLevelRequired != other.administratorLevelRequired) return false
        if (isBotAdministratorOnly != other.isBotAdministratorOnly) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (usage?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (commandType?.hashCode() ?: 0)
        result = 31 * result + actions.hashCode()
        result = 31 * result + aliases.hashCode()
        result = 31 * result + minimumAmountOfArgs
        result = 31 * result + flags.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + botRequiredPermissions.hashCode()
        result = 31 * result + administratorLevelRequired
        result = 31 * result + isBotAdministratorOnly.hashCode()
        return result
    }

    override fun toString(): String {
        return "AbstractCommandHandlerBuilder(name=$name, " +
                "usage=$usage, " +
                "description=$description, " +
                "commandType=$commandType, " +
                "actions=$actions, " +
                "aliases=$aliases, " +
                "minimumAmountOfArgs=$minimumAmountOfArgs, " +
                "flags=$flags, " +
                "arguments=$arguments, " +
                "botRequiredPermissions=$botRequiredPermissions, " +
                "administratorLevelRequired=$administratorLevelRequired, " +
                "isBotAdministratorOnly=$isBotAdministratorOnly" +
                ")"
    }
}