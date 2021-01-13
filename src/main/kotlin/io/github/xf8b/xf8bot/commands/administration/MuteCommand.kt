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
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.api.commands.flags.TimeFlag
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono

class MuteCommand : Command(
    name = "\${prefix}mute",
    description = """
    Mutes the specified member for the specified amount of time. 
    :warning: Not done yet, running will cause an error.
    """.trimIndent(),
    commandType = CommandType.ADMINISTRATION,
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

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        TODO("not done yet, please come back later")
    }
}
