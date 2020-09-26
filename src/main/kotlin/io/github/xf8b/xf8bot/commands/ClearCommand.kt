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

package io.github.xf8b.xf8bot.commands

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.ClientExceptionUtil
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant

class ClearCommand : AbstractCommand(
        name = "\${prefix}clear",
        description = "Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 500 or be below 2.",
        commandType = CommandType.ADMINISTRATION,
        aliases = ImmutableList.of("\${prefix}purge"),
        minimumAmountOfArgs = 1,
        arguments = ImmutableList.of(AMOUNT),
        botRequiredPermissions = PermissionSet.of(Permission.MANAGE_MESSAGES),
        administratorLevelRequired = 2,
) {
    companion object {
        private val AMOUNT = IntegerArgument.builder()
                .setIndex(Range.singleton(1))
                .setName("amount")
                .setValidityPredicate {
                    try {
                        val amount = it.toInt()
                        amount in 2..500
                    } catch (exception: NumberFormatException) {
                        false
                    }
                }
                .setInvalidValueErrorMessageFunction {
                    try {
                        val amount = it.toInt()
                        when {
                            amount < 2 -> "Sorry, but you cannot clear less than 2 messages."
                            amount > 500 -> "Sorry, but you cannot clear more than 500 messages."
                            else -> throw ThisShouldNotHaveBeenThrownException()
                        }
                    } catch (exception: NumberFormatException) {
                        Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                    }
                }
                .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val amountToClear = event.getValueOfArgument(AMOUNT)
                .orElseThrow { ThisShouldNotHaveBeenThrownException() }
        val amountOfMessagesPurged: Long = event.channel.flux().flatMap {
            it.getMessagesBefore(Snowflake.of(Instant.now()))
        }.take(amountToClear.toLong()).count().blockOptional().orElseThrow {
            ThisShouldNotHaveBeenThrownException()
        }
        return event.channel.flux().flatMap { it.getMessagesBefore(Snowflake.of(Instant.now())) }
                .take(amountToClear.toLong())
                .transform { (event.channel.block() as TextChannel).bulkDeleteMessages(it) }
                .flatMap { it.delete() }
                .doOnComplete {
                    event.channel.flatMap { it.createMessage("Successfully purged $amountOfMessagesPurged message(s).") }
                            .delayElement(Duration.ofSeconds(3))
                            .flatMap { it.delete() }
                            .subscribe()
                }
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10008)) { Flux.empty() } //unknown message
                .subscribeOn(Schedulers.boundedElastic())
                .then()
    }
}