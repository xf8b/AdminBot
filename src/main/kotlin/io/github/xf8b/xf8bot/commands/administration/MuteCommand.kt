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

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import discord4j.common.util.Snowflake
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.api.commands.flags.TimeFlag
import io.github.xf8b.xf8bot.util.ParsingUtil
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast

class MuteCommand : AbstractCommand(
    name = "\${prefix}mute",
    description = """
                Mutes the specified member for the specified amount of time. 
                :warning: Currently incomplete.
                """.trimIndent(),
    commandType = CommandType.ADMINISTRATION,
    minimumAmountOfArgs = 2,
    flags = ImmutableList.of(MEMBER, TIME),
    botRequiredPermissions = Permission.MANAGE_ROLES.toSingletonPermissionSet(),
    administratorLevelRequired = 1
) {
    companion object {
        private val MEMBER = StringFlag(
            shortName = "m",
            longName = "member"
        )

        private val TIME = TimeFlag(
            shortName = "t",
            longName = "time"
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> =
        ParsingUtil.parseUserId(context.guild, context.getValueOfFlag(MEMBER).get())
            .map(Snowflake::of)
            .switchIfEmpty(context.channel.flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap {
                context.channel.flatMap {
                    it.createMessage("This command is not complete yet!")
                    TODO("gotta fix this")
                }
            }
}
