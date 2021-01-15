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

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.util.Permission
import io.github.xf8b.utils.exceptions.UnexpectedException
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.Argument
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument
import io.github.xf8b.xf8bot.util.Checks
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import io.github.xf8b.xf8bot.util.toSnowflake
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.extra.bool.logicalAnd
import java.time.Duration
import java.time.Instant

class ClearCommand : Command(
    name = "\${prefix}clear",
    description = "Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 500 or be below 2.",
    commandType = CommandType.ADMINISTRATION,
    aliases = ImmutableList.of("\${prefix}purge"),
    arguments = ImmutableList.of(AMOUNT),
    botRequiredPermissions = Permission.MANAGE_MESSAGES.toSingletonPermissionSet(),
    administratorLevelRequired = 2,
) {
    companion object {
        private val AMOUNT = IntegerArgument(
            index = Range.singleton(0),
            name = "amount",
            validityPredicate = {
                try {
                    val amount = it.toInt()
                    amount in 2..500
                } catch (exception: NumberFormatException) {
                    false
                }
            },
            errorMessageFunction = {
                try {
                    val amount = it.toInt()
                    when {
                        amount < 2 -> "Sorry, but you cannot clear less than 2 messages."
                        amount > 500 -> "Sorry, but you cannot clear more than 500 messages."
                        else -> throw UnexpectedException()
                    }
                } catch (exception: NumberFormatException) {
                    Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            }
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = mono {
        val amountToClear = event[AMOUNT]!!
        val messagesToDelete = event.channel
            .flatMapMany { it.getMessagesBefore(Instant.now().toSnowflake()) }
            .take(amountToClear.toLong())
            .cache()
        val count = messagesToDelete.count().cache()
        val leftoverMessages = messagesToDelete
            .transform { messages ->
                event.channel.cast<TextChannel>().flatMapMany {
                    it.bulkDeleteMessages(messages)
                        .onErrorResume(Checks.isClientExceptionWithCode(10008)) { Mono.empty() } // unknown message
                }
            }
        val leftoverCount = leftoverMessages.count()

        leftoverMessages.repeatWhen {
            leftoverCount.map { it != 1L && it != 0L }.logicalAnd(leftoverMessages.all {
                !it.id.timestamp.isBefore(Instant.now().minus(Duration.ofDays(14L)))
            })
        }.limitRate(10, 3)
            .flatMap {
                it.delete()
            }
            .then(count.flatMap { amountDeleted ->
                event.channel
                    .flatMap { it.createMessage("Successfully purged $amountDeleted message(s).") }
                    .delayElement(Duration.ofSeconds(3))
                    .flatMap(Message::delete)
            })
            .awaitFirstOrNull()
    }
}