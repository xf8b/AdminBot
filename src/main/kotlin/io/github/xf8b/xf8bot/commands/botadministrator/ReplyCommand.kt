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

package io.github.xf8b.xf8bot.commands.botadministrator

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.createMessageDsl
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Mono

class ReplyCommand : AbstractCommand(
    name = "\${prefix}reply",
    description = "Replies to the specified message with the passed in content.",
    commandType = CommandType.BOT_ADMINISTRATOR,
    arguments = ImmutableList.of(CONTENT, MESSAGE_ID),
    minimumAmountOfArgs = 1,
    botAdministratorOnly = true
) {
    companion object {
        private val MESSAGE_ID = StringArgument(
            name = "message id",
            index = Range.singleton(1)
        )
        private val CONTENT = StringArgument(
            name = "content",
            index = Range.atLeast(2)
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> = context.channel.flatMap {
        it.createMessageDsl {
            content(context.getValueOfArgument(CONTENT).get())

            messageReference(context.getValueOfArgument(MESSAGE_ID).get().toSnowflake())
        }
    }.then(context.message.delete())
}