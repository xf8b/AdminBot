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

package io.github.xf8b.xf8bot.util

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.database.actions.find.FindAdministratorRoleAction
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

object PermissionUtil {
    /**
     * Returns a [Boolean] that represents if [firstMember] has a higher or same administrator level than/with [secondMember].
     */
    fun isMemberHigherOrEqual(
        xf8bot: Xf8bot,
        guild: Guild,
        firstMember: Member,
        secondMember: Member
    ): Mono<Boolean> = Mono.zip(
        { it[0] as Int >= it[1] as Int },
        getAdministratorLevel(xf8bot, guild, firstMember),
        getAdministratorLevel(xf8bot, guild, secondMember)
    )

    fun canMemberUseCommand(
        xf8bot: Xf8bot,
        guild: Guild,
        member: Member,
        command: AbstractCommand
    ): Mono<Boolean> = getAdministratorLevel(xf8bot, guild, member).map { it >= command.administratorLevelRequired }

    fun getAdministratorLevel(xf8bot: Xf8bot, guild: Guild, member: Member): Mono<Int> {
        if (member.id == guild.ownerId) return 4.toMono()

        return member.roles
            .map(Role::getId)
            .flatMap { xf8bot.botDatabase.execute(FindAdministratorRoleAction(guild.id, it)) }
            .flatMap { it.toFlux() }
            .flatMap { it.map { row, _ -> row } }
            .map { it["level", Integer::class.java] }
            .sort()
            .cast<Int>()
            .last(0)
    }
}