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

import discord4j.core.`object`.entity.User
import discord4j.core.event.ReactiveEventAdapter
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.http.client.ClientException
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.parsers.ArgumentCommandParser
import io.github.xf8b.xf8bot.api.commands.parsers.FlagCommandParser
import io.github.xf8b.xf8bot.commands.info.InfoCommand
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.Checks
import io.github.xf8b.xf8bot.util.LoggerDelegate
import io.github.xf8b.xf8bot.util.skip
import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume

class MessageListener(
    private val xf8bot: Xf8bot,
    private val commandRegistry: CommandRegistry
) : ReactiveEventAdapter() {
    override fun onMessageCreate(event: MessageCreateEvent): Mono<Void> {
        if (event.message.content.isEmpty()
            || event.member.isEmpty
            || event.message.author.map(User::isBot).orElse(true)
        ) {
            return Mono.empty()
        }

        val message = event.message
        val content = message.content.trim()
        val guildId = event.guildId.get().asString()

        if (content matches "<@!?${event.client.selfId.asString()}> help".toRegex()) {
            val infoCommand: InfoCommand = commandRegistry.findRegisteredWithType()
            return onCommandFired(event, infoCommand, content)
        }

        val commandRequested = content.trim().split(" ").toTypedArray()[0]

        return onCommandFired(event, findCommand(commandRequested, guildId) ?: return Mono.empty(), content)
    }

    private fun findCommand(commandRequested: String, guildId: String): AbstractCommand? =
        commandRegistry.find { command ->
            command.getNameWithPrefix(xf8bot, guildId).equals(commandRequested, ignoreCase = true)
        } ?: commandRegistry.find { command ->
            command.getAliasesWithPrefixes(xf8bot, guildId).any { alias ->
                alias.equals(commandRequested, ignoreCase = true)
            }
        }

    private fun onCommandFired(event: MessageCreateEvent, command: AbstractCommand, content: String): Mono<Void> {
        val flagParseResult = FLAG_PARSER.parse(command, content)
        val argumentParseResult = ARGUMENT_PARSER.parse(command, content)

        return if (flagParseResult.isSuccess() && argumentParseResult.isSuccess()) {
            val commandFiredEvent = CommandFiredEvent(
                event,
                xf8bot,
                flagParseResult.result!!,
                argumentParseResult.result!!
            )
            val commandName = command.name.skip(1)

            commandFiredEvent.guild
                .filterWhen { Checks.doesBotHavePermissionsRequired(command, it.selfMember, commandFiredEvent.channel) }
                .filterWhen { Checks.doesMemberHaveCorrectAdministratorLevel(command, commandFiredEvent) }
                .filterWhen {
                    Checks.canMemberUseBotAdministratorOnlyCommand(
                        command,
                        xf8bot,
                        event.member.get(),
                        event.message.channel
                    )
                }
                .filterWhen { Checks.isThereEnoughArguments(command, commandFiredEvent) }
                .flatMap { command.onCommandFired(commandFiredEvent) }
                .doOnError { LOGGER.error("An error happened while handling command $commandName!", it) }
                .onErrorResume(ClientException::class) { exception ->
                    commandFiredEvent.channel
                        .flatMap { it.createMessage("Client exception happened while handling command: ${exception.status}: ${exception.errorResponse.get().fields}") }
                        .then()
                }
                .onErrorResume(ThisShouldNotHaveBeenThrownException::class) {
                    commandFiredEvent.channel
                        .flatMap { it.createMessage("Something has horribly gone wrong. Please report this to the bot developer with the log.") }
                        .then()
                }
                .onErrorResume { throwable ->
                    commandFiredEvent.channel
                        .flatMap { it.createMessage("Exception happened while handling command: ${throwable.message}") }
                        .then()
                }
        } else {
            when {
                flagParseResult.isFailure() -> event.message.channel
                    .flatMap { it.createMessage(flagParseResult.errorMessage) }
                    .then()
                argumentParseResult.isFailure() -> event.message.channel
                    .flatMap { it.createMessage(argumentParseResult.errorMessage) }
                    .then()
                else -> throw ThisShouldNotHaveBeenThrownException()
            }
        }
    }

    companion object {
        private val ARGUMENT_PARSER = ArgumentCommandParser()
        private val FLAG_PARSER = FlagCommandParser()
        private val LOGGER: Logger by LoggerDelegate()
    }
}
