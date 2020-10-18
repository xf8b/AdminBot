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

package io.github.xf8b.xf8bot.listeners

import com.mongodb.MongoCommandException
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.commands.InfoCommand
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.settings.CommandHandlerChecks
import io.github.xf8b.xf8bot.settings.DisableChecks
import io.github.xf8b.xf8bot.util.LoggerDelegate
import io.github.xf8b.xf8bot.util.PermissionUtil.canMemberUseCommand
import io.github.xf8b.xf8bot.util.Result
import io.github.xf8b.xf8bot.util.parser.ArgumentParser
import io.github.xf8b.xf8bot.util.parser.FlagParser
import io.github.xf8b.xf8bot.util.toSnowflake
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.Document
import org.slf4j.Logger
import reactor.core.publisher.Mono

class MessageListener(
        private val xf8bot: Xf8bot,
        private val commandRegistry: CommandRegistry
) : EventListener<MessageCreateEvent> {
    companion object {
        private val ARGUMENT_PARSER = ArgumentParser()
        private val FLAG_PARSER = FlagParser()
        private val LOGGER: Logger by LoggerDelegate()
    }

    override fun onEventFired(event: MessageCreateEvent): Mono<MessageCreateEvent> {
        //TODO: reactify all the classes
        //TODO: make exception handler
        //TODO: add spam protection
        val message = event.message
        val content = message.content
        val guild = event.guild.block()!!
        val guildId = guild.id.asString()
        if (content.trim() == "<@!${event.client.selfId.asString()}> help") {
            val commandHandler = commandRegistry.getCommandHandler(InfoCommand::class.java)
            return onCommandFired(event, commandHandler, guildId, content).thenReturn(event)
        }
        val mongoCollection = xf8bot.mongoDatabase.getCollection("prefixes")
        xf8bot.prefixCache.getPrefix(guildId.toSnowflake())
                .switchIfEmpty(Mono.from(mongoCollection.insertOne(Document()
                        .append("guildId", guildId.toLong())
                        .append("prefix", Xf8bot.DEFAULT_PREFIX)))
                        .thenReturn(Xf8bot.DEFAULT_PREFIX))
                .block()
        val commandType = content.trim().split(" ").toTypedArray()[0]
        for (commandHandler in commandRegistry) {
            val name = commandHandler.getNameWithPrefix(xf8bot, guildId)
            val aliases = commandHandler.getAliasesWithPrefixes(xf8bot, guildId)
            if (commandType.equals(name, ignoreCase = true)) {
                return onCommandFired(event, commandHandler, guildId, content).thenReturn(event)
            } else if (aliases.isNotEmpty()) {
                for (alias in aliases) {
                    if (commandType.equals(alias, ignoreCase = true)) {
                        return onCommandFired(event, commandHandler, guildId, content).thenReturn(event)
                    }
                }
            }
        }
        return Mono.just(event)
    }

    private fun onCommandFired(event: MessageCreateEvent, commandHandler: AbstractCommand, guildId: String, content: String): Mono<Void> {
        val flagParseResult = FLAG_PARSER.parse(commandHandler, content)
        val argumentParseResult = ARGUMENT_PARSER.parse(commandHandler, content)
        return if (flagParseResult.resultType !== Result.ResultType.FAILURE && argumentParseResult.resultType !== Result.ResultType.FAILURE) {
            val commandFiredEvent = CommandFiredEvent(
                    xf8bot,
                    flagParseResult.result!!,
                    argumentParseResult.result!!,
                    event
            )
            val disableChecksAnnotation: DisableChecks? = commandHandler.javaClass.getAnnotation(DisableChecks::class.java)
            mono {
                commandFiredEvent.guild
                        .flatMap { it.selfMember }
                        .flatMap { it.basePermissions }
                        .map { it.containsAll(commandHandler.botRequiredPermissions) || it.contains(Permission.ADMINISTRATOR) }
                        .filter {
                            if (disableChecksAnnotation != null) {
                                if (disableChecksAnnotation.value.asList()
                                                .contains(CommandHandlerChecks.BOT_HAS_REQUIRED_PERMISSIONS)) {
                                    return@filter true
                                }
                            }
                            if (!it) {
                                commandFiredEvent.channel
                                        .flatMap { messageChannel: MessageChannel ->
                                            messageChannel
                                                    .createMessage("Could not execute command \"${commandHandler.getNameWithPrefix(xf8bot, guildId)}\" because of insufficient permissions!")
                                        }
                                        .subscribe()
                                false
                            } else {
                                true
                            }
                        }
                        .filterWhen {
                            if (disableChecksAnnotation != null) {
                                if (disableChecksAnnotation.value.asList()
                                                .contains(CommandHandlerChecks.IS_ADMINISTRATOR)) {
                                    return@filterWhen Mono.just(true)
                                }
                            }
                            if (commandHandler.requiresAdministrator()) {
                                return@filterWhen canMemberUseCommand(xf8bot, commandFiredEvent.guild.block()!!, commandFiredEvent.member.get(), commandHandler)
                                        .doOnNext {
                                            if (!it) {
                                                commandFiredEvent.channel
                                                        .flatMap { it.createMessage("Sorry, you don't have high enough permissions.") }
                                                        .subscribe()
                                            }
                                        }
                            } else {
                                Mono.just(true)
                            }
                        }
                        .filterWhen {
                            if (disableChecksAnnotation != null) {
                                if (disableChecksAnnotation.value.asList()
                                                .contains(CommandHandlerChecks.IS_BOT_ADMINISTRATOR)) {
                                    return@filterWhen Mono.just(true)
                                }
                            }
                            if (commandHandler.isBotAdministratorOnly) {
                                if (!commandFiredEvent.xf8bot.isBotAdministrator(commandFiredEvent.member.get().id)) {
                                    return@filterWhen commandFiredEvent.channel
                                            .flatMap { it.createMessage("Sorry, you aren't a administrator of xf8bot.") }
                                            .thenReturn(false)
                                }
                            }
                            return@filterWhen Mono.just(true)
                        }
                        .flatMap { bool: Boolean ->
                            if (disableChecksAnnotation != null) {
                                if (disableChecksAnnotation.value.asList()
                                                .contains(CommandHandlerChecks.SURPASSES_MINIMUM_AMOUNT_OF_ARGUMENTS)) {
                                    Mono.just(bool)
                                }
                            }
                            if (content.trim().split(" ").toTypedArray().size < commandHandler.minimumAmountOfArgs + 1) {
                                commandFiredEvent.message.channel
                                        .flatMap { it.createMessage("Huh? Could you repeat that? The usage of this command is: `" + commandHandler.getUsageWithPrefix(xf8bot, guildId) + "`.") }
                                        .subscribe()
                                Mono.empty()
                            } else {
                                Mono.just(bool)
                            }
                        }
                        .flatMap { commandHandler.onCommandFired(commandFiredEvent) }
                        .doOnError { LOGGER.error("An error happened while handling commands!", it) }
                        .onErrorResume(ClientException::class.java) { exception ->
                            commandFiredEvent.channel
                                    .flatMap { it.createMessage("Client exception happened while handling command: " + exception.status + " " + exception.errorResponse.get().fields) }
                                    .then()
                        }
                        .onErrorResume(MongoCommandException::class.java) { exception ->
                            commandFiredEvent.channel
                                    .flatMap { it.createMessage("Database error happened while handling command: " + exception.errorCodeName) }
                                    .then()
                        }
                        .onErrorResume(ThisShouldNotHaveBeenThrownException::class.java) {
                            commandFiredEvent.channel
                                    .flatMap { it.createMessage("Something has horribly gone wrong. Please report this to the bot developer with the log.") }
                                    .then()
                        }
                        .onErrorResume { t ->
                            commandFiredEvent.channel
                                    .flatMap { it.createMessage("Exception happened while handling command: " + t.message) }
                                    .then()
                        }.awaitFirstOrNull()
            }
        } else {
            when {
                flagParseResult.resultType === Result.ResultType.FAILURE -> event.message
                        .channel
                        .flatMap { it.createMessage(flagParseResult.errorMessage) }
                        .then()
                argumentParseResult.resultType === Result.ResultType.FAILURE -> event.message
                        .channel
                        .flatMap { it.createMessage(argumentParseResult.errorMessage) }
                        .then()
                else -> throw ThisShouldNotHaveBeenThrownException()
            }
        }
    }
}
