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

import com.google.common.collect.Range
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono

class UnbanCommand : AbstractCommand(
    name = "\${prefix}unban",
    description = "Unbans the specified member.",
    commandType = CommandType.ADMINISTRATION,
    minimumAmountOfArgs = 1,
    arguments = MEMBER.toSingletonImmutableList(),
    botRequiredPermissions = Permission.BAN_MEMBERS.toSingletonPermissionSet(),
    administratorLevelRequired = 3
) {
    companion object {
        private val MEMBER = StringArgument(
            name = "member",
            index = Range.atLeast(1)
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val memberIdOrUsername = context.getValueOfArgument(MEMBER).get()
        return context.guild.flatMap { guild ->
            guild.bans.filter { ban ->
                val usernameMatches = ban.user.username == memberIdOrUsername
                if (!usernameMatches) {
                    try {
                        ban.user.id.asLong() == memberIdOrUsername
                            .replace("[<@!>]".toRegex(), "")
                            .toLong()
                    } catch (exception: NumberFormatException) {
                        false
                    }
                } else {
                    true
                }
            }.take(1).flatMap { ban ->
                guild.unban(ban.user.id)
                    .then(context.channel.flatMap {
                        it.createMessage("Successfully unbanned ${ban.user.username}!")
                    }).switchIfEmpty(context.channel.flatMap {
                        it.createMessage("The member does not exist or is not banned!")
                    })
            }.then()
        }
    }
}