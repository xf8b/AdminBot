/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.xf8b.adminbot.api.commands

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import discord4j.rest.util.PermissionSet
import io.github.xf8b.adminbot.api.commands.arguments.Argument
import io.github.xf8b.adminbot.api.commands.flags.Flag
import io.github.xf8b.adminbot.settings.GuildSettings.Companion.getGuildSettings

abstract class AbstractCommandHandler {
    val name: String
    val usage: String
    val description: String
    val commandType: CommandType
    val actions: Map<String, String>
    val aliases: List<String>
    val minimumAmountOfArgs: Int
    val flags: List<Flag<*>>
    val arguments: List<Argument<*>>
    val botRequiredPermissions: PermissionSet
    val administratorLevelRequired: Int
    val isBotAdministratorOnly: Boolean

    constructor(name: String,
                description: String,
                commandType: CommandType,
                actions: Map<String, String> = ImmutableMap.of(),
                aliases: List<String> = ImmutableList.of(),
                minimumAmountOfArgs: Int = 0,
                flags: List<Flag<*>> = ImmutableList.of(),
                arguments: List<Argument<*>> = ImmutableList.of(),
                botRequiredPermissions: PermissionSet = PermissionSet.none(),
                administratorLevelRequired: Int = 0,
                isBotAdministratorOnly: Boolean = false) {
        this.name = name
        this.description = description
        this.commandType = commandType
        this.actions = ImmutableMap.copyOf(actions)
        this.aliases = ImmutableList.copyOf(aliases)
        this.minimumAmountOfArgs = minimumAmountOfArgs
        this.flags = ImmutableList.copyOf(flags)
        this.arguments = ImmutableList.copyOf(arguments)
        this.botRequiredPermissions = botRequiredPermissions
        this.administratorLevelRequired = administratorLevelRequired
        this.isBotAdministratorOnly = isBotAdministratorOnly
        usage = generateUsage(name, flags, arguments)
    }

    constructor(builder: AbstractCommandHandlerBuilder) {
        this.name = builder.name!!
        this.description = builder.description!!
        this.commandType = builder.commandType!!
        this.actions = ImmutableMap.copyOf(builder.actions)
        this.aliases = ImmutableList.copyOf(builder.aliases)
        this.minimumAmountOfArgs = builder.minimumAmountOfArgs
        this.flags = ImmutableList.copyOf(builder.flags)
        this.arguments = ImmutableList.copyOf(builder.arguments)
        this.botRequiredPermissions = builder.botRequiredPermissions
        this.administratorLevelRequired = builder.administratorLevelRequired
        this.isBotAdministratorOnly = builder.isBotAdministratorOnly
        usage = builder.usage ?: generateUsage(name, flags, arguments)
    }

    companion object {
        @JvmStatic
        fun builder(): AbstractCommandHandlerBuilder = AbstractCommandHandlerBuilder()

        private fun generateUsage(commandName: String, flags: List<Flag<*>>, arguments: List<Argument<*>>): String {
            val tempUsage = StringBuilder(commandName).append(" ")
            for (flag in flags) {
                if (flag.isRequired) {
                    tempUsage.append("<")
                } else {
                    tempUsage.append("[")
                }
                tempUsage.append("-").append(flag.shortName())
                        .append(" ")
                if (flag.requiresValue()) {
                    tempUsage.append("<").append(flag.longName()).append(">")
                } else {
                    tempUsage.append("[").append(flag.longName()).append("]")
                }
                if (flag.isRequired) {
                    tempUsage.append(">")
                } else {
                    tempUsage.append("]")
                }
                tempUsage.append(" ")
            }
            for (argument in arguments) {
                if (argument.isRequired) {
                    tempUsage.append("<").append(argument.name).append(">")
                            .append(" ")
                } else {
                    tempUsage.append("[").append(argument.name).append("]")
                            .append(" ")
                }
            }
            return tempUsage.toString().trim {
                it <= ' '
            }
        }
    }

    abstract fun onCommandFired(event: CommandFiredEvent)

    fun getNameWithPrefix(guildId: String): String = name.replace("\${prefix}", getGuildSettings(guildId).getPrefix())

    fun getUsageWithPrefix(guildId: String): String = usage.replace("\${prefix}", getGuildSettings(guildId).getPrefix())

    fun getAliasesWithPrefixes(guildId: String): List<String> = aliases.map {
        it.replace("\${prefix}", getGuildSettings(guildId).getPrefix())
    }

    fun requiresAdministrator(): Boolean = administratorLevelRequired > 0

    enum class CommandType(val description: String) {
        ADMINISTRATION("Commands related with administration."),
        BOT_ADMINISTRATOR("Commands only for bot administrators."),
        OTHER("Other commands which do not fit in any of the above categories.");
    }
}