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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*

abstract class AbstractCommand(
    /** What triggers this command */
    val name: String,
    /** The description of this command, shown on the help page */
    val description: String,
    /** The type of this command, to dictate where the command is put in the help page */
    val commandType: CommandType,
    /** Actions that you can use for this command (>command ${action} ${other args}) */
    val actions: Map<String, String> = ImmutableMap.of(),
    /** Aliases for this command. Works the same as [name]. */
    val aliases: List<String> = ImmutableList.of(),
    /** Flags that this command takes in (optionally or mandatory) */
    val flags: List<Flag<*>> = ImmutableList.of(),
    /** Arguments that this command takes in (optionally or mandatory) */
    val arguments: List<Argument<*>> = ImmutableList.of(),
    /** Minimum amount of arguments required to use this command. Automatically set, do not set this */
    @Deprecated("Do not set this, rather use the automatically generated one. Getting is fine.")
    val minimumAmountOfArgs: Int = arguments.filter { it.required }.size,
    /** Usage for this command shown on the help page. Automatically set, do not set this */
    @Deprecated("Do not set this, rather use the automatically generated usage. Getting is fine.")
    val usage: String = generateUsage(name, flags, arguments),
    /** Permissions that the bot requires before this command be ran */
    val botRequiredPermissions: PermissionSet = PermissionSet.none(),
    /** Checks ran during command handling that are disabled */
    val disabledChecks: EnumSet<ExecutionChecks> = EnumSet.noneOf(ExecutionChecks::class.java),
    /** Administrator level required to run this command according to [io.github.xf8b.xf8bot.commands.settings.AdministratorsCommand] */
    val administratorLevelRequired: Int = 0,
    /** If this command is only to be run by bot administrators */
    val botAdministratorOnly: Boolean = false
) {
    companion object {
        private fun generateUsage(commandName: String, flags: List<Flag<*>>, arguments: List<Argument<*>>): String {
            val argumentsUsage = arguments.joinToString(separator = " ") {
                if (it.required) "<${it.name}>" else "[${it.name}]"
            }
            val flagsUsage = flags.joinToString(separator = " ") { flag ->
                var usage = ""

                // end result example: ${prefix}hello <person> [-p [ping] = true]] <-c <channel>>

                usage += if (flag.required) "<" else "["
                usage += "-${flag.shortName} "
                usage += if (flag.requiresValue) "<${flag.longName}>" else "[${flag.longName}]"
                if (flag.defaultValue != null) usage += " = ${flag.defaultValue}"
                usage += (if (flag.required) ">" else "]")

                usage
            }
            var finalUsage = commandName

            if (argumentsUsage.isNotEmpty()) finalUsage += " $argumentsUsage"
            if (flagsUsage.isNotEmpty()) finalUsage += " $flagsUsage"

            return finalUsage
        }
    }

    val rawName get() = name.replace("\${prefix}", "")

    abstract fun onCommandFired(event: CommandFiredEvent): Mono<Void>

    fun getNameWithPrefix(xf8bot: Xf8bot, guildId: String): Mono<String> = xf8bot.prefixCache
        .get(guildId.toSnowflake())
        .map { prefix -> name.replace("\${prefix}", prefix) }

    @Suppress("DEPRECATION") // usage is deprecated for setting, not getting
    fun getUsageWithPrefix(xf8bot: Xf8bot, guildId: String): Mono<String> = xf8bot.prefixCache
        .get(guildId.toSnowflake())
        .map { prefix -> usage.replace("\${prefix}", prefix) }

    fun getAliasesWithPrefixes(xf8bot: Xf8bot, guildId: String): Flux<String> = aliases.toFlux()
        .flatMap {
            xf8bot.prefixCache
                .get(guildId.toSnowflake())
                .map { prefix -> it.replace("\${prefix}", prefix) }
        }

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractCommand) return false

        if (name != other.name) return false
        if (description != other.description) return false
        if (commandType != other.commandType) return false
        if (actions != other.actions) return false
        if (aliases != other.aliases) return false
        if (flags != other.flags) return false
        if (arguments != other.arguments) return false
        if (minimumAmountOfArgs != other.minimumAmountOfArgs) return false
        if (usage != other.usage) return false
        if (botRequiredPermissions != other.botRequiredPermissions) return false
        if (administratorLevelRequired != other.administratorLevelRequired) return false
        if (botAdministratorOnly != other.botAdministratorOnly) return false

        return true
    }

    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + commandType.hashCode()
        result = 31 * result + actions.hashCode()
        result = 31 * result + aliases.hashCode()
        result = 31 * result + flags.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + minimumAmountOfArgs
        result = 31 * result + usage.hashCode()
        result = 31 * result + botRequiredPermissions.hashCode()
        result = 31 * result + administratorLevelRequired
        result = 31 * result + botAdministratorOnly.hashCode()

        return result
    }

    enum class CommandType(val description: String) {
        ADMINISTRATION("Commands related with administration."),
        BOT_ADMINISTRATOR("Commands only for bot administrators."),
        MUSIC("Commands related with playing music."),
        INFO("Commands which give information."),
        LEVELING("Leveling commands. Somewhat useless."),
        SETTINGS("Commands that are used for settings/configurations."),
        OTHER("Other commands which do not fit in any of the above categories."),
    }

    enum class ExecutionChecks {
        IS_ADMINISTRATOR,
        IS_BOT_ADMINISTRATOR,
        SURPASSES_MINIMUM_AMOUNT_OF_ARGUMENTS,
        BOT_HAS_REQUIRED_PERMISSIONS
    }
}