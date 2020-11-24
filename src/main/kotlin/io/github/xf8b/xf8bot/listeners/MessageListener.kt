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

package io.github.xf8b.xf8bot.listeners

import discord4j.core.event.ReactiveEventAdapter
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.DisableChecks
import io.github.xf8b.xf8bot.api.commands.parsers.ArgumentCommandParser
import io.github.xf8b.xf8bot.api.commands.parsers.FlagCommandParser
import io.github.xf8b.xf8bot.commands.info.InfoCommand
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.LoggerDelegate
import io.github.xf8b.xf8bot.util.PermissionUtil.canMemberUseCommand
import io.github.xf8b.xf8bot.util.skip
import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.publisher.toMono

class MessageListener(
    private val xf8bot: Xf8bot,
    private val commandRegistry: CommandRegistry
) : ReactiveEventAdapter() {
    companion object {
        private val ARGUMENT_PARSER = ArgumentCommandParser()
        private val FLAG_PARSER = FlagCommandParser()
        private val LOGGER: Logger by LoggerDelegate()
    }

    override fun onMessageCreate(event: MessageCreateEvent): Mono<Void> {
        // TODO: reactify all the classes
        // TODO: add spam protection
        if (event.message.content.isEmpty()
            || event.member.isEmpty
            || event.message.author.map { it.isBot }.orElse(true)
        ) {
            return Mono.empty()
        }
        val message = event.message
        val content = message.content.trim()
        val guildId = event.guildId.get().asString()

        if (content matches "<@!?${event.client.selfId.asString()}> help".toRegex()) {
            val infoCommand: InfoCommand = commandRegistry.findRegisteredWithType()
            return onCommandFired(event, infoCommand, guildId, content)
        }

        val commandRequested = content.trim().split(" ").toTypedArray()[0]

        return onCommandFired(
            event,
            findCommand(commandRequested, guildId) ?: return Mono.empty(),
            guildId,
            content
        )
    }

    private fun findCommand(commandRequested: String, guildId: String): AbstractCommand? = commandRegistry.find {
        it.getNameWithPrefix(xf8bot, guildId).equals(commandRequested, ignoreCase = true)
    } ?: commandRegistry.find { command ->
        command.getAliasesWithPrefixes(xf8bot, guildId).any {
            it.equals(commandRequested, ignoreCase = true)
        }
    }

    private fun handleLevels() {
        TODO("finish this later")
    }


    private fun onCommandFired(
        event: MessageCreateEvent,
        command: AbstractCommand,
        guildId: String,
        content: String
    ): Mono<Void> {
        val flagParseResult = FLAG_PARSER.parse(command, content)
        val argumentParseResult = ARGUMENT_PARSER.parse(command, content)

        return if (flagParseResult.isSuccess() && argumentParseResult.isSuccess()) {
            val commandFiredContext = CommandFiredEvent(
                event,
                xf8bot,
                flagParseResult.result!!,
                argumentParseResult.result!!
            )
            val disabledChecks: List<AbstractCommand.Checks>? = command.javaClass
                .getAnnotation(DisableChecks::class.java)
                ?.value
                ?.asList()
            val commandName = command.name.skip(1)

            commandFiredContext.guild.filterWhen { guild ->
                if (disabledChecks != null) {
                    if (disabledChecks.contains(AbstractCommand.Checks.BOT_HAS_REQUIRED_PERMISSIONS)) {
                        return@filterWhen true.toMono()
                    }
                }

                guild.selfMember
                    .flatMap { it.basePermissions }
                    .filter { it.containsAll(command.botRequiredPermissions) || it.contains(Permission.ADMINISTRATOR) }
                    .map { true }
                    .switchIfEmpty(commandFiredContext.channel.flatMap {
                        it.createMessage("Could not execute command \'$commandName\' because of insufficient permissions!")
                    }.thenReturn(false))
            }.filterWhen { guild ->
                if (disabledChecks != null) {
                    if (disabledChecks.contains(AbstractCommand.Checks.IS_ADMINISTRATOR)) {
                        return@filterWhen true.toMono()
                    }
                }

                if (command.requiresAdministrator()) {
                    canMemberUseCommand(
                        xf8bot,
                        guild,
                        commandFiredContext.member.get(),
                        command
                    ).flatMap { canUseCommand ->
                        if (!canUseCommand) {
                            commandFiredContext.channel
                                .flatMap { it.createMessage("Sorry, you don't have high enough permissions.") }
                                .thenReturn(false)
                        } else {
                            true.toMono()
                        }
                    }
                } else {
                    true.toMono()
                }
            }.filterWhen {
                if (disabledChecks != null) {
                    if (disabledChecks.contains(AbstractCommand.Checks.IS_BOT_ADMINISTRATOR)) {
                        return@filterWhen true.toMono()
                    }
                }

                if (command.botAdministratorOnly
                    && !commandFiredContext.xf8bot.isBotAdministrator(commandFiredContext.member.get().id)
                ) {
                    commandFiredContext.channel
                        .flatMap { it.createMessage("Sorry, you aren't a administrator of xf8bot.") }
                        .thenReturn(false)
                } else {
                    true.toMono()
                }
            }.filterWhen {
                if (disabledChecks != null) {
                    if (disabledChecks.contains(AbstractCommand.Checks.SURPASSES_MINIMUM_AMOUNT_OF_ARGUMENTS)) {
                        return@filterWhen true.toMono()
                    }
                }

                @Suppress("DEPRECATION")
                if (content.split(" ").skip(1).size < command.minimumAmountOfArgs) {
                    val usage = command.getUsageWithPrefix(xf8bot, guildId)

                    commandFiredContext.channel
                        .flatMap { it.createMessage("Huh? Could you repeat that? The usage of this command is: `$usage`.") }
                        .thenReturn(false)
                } else {
                    true.toMono()
                }
            }.flatMap {
                command.onCommandFired(commandFiredContext)
            }.doOnError {
                LOGGER.error("An error happened while handling command $commandName!", it)
            }.onErrorResume(ClientException::class) { exception ->
                commandFiredContext.channel
                    .flatMap { it.createMessage("Client exception happened while handling command: ${exception.status}: ${exception.errorResponse.get().fields}") }
                    .then()
            }.onErrorResume(ThisShouldNotHaveBeenThrownException::class) {
                commandFiredContext.channel
                    .flatMap { it.createMessage("Something has horribly gone wrong. Please report this to the bot developer with the log.") }
                    .then()
            }.onErrorResume { throwable ->
                commandFiredContext.channel
                    .flatMap { it.createMessage("Exception happened while handling command: ${throwable.message}") }
                    .then()
            }
        } else {
            when {
                flagParseResult.isFailure() -> event.message
                    .channel
                    .flatMap { it.createMessage(flagParseResult.errorMessage) }
                    .then()

                argumentParseResult.isFailure() -> event.message
                    .channel
                    .flatMap { it.createMessage(argumentParseResult.errorMessage) }
                    .then()

                else -> throw ThisShouldNotHaveBeenThrownException()
            }
        }
    }
}