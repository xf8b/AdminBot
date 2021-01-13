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

package io.github.xf8b.xf8bot.listeners

import discord4j.core.`object`.entity.User
import discord4j.core.event.ReactiveEventAdapter
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.http.client.ClientException
import io.github.xf8b.utils.exceptions.UnexpectedException
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.parsers.ArgumentCommandInputParser
import io.github.xf8b.xf8bot.api.commands.parsers.FlagCommandInputParser
import io.github.xf8b.xf8bot.commands.info.InfoCommand
import io.github.xf8b.xf8bot.database.actions.find.FindDisabledCommandAction
import io.github.xf8b.xf8bot.util.Checks
import io.github.xf8b.xf8bot.util.LoggerDelegate
import io.github.xf8b.xf8bot.util.PermissionUtil.getAdministratorLevel
import io.github.xf8b.xf8bot.util.toSnowflake
import io.github.xf8b.xf8bot.util.updatedRows
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.extra.bool.logicalOr

class MessageListener(
    private val xf8bot: Xf8bot,
    private val commandRegistry: CommandRegistry
) : ReactiveEventAdapter() {
    override fun onMessageCreate(event: MessageCreateEvent): Mono<Void> {
        if (event.message.content.isBlank()
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
        val foundCommand = findCommand(commandRequested, guildId)

        // FIXME: fix levels

        return /*handleLevels(event).thenEmpty(*/ foundCommand.flatMap { command ->
            xf8bot.botDatabase.execute(FindDisabledCommandAction(guildId.toSnowflake(), command))
                .filter { it.isNotEmpty() }
                .filterWhen { it[0].updatedRows }
                .flatMap {
                    event.guild
                        .flatMap { event.member.get().getAdministratorLevel(xf8bot, it) }
                        .map { it >= 4 }
                }
                .defaultIfEmpty(true)
                .flatMap { allowedToRun ->
                    if (allowedToRun) {
                        event.message.channel.flatMap { it.type().then(onCommandFired(event, command, content)) }
                    } else {
                        event.message.channel
                            .flatMap { it.createMessage("Sorry, but this command has been disabled by an administrator.") }
                            .then()
                    }
                }
        }
        // )
    }

    private fun findCommand(commandRequested: String, guildId: String): Mono<Command> =
        commandRegistry.toFlux()
            .filterWhen { command ->
                command.getNameWithPrefix(xf8bot, guildId)
                    .map { it.equals(commandRequested, ignoreCase = true) }
                    .logicalOr(command.getAliasesWithPrefixes(xf8bot, guildId)
                        .any { alias -> alias.equals(commandRequested, ignoreCase = true) })
            }
            .singleOrEmpty()

    /*
    private fun handleLevels(event: MessageCreateEvent): Mono<Void> = xf8bot.botDatabase
        .execute(GetXpAction(event.guildId.get(), event.member.get().id))
        .filter { it.isNotEmpty() }
        .flatMapMany { it.toFlux() }
        .flatMap { it.map { row, _ -> row } }
        .map { it["xp", java.lang.Long::class.java] }
        .cast<Long>()
        .switchIfEmpty(
            xf8bot.botDatabase
                .execute(AddXpAction(guildId = event.guildId.get(), memberId = event.member.get().id))
                .cast()
        )
        .flatMap { previousXp ->
            val previousLevel = LevelsCalculator.xpToLevels(previousXp as Long)
            val newXp = previousXp + LevelsCalculator.randomXp(5, 25)
            val newLevel = LevelsCalculator.xpToLevels(newXp)

            xf8bot.botDatabase
                .execute(UpdateXpAction(guildId = event.guildId.get(), memberId = event.member.get().id, xp = newXp))
                .then(Mono.zip(previousLevel.toMono(), newLevel.toMono()).flatMap { levelTuple ->
                    if (levelTuple.t1 < levelTuple.t2) {
                        event.message.channel.flatMap {
                            it.createMessage("Congratulations, you've leveled up from ${levelTuple.t1} to ${levelTuple.t2}!")
                        }
                    } else {
                        Mono.empty()
                    }
                })
        }
        .then()
     */

    private fun onCommandFired(event: MessageCreateEvent, command: Command, content: String): Mono<Void> {
        val flagParseResult = FLAG_PARSER.parse(command, content)
        val argumentParseResult = ARGUMENT_PARSER.parse(command, content)

        return if (flagParseResult.isSuccess() && argumentParseResult.isSuccess()) {
            val commandFiredEvent = CommandFiredEvent(
                event,
                xf8bot,
                flagParseResult.result!!,
                argumentParseResult.result!!
            )
            val commandName = command.name.drop(1)

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
                .onErrorResume(UnexpectedException::class) {
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

                else -> throw UnexpectedException()
            }
        }
    }

    companion object {
        private val ARGUMENT_PARSER = ArgumentCommandInputParser()
        private val FLAG_PARSER = FlagCommandInputParser()
        private val LOGGER by LoggerDelegate()
    }
}
