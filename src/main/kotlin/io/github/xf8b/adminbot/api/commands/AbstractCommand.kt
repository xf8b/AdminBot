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
import com.mongodb.client.model.Filters
import discord4j.rest.util.PermissionSet
import io.github.xf8b.adminbot.AdminBot
import io.github.xf8b.adminbot.api.commands.arguments.Argument
import io.github.xf8b.adminbot.api.commands.flags.Flag
import reactor.core.publisher.Mono

abstract class AbstractCommand {
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

    constructor(builder: AbstractCommandBuilder) {
        if (builder.name == null || builder.description == null || builder.commandType == null) {
            throw NullPointerException("The name, description, and/or commandType was not set!")
        }
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
        fun builder(): AbstractCommandBuilder = AbstractCommandBuilder()

        private fun getPrefix(adminBot: AdminBot, guildId: String) = Mono.from(adminBot.mongoDatabase.getCollection("prefixes")
                .find(Filters.eq("guildId", guildId.toLong())))
                .map { it.get("prefix", String::class.java) }
                .block()!!

        private fun generateUsage(commandName: String, flags: List<Flag<*>>, arguments: List<Argument<*>>): String {
            val tempUsage = StringBuilder(commandName).append(" ")
            for (argument in arguments) {
                if (argument.required) {
                    tempUsage.append("<").append(argument.name).append(">")
                            .append(" ")
                } else {
                    tempUsage.append("[").append(argument.name).append("]")
                            .append(" ")
                }
            }
            for (flag in flags) {
                if (flag.required) {
                    tempUsage.append("<")
                } else {
                    tempUsage.append("[")
                }
                tempUsage.append("-").append(flag.shortName)
                        .append(" ")
                if (flag.requiresValue) {
                    tempUsage.append("<").append(flag.longName).append(">")
                } else {
                    tempUsage.append("[").append(flag.longName).append("]")
                }
                if (flag.required) {
                    tempUsage.append(">")
                } else {
                    tempUsage.append("]")
                }
                tempUsage.append(" ")
            }
            return tempUsage.toString().trim { it <= ' ' }
        }
    }

    abstract fun onCommandFired(event: CommandFiredEvent): Mono<Void>

    fun getNameWithPrefix(adminBot: AdminBot, guildId: String): String =
            name.replace("\${prefix}", getPrefix(adminBot, guildId))

    fun getUsageWithPrefix(adminBot: AdminBot, guildId: String): String =
            usage.replace("\${prefix}", getPrefix(adminBot, guildId))

    fun getAliasesWithPrefixes(adminBot: AdminBot, guildId: String): List<String> = aliases.map {
        it.replace("\${prefix}", getPrefix(adminBot, guildId))
    }

    fun requiresAdministrator(): Boolean = administratorLevelRequired > 0

    enum class CommandType(val description: String) {
        ADMINISTRATION("Commands related with administration."),
        BOT_ADMINISTRATOR("Commands only for bot administrators."),
        OTHER("Other commands which do not fit in any of the above categories.");
    }
}