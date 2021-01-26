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

package io.github.xf8b.xf8bot.util.extensions

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.database.actions.find.FindAdministratorRoleAction
import reactor.core.publisher.Mono

/**
 * Returns a [Boolean] that represents if this member has a higher administrator level than [other].
 */
fun Member.isAdministratorLevelHigher(xf8bot: Xf8bot, other: Member): Mono<Boolean> = Mono.zip(
    { administratorLevels -> administratorLevels[0] as Int > administratorLevels[1] as Int },
    this.getAdministratorLevel(xf8bot),
    other.getAdministratorLevel(xf8bot)
)

fun Member.canUse(xf8bot: Xf8bot, command: Command): Mono<Boolean> = this.getAdministratorLevel(xf8bot)
    .map { it >= command.administratorLevelRequired }

fun Member.getAdministratorLevel(xf8bot: Xf8bot): Mono<Int> = guild.map(Guild::getOwnerId)
    .filter { ownerId -> this.id != ownerId }
    .flatMap { _ ->
        this.guild.flatMap { guild ->
            this.roles.map(Role::getId)
                .flatMap { role -> xf8bot.botDatabase.execute(FindAdministratorRoleAction(guild.id, role)) }
                .flatMap { result -> result.map { row, _ -> row["level", Integer::class.java]!! as Int } }
                .sort()
                .last(0)
        }
    }
    .defaultIfEmpty(4)