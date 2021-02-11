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

package io.github.xf8b.xf8bot.commands.info

import com.google.common.collect.Range
import discord4j.common.util.Snowflake
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.EmbedCreateDsl
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.extensions.*
import io.github.xf8b.xf8bot.util.immutableListOf
import io.github.xf8b.xf8bot.util.pagination.createPaginatedEmbed
import org.apache.commons.text.WordUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.*

class HelpCommand : Command(
    name = "\${prefix}help",
    description = """
    If a command was specified, this shows the command's description, usage, aliases, and actions.
    If no command was specified, but a section was specified, all the commands in the section will be shown.
    If no section or command was specified, all the commands will be shown.
    """.trimIndent(),
    commandType = CommandType.INFO,
    arguments = immutableListOf(SECTION_OR_COMMAND),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val commandOrSection = event[SECTION_OR_COMMAND]

        if (commandOrSection == null) {
            return event.prefix
                .map { prefix -> if (prefix.isAlpha()) "$prefix " else prefix }
                .flatMap { prefix ->
                    event.channel.flatMap {
                        it.createEmbedDsl {
                            title("Help Page")
                            color(Color.BLUE)

                            for (commandType in CommandType.values()) {
                                val commandTypeName = commandType.name
                                    .toLowerCase(Locale.ROOT)
                                    .replace("_", " ")

                                field(
                                    "`${WordUtils.capitalizeFully(commandTypeName)}`",
                                    """
                                    ${commandType.description}
                                    To go to this section, use `${prefix}help $commandTypeName`
                                    """.trimIndent(),
                                    inline = false
                                )
                            }
                        }
                    }
                }
                .then()
        } else {
            for (commandType in CommandType.values()) {
                if (commandOrSection.equals(commandType.name, ignoreCase = true)) {
                    return generateCommandTypeEmbed(event, commandType)
                }
            }

            for (command in event.xf8bot.commandRegistry) {
                if (commandOrSection == command.rawName || command.rawAliases.any { alias -> commandOrSection == alias }) {
                    return generateCommandEmbed(event, command)
                }
            }
        }

        return event.channel
            .flatMap { it.createMessage("Could not find command/section $commandOrSection!") }
            .then()
    }

    private fun generateCommandTypeEmbed(event: CommandFiredEvent, commandType: CommandType): Mono<Void> =
        event.xf8bot.commandRegistry
            .getCommandsWithCommandType(commandType)
            .toFlux()
            .flatMap { command ->
                Mono.zip(
                    command.getNameWithPrefix(event.xf8bot, event.guildId.map(Snowflake::asString).get()),
                    command.rawName.toMono(),
                    command.description.toMono(),
                    command.getUsageWithPrefix(event.xf8bot, event.guildId.map(Snowflake::asString).get()),
                    command.administratorLevelRequired.toMono(),
                    event.prefix
                )
            }
            .buffer(6)
            .map<EmbedCreateDsl.() -> Unit> { information ->
                {
                    val prefix = information[0].t6

                    title("Help Page")
                    description("More detailed command information is not listed on this page. To see it, use `${prefix}help <command>`.")

                    for (info in information) {
                        val (name, rawName, description, usage, administratorLevel) = info

                        field(
                            "`$name`",
                            """
                            $description
                            Usage: `$usage`
                            Administrator Level Required: $administratorLevel
                            If you want to go to the help page for this command, use `${prefix}help $rawName`.
                            """.trimIndent(),
                            inline = false
                        )
                    }

                    color(Color.BLUE)
                }
            }
            .collectList()
            .flatMap { dslInitializers ->
                event.channel.flatMap { channel ->
                    channel.createPaginatedEmbed(*dslInitializers.toTypedArray())
                }
            }
            .then()

    private fun generateCommandEmbed(event: CommandFiredEvent, command: Command): Mono<Void> = Mono.zip(
        command.getUsageWithPrefix(event.xf8bot, event.guildId.map(Snowflake::asString).get()),
        command.getAliasesWithPrefixes(event.xf8bot, event.guildId.map(Snowflake::asString).get()).collectList()
    ).flatMap { commandInfo ->
        event.channel.flatMap { channel ->
            channel.createEmbedDsl {
                val (usage, aliases) = commandInfo

                title("Help Page For `${command.rawName}`")

                field(
                    "`${command.rawName}`",
                    """
                    ${command.description}
                    Usage: `$usage`
                    """.trimIndent(),
                    inline = false
                )

                field(
                    "Administrator Level Required",
                    command.administratorLevelRequired.toString(),
                    inline = false
                )
                field(
                    "Bot Required Permissions",
                    command.botRequiredPermissions
                        .joinToString { WordUtils.capitalizeFully(it.name.replace("_", " ")) }
                        .takeIf(String::isNotEmpty)
                        ?: "No permissions required",
                    inline = false
                )

                color(Color.BLUE)

                if (command.actions.isNotEmpty()) {
                    val actionsFormatted = command.actions
                        .toList()
                        .joinToString(separator = "\n") { "`${it.first}`: ${it.second}" }

                    field("Actions", actionsFormatted, inline = false)
                }

                if (aliases.isNotEmpty()) {
                    val aliasesFormatted = aliases.joinToString(separator = "\n") { "`$it`" }

                    field("Aliases", aliasesFormatted, inline = false)
                }
            }
        }
    }.then()

    companion object {
        private val SECTION_OR_COMMAND = StringArgument(
            name = "section or command",
            index = Range.singleton(0),
            required = false
        )
    }
}