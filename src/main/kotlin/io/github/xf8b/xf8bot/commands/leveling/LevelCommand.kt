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

package io.github.xf8b.xf8bot.commands.leveling

import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.database.actions.find.GetXpAction
import io.github.xf8b.xf8bot.util.LevelsCalculator
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.immutableListOf
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toMono

// FIXME: not sending an embed
class LevelCommand : Command(
    name = "\${prefix}level",
    description = "Shows your current level, XP and XP needed to advance.",
    commandType = CommandType.LEVELING,
    aliases = immutableListOf("\${prefix}xp"),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.xf8bot.botDatabase
        .execute(GetXpAction(guildId = event.guildId.get(), memberId = event.member.get().id))
        .filter { it.isNotEmpty() }
        .flatMap { it[0].map { row, _ -> row["xp", Long::class.javaObjectType] }.toMono() }
        .cast<Long>()
        .flatMap { xp ->
            event.channel.flatMap {
                it.createEmbedDsl {
                    title("Level")

                    field("XP", xp.toString(), inline = true)
                    field("Level", LevelsCalculator.xpToLevels(xp).toString(), inline = true)
                    field("Remaining XP to next level", "$xp/${LevelsCalculator.remainingXp(xp)}", inline = true)

                    timestamp()
                    footer("Level up by talking!")
                }
            }
        }
        .then()
}