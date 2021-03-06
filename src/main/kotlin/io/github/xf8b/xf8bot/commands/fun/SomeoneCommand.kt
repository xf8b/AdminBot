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

package io.github.xf8b.xf8bot.commands.`fun`

import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.BooleanFlag
import io.github.xf8b.xf8bot.util.extensions.isNotBot
import io.github.xf8b.xf8bot.util.extensions.toSingletonImmutableList
import reactor.core.publisher.Mono

class SomeoneCommand : Command(
    name = "\${prefix}someone",
    description = "Pings a random person.",
    commandType = CommandType.FUN,
    aliases = "@someone".toSingletonImmutableList(),
    flags = IGNORE_BOTS.toSingletonImmutableList()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val membersToPickFrom = event.guild.flatMap { guild ->
            if (event[IGNORE_BOTS] == null || event[IGNORE_BOTS] == false) {
                guild.requestMembers().collectList()
            } else {
                guild.requestMembers().filter { it.isNotBot }.collectList()
            }
        }

        return membersToPickFrom.map { it.random() }
            .flatMap { member -> event.channel.flatMap { it.createMessage(member.nicknameMention) } }
            .then()
    }

    companion object {
        private val IGNORE_BOTS = BooleanFlag(
            shortName = "i",
            longName = "ignoreBots"
        )
    }
}
