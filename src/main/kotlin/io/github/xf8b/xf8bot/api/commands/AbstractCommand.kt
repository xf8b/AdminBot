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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import discord4j.rest.util.PermissionSet
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Mono

abstract class AbstractCommand(
    val name: String,
    val description: String,
    val commandType: CommandType,
    val actions: Map<String, String> = ImmutableMap.of(),
    val aliases: List<String> = ImmutableList.of(),
    val minimumAmountOfArgs: Int = 0,
    val flags: List<Flag<*>> = ImmutableList.of(),
    val arguments: List<Argument<*>> = ImmutableList.of(),
    // Do not set this, you should use the automatically generated usage.
    val usage: String = generateUsage(name, flags, arguments),
    val botRequiredPermissions: PermissionSet = PermissionSet.none(),
    val administratorLevelRequired: Int = 0,
    val isBotAdministratorOnly: Boolean = false
) {
    companion object {
        private fun generateUsage(
            commandName: String,
            flags: List<Flag<*>>,
            arguments: List<Argument<*>>
        ): String {
            val generatedUsage = StringBuilder(commandName).append(" ")
            for (argument in arguments) {
                if (argument.required) {
                    generatedUsage.append("<").append(argument.name).append(">")
                        .append(" ")
                } else {
                    generatedUsage.append("[").append(argument.name).append("]")
                        .append(" ")
                }
            }
            for (flag in flags) {
                if (flag.required) {
                    generatedUsage.append("<")
                } else {
                    generatedUsage.append("[")
                }
                generatedUsage.append("-").append(flag.shortName)
                    .append(" ")
                if (flag.requiresValue) {
                    generatedUsage.append("<").append(flag.longName).append(">")
                } else {
                    generatedUsage.append("[").append(flag.longName).append("]")
                }
                if (flag.required) {
                    generatedUsage.append(">")
                } else {
                    generatedUsage.append("]")
                }
                generatedUsage.append(" ")
            }
            return generatedUsage.toString().trim()
        }
    }

    abstract fun onCommandFired(context: CommandFiredContext): Mono<Void>

    fun getNameWithPrefix(xf8bot: Xf8bot, guildId: String): String =
        name.replace("\${prefix}", xf8bot.prefixCache.get(guildId.toSnowflake()).block()!!)

    fun getUsageWithPrefix(xf8bot: Xf8bot, guildId: String): String =
        usage.replace("\${prefix}", xf8bot.prefixCache.get(guildId.toSnowflake()).block()!!)

    fun getAliasesWithPrefixes(xf8bot: Xf8bot, guildId: String): List<String> = aliases.map {
        it.replace("\${prefix}", xf8bot.prefixCache.get(guildId.toSnowflake()).block()!!)
    }

    fun requiresAdministrator(): Boolean = administratorLevelRequired > 0

    enum class CommandType(val description: String) {
        ADMINISTRATION("Commands related with administration."),
        BOT_ADMINISTRATOR("Commands only for bot administrators."),
        MUSIC("Commands related with playing music."),
        INFO("Commands which give information."),
        SETTINGS("Commands that are used for settings/configurations."),
        OTHER("Other commands which do not fit in any of the above categories."),
    }

    enum class Checks {
        IS_ADMINISTRATOR,
        IS_BOT_ADMINISTRATOR,
        SURPASSES_MINIMUM_AMOUNT_OF_ARGUMENTS,
        BOT_HAS_REQUIRED_PERMISSIONS
    }
}