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

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.ExceptionPredicates
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import io.github.xf8b.xf8bot.util.toSnowflake
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

class ClearCommand : AbstractCommand(
    name = "\${prefix}clear",
    description = "Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 500 or be below 2.",
    commandType = CommandType.ADMINISTRATION,
    aliases = ImmutableList.of("\${prefix}purge"),
    minimumAmountOfArgs = 1,
    arguments = ImmutableList.of(AMOUNT),
    botRequiredPermissions = Permission.MANAGE_MESSAGES.toSingletonPermissionSet(),
    administratorLevelRequired = 2,
) {
    companion object {
        private val AMOUNT = IntegerArgument(
            index = Range.singleton(1),
            name = "amount",
            validityPredicate = {
                try {
                    val amount = it.toInt()
                    amount in 2..500
                } catch (exception: NumberFormatException) {
                    false
                }
            },
            invalidValueErrorMessageFunction = {
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
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> = mono {
        val amountToClear = context.getValueOfArgument(AMOUNT)
            .orElseThrow { ThisShouldNotHaveBeenThrownException() }
        val messagesToDelete = context.channel.flux().flatMap {
            it.getMessagesBefore(Instant.now().toSnowflake())
        }.take(amountToClear.toLong())
        val count = messagesToDelete.count()
        messagesToDelete.transform { (context.channel.block() as TextChannel).bulkDeleteMessages(it) }
            .flatMap(Message::delete)
            .then(count.flatMap { amountDeleted ->
                context.channel.flatMap {
                    it.createMessage("Successfully purged $amountDeleted message(s).")
                }
            }.delayElement(Duration.ofSeconds(3)).flatMap(Message::delete))
            .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10008)) { Mono.empty() } // unknown message
            .awaitFirstOrNull()
    }
}