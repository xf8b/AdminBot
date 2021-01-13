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

import com.google.common.collect.Range
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import reactor.core.publisher.Mono
import java.util.concurrent.ThreadLocalRandom

class SlapCommand : Command(
    name = "\${prefix}slap",
    description = """
    Slaps the person.
    Possible items:
    ${ITEMS.joinToString("\n") { "- $it" }}
    """.trimIndent(),
    commandType = CommandType.FUN,
    arguments = PERSON.toSingletonImmutableList()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.guild
        .flatMap { it.selfMember }
        .map { it.displayName }
        .flatMap { selfDisplayName ->
            var senderUsername = event.member.get().displayName
            var personToSlap = event[PERSON]!!
            val itemToUse = ITEMS[ThreadLocalRandom.current().nextInt(ITEMS.size)]

            if (personToSlap.equals(selfDisplayName, ignoreCase = true)) {
                personToSlap = senderUsername
                senderUsername = selfDisplayName
            }

            event.channel.flatMap {
                it.createMessage("$senderUsername slapped $personToSlap with a $itemToUse!")
            }
        }.then()


    companion object {
        private val PERSON = StringArgument(
            name = "person",
            index = Range.atLeast(1)
        )

        val ITEMS = arrayOf(
            "large bat",
            "large trout",
            "wooden door",
            "metal pipe",
            "vent",
            "glass bottle"
        )
    }
}
