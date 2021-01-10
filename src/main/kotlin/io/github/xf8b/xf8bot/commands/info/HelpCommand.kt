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
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.toImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import org.apache.commons.text.WordUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

// TODO: make paginated embed system so this can go back to using reactions for pages
class HelpCommand : AbstractCommand(
    name = "\${prefix}help",
    description = """
    If a command was specified, this shows the command's description, usage, aliases, and actions.
    If no command was specified, but a section was specified, all the commands in the section will be shown.
    If no section or command was specified, all the commands will be shown.
    """.trimIndent(),
    commandType = CommandType.INFO,
    arguments = (SECTION_OR_COMMAND to PAGE).toImmutableList(),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId = event.guildId.orElseThrow().asString()
        val commandOrSection = event.getValueOfArgument(SECTION_OR_COMMAND)
        if (commandOrSection.isEmpty) {
            return event.prefix.flatMap { prefix ->
                event.channel.flatMap {
                    it.createEmbedDsl {
                        title("Help Page")
                        color(Color.BLUE)

                        for (commandType in CommandType.values()) {
                            val commandTypeName = WordUtils.capitalizeFully(
                                commandType.name
                                    .toLowerCase()
                                    .replace("_", " ")
                            )
                            field(
                                "`$commandTypeName`",
                                """
                                ${commandType.description}
                                To go to this section, use `${prefix}help ${
                                    commandType.name
                                        .toLowerCase()
                                        .replace(" ", "_")
                                }`
                                """.trimIndent(),
                                inline = false
                            )
                        }
                    }
                }
            }.then()
        } else {
            for (commandType in CommandType.values()) {
                if (commandOrSection.get().equals(commandType.name, ignoreCase = true)) {
                    val pageNumber = event.getValueOfArgument(PAGE).orElse(1)
                    val commandsWithCurrentCommandType = event.xf8bot
                        .commandRegistry
                        .getCommandsWithCommandType(commandType)
                    if (!commandsWithCurrentCommandType.indices.contains((pageNumber - 1) * 6)) {
                        return event.channel
                            .flatMap { it.createMessage("No page with the index $pageNumber exists!") }
                            .then()
                    }
                    return event.prefix.flatMap { prefix ->
                        event.channel.flatMap {
                            generateCommandTypeEmbed(
                                event,
                                event.xf8bot.commandRegistry,
                                it,
                                commandType,
                                guildId,
                                pageNumber,
                                prefix
                            )
                        }
                    }.then()
                }
            }

            for (command in event.xf8bot.commandRegistry) {
                if (commandOrSection.get() == command.rawName) {
                    return event.channel.flatMap { channel ->
                        Mono.zip(
                            command.getUsageWithPrefix(event.xf8bot, guildId),
                            command.getAliasesWithPrefixes(event.xf8bot, guildId).collectList()
                        ).flatMap {
                            generateCommandEmbed(channel, command, it.t1, it.t2)
                        }
                    }.then()
                } else if (command.aliases.isNotEmpty()) {
                    for (alias in command.aliases) {
                        if (commandOrSection.get() == alias.replace("\${prefix}", "")) {
                            return event.channel.flatMap { channel ->
                                Mono.zip(
                                    command.getUsageWithPrefix(event.xf8bot, guildId),
                                    command.getAliasesWithPrefixes(event.xf8bot, guildId).collectList()
                                ).flatMap {
                                    generateCommandEmbed(channel, command, it.t1, it.t2)
                                }
                            }.then()
                        }
                    }
                }
            }
        }
        return event.channel
            .flatMap { it.createMessage("Could not find command/section ${commandOrSection.get()}!") }
            .then()
    }

    private fun generateCommandTypeEmbed(
        event: CommandFiredEvent,
        commandRegistry: CommandRegistry,
        messageChannel: MessageChannel,
        commandType: CommandType,
        guildId: String,
        pageNumber: Int,
        prefix: String
    ): Mono<Message> =
        commandRegistry.getCommandsWithCommandType(commandType)
            .toFlux()
            .skip((pageNumber - 1) * 6L)
            .take(6)
            .flatMap {
                Mono.zip(
                    it.getNameWithPrefix(event.xf8bot, guildId),
                    it.rawName.toMono(),
                    it.description.toMono(),
                    it.getUsageWithPrefix(event.xf8bot, guildId),
                    it.administratorLevelRequired.toMono()
                )
            }
            .collectList()
            .flatMap { commandInfos ->
                messageChannel.createEmbedDsl {
                    title("Help Page #$pageNumber")
                    description(
                        """
                        More detailed command information is not listed on this page. To see it, use `${prefix}help <command>`.
                        To go to a different page, use `${prefix}help <section> <page>`.
                        """.trimIndent(),
                    )

                    commandInfos.forEach {
                        field(
                            "`${it.t1}`",
                            """
                            ${it.t3}
                            Usage: `${it.t4}`
                            Administrator Level Required: ${it.t5}
                            If you want to go to the help page for this command, use `${prefix}help ${it.t2}`.
                            """.trimIndent(),
                            inline = false
                        )
                    }

                    color(Color.BLUE)
                }
            }

    private fun generateCommandEmbed(
        messageChannel: MessageChannel,
        command: AbstractCommand,
        usage: String,
        aliases: List<String>
    ): Mono<Message> = messageChannel.createEmbedDsl {
        title("Help Page For `${command.rawName}`")

        field(
            "`${command.rawName}`",
            """
            ${command.description}
            Usage: `$usage`
            """.trimIndent(),
            inline = false
        )

        field("Administrator Level Required", command.administratorLevelRequired.toString(), inline = false)
        field(
            "Bot Required Permissions",
            command.botRequiredPermissions
                .joinToString { WordUtils.capitalizeFully(it.name.replace("_", " ")) },
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

    companion object {
        private val SECTION_OR_COMMAND = StringArgument(
            name = "section or command",
            index = Range.singleton(1),
            required = false
        )
        private val PAGE = IntegerArgument(
            name = "page",
            index = Range.singleton(2),
            required = false
        )
    }
}