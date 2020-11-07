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

package io.github.xf8b.xf8bot.commands.info

import com.google.common.collect.Range
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.toImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import org.apache.commons.text.WordUtils
import reactor.core.publisher.Mono

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
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val guildId = context.guildId.orElseThrow().asString()
        val xf8bot = context.xf8bot
        val commandOrSection = context.getValueOfArgument(SECTION_OR_COMMAND)
        if (commandOrSection.isEmpty) {
            return context.prefix.flatMap { prefix ->
                context.channel.flatMap {
                    it.createEmbed { spec ->
                        spec.setTitle("Help Page").setColor(Color.BLUE)
                        for (commandType in CommandType.values()) {
                            val commandTypeName = WordUtils.capitalizeFully(
                                commandType.name
                                    .toLowerCase()
                                    .replace("_", " ")
                            )
                            spec.addField(
                                "`$commandTypeName`",
                                """
                                ${commandType.description}
                                To go to this section, use `${prefix}help ${
                                    commandType.name
                                        .toLowerCase()
                                        .replace(" ", "_")
                                }`
                                """.trimIndent(),
                                false
                            )
                        }
                    }
                }
            }.then()
        } else {
            for (commandType in CommandType.values()) {
                if (commandOrSection.get().equals(commandType.name, ignoreCase = true)) {
                    val pageNumber = context.getValueOfArgument(PAGE).orElse(0)
                    val commandsWithCurrentCommandType = context.xf8bot
                        .commandRegistry
                        .getCommandsWithCommandType(commandType)
                    if (commandsWithCurrentCommandType.size > 6) {
                        if (!(0 until commandsWithCurrentCommandType.size % 6).contains(pageNumber)) {
                            return context.channel
                                .flatMap {
                                    it.createMessage("No page with the index ${pageNumber + 1} exists!")
                                }
                                .then()
                        }
                    }
                    return context.prefix.flatMap { prefix ->
                        context.channel.flatMap {
                            it.createEmbed { spec ->
                                generateCommandTypeEmbed(
                                    context,
                                    context.xf8bot.commandRegistry,
                                    spec,
                                    commandType,
                                    guildId,
                                    pageNumber,
                                    prefix
                                )
                            }
                        }
                    }.then()
                }
            }

            for (command in context.xf8bot.commandRegistry) {
                val name = command.name
                val nameWithPrefix = command.getNameWithPrefix(xf8bot, guildId)
                val aliases = command.aliases
                val aliasesWithPrefixes = command.getAliasesWithPrefixes(xf8bot, guildId)
                if (commandOrSection.get() == name.replace("\${prefix}", "")) {
                    val description = command.description
                    val usage = command.getUsageWithPrefix(xf8bot, guildId)
                    val actions = command.actions
                    return context.channel.flatMap {
                        it.createEmbed { spec ->
                            generateCommandEmbed(
                                spec,
                                nameWithPrefix,
                                description,
                                usage,
                                aliasesWithPrefixes,
                                actions
                            )
                        }
                    }.then()
                } else if (aliases.isNotEmpty()) {
                    for (alias in aliases) {
                        if (commandOrSection.get() == alias.replace("\${prefix}", "")) {
                            val description = command.description
                            val usage = command.getUsageWithPrefix(xf8bot, guildId)
                            val actions = command.actions
                            return context.channel.flatMap {
                                it.createEmbed { spec ->
                                    generateCommandEmbed(
                                        spec,
                                        nameWithPrefix,
                                        description,
                                        usage,
                                        aliasesWithPrefixes,
                                        actions
                                    )
                                }
                            }.then()
                        }
                    }
                }
            }
        }
        return context.channel.flatMap {
            it.createMessage("Error: Could not find command/section ${commandOrSection.get()}")
        }.then()
    }

    private fun generateCommandTypeEmbed(
        context: CommandFiredContext,
        commandRegistry: CommandRegistry,
        embedCreateSpec: EmbedCreateSpec,
        commandType: CommandType,
        guildId: String,
        pageNumber: Int,
        prefix: String
    ) {
        embedCreateSpec.setTitle("Help Page #${pageNumber + 1}")
            .setDescription(
                """
                Actions are not listed on this page. To see them, do `${prefix}help <command>`.
                To go to a different page, use `${prefix}help <section> <page>`.
                """.trimIndent(),
            )
            .setColor(Color.BLUE)
        val commandsWithCurrentCommandType = commandRegistry
            .getCommandsWithCommandType(commandType)
        for (i in pageNumber * 6 until pageNumber * 6 + 6) {
            val command: AbstractCommand = try {
                commandsWithCurrentCommandType[i]
            } catch (exception: IndexOutOfBoundsException) {
                break
            }

            val name = command.getNameWithPrefix(context.xf8bot, guildId)
            val nameWithPrefixRemoved = command.name.replace("\${prefix}", "")
            val description = command.description
            val usage = command.getUsageWithPrefix(context.xf8bot, guildId)
            embedCreateSpec.addField(
                "`$name`",
                """
                $description
                Usage: `$usage`
                If you want to go to the help page for this command, use `${prefix}help $nameWithPrefixRemoved`                 `.
                """.trimIndent(),
                false
            )
        }
    }

    private fun generateCommandEmbed(
        embedCreateSpec: EmbedCreateSpec,
        name: String,
        description: String,
        usage: String,
        aliases: List<String>,
        actions: Map<String, String>
    ) {
        embedCreateSpec.setTitle("Help Page For `$name`")
            .addField(
                "`$name`",
                """
                $description
                Usage: `$usage`
                """.trimIndent(),
                false
            )
            .setColor(Color.BLUE)
        if (actions.isNotEmpty()) {
            val actionsFormatted = StringBuilder()
            actions.forEach { (action: String, actionDescription: String) ->
                actionsFormatted
                    .append("`").append(action).append("`: ")
                    .append(actionDescription)
                    .append("\n")
            }
            embedCreateSpec.addField("Actions", actionsFormatted.toString().replace("\n$".toRegex(), ""), false)
        }
        if (aliases.isNotEmpty()) {
            val aliasesFormatted = StringBuilder()
            aliases.forEach {
                aliasesFormatted.append("`").append(it).append("`\n")
            }
            embedCreateSpec.addField("Aliases", aliasesFormatted.toString().replace("\n$".toRegex(), ""), false)
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