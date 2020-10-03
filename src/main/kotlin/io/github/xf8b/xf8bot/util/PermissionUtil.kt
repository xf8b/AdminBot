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
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import reactor.core.publisher.Mono

object PermissionUtil {
    @JvmStatic
    fun isMemberHigher(xf8bot: Xf8bot, guild: Guild, member: Member, otherMember: Member): Boolean =
            getAdministratorLevel(xf8bot, guild, member) > getAdministratorLevel(xf8bot, guild, otherMember)

    @JvmStatic
    fun canMemberUseCommand(xf8bot: Xf8bot, guild: Guild, member: Member, command: AbstractCommand): Boolean =
            getAdministratorLevel(xf8bot, guild, member) >= command.administratorLevelRequired

    @JvmStatic
    fun isAdministrator(xf8bot: Xf8bot, guild: Guild, member: Member): Boolean {
        if (member.id == guild.ownerId) return true
        val guildId = guild.id.asString()
        val mongoCollection = xf8bot.mongoDatabase.getCollection("administratorRoles")
        return member.roles.map(Role::getId).any {
            Mono.from(mongoCollection.find(and(eq("roleId", it.asLong()), eq("guildId", guildId.toLong()))))
                    .map { true }
                    .defaultIfEmpty(false)
                    .block()!!
        }.block()!!
    }

    @JvmStatic
    fun getAdministratorLevel(xf8bot: Xf8bot, guild: Guild, member: Member): Int {
        if (member.id == guild.ownerId) return 4
        val guildId = guild.id.asString()
        val mongoCollection = xf8bot.mongoDatabase.getCollection("administratorRoles")
        return member.roles.map(Role::getId)
                .map { roleId: Snowflake ->
                    Mono.from(mongoCollection.find(and(eq("roleId", roleId.asLong()), eq("guildId", guildId.toLong()))))
                            .map { it.getInteger("level") }
                            .defaultIfEmpty(0)
                            .block()!!
                }
                .sort()
                .blockLast()!!
    }
}