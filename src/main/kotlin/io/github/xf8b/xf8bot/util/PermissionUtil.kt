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

package io.github.xf8b.xf8bot.util

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

object PermissionUtil {
    @JvmStatic
    fun isMemberHigher(xf8bot: Xf8bot, guild: Guild, member: Member, otherMember: Member): Mono<Boolean> =
        getAdministratorLevel(xf8bot, guild, member).flatMap { memberAdministratorLevel ->
            getAdministratorLevel(xf8bot, guild, otherMember).map { otherMemberAdministratorLevel ->
                memberAdministratorLevel >= otherMemberAdministratorLevel
            }
        }

    @JvmStatic
    fun canMemberUseCommand(xf8bot: Xf8bot, guild: Guild, member: Member, command: AbstractCommand): Mono<Boolean> =
        getAdministratorLevel(xf8bot, guild, member).map {
            it >= command.administratorLevelRequired
        }

    @JvmStatic
    fun isAdministrator(xf8bot: Xf8bot, guild: Guild, member: Member): Mono<Boolean> {
        if (member.id == guild.ownerId) return true.toMono()
        val guildId = guild.id.asString()
        val mongoCollection = xf8bot.mongoDatabase.getCollection("administratorRoles")
        return member.roles.map(Role::getId).filterWhen {
            mongoCollection.find(and(eq("roleId", it.asLong()), eq("guildId", guildId.toLong()))).toMono()
                .map { true }
                .defaultIfEmpty(false)
        }.count().map { it >= 1 }
    }

    @JvmStatic
    fun getAdministratorLevel(xf8bot: Xf8bot, guild: Guild, member: Member): Mono<Int> {
        if (member.id == guild.ownerId) return 4.toMono()
        val guildId = guild.id.asString()
        val mongoCollection = xf8bot.mongoDatabase.getCollection("administratorRoles")
        return member.roles.map(Role::getId)
            .flatMap { roleId ->
                mongoCollection.find(
                    and(
                        eq("roleId", roleId.asLong()),
                        eq("guildId", guildId.toLong())
                    )
                ).toMono().map { it.getInteger("level") }.defaultIfEmpty(0)
            }
            .sort()
            .last()
    }
}