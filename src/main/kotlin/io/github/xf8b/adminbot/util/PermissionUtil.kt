/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.util

import com.mongodb.client.model.Filters
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import io.github.xf8b.adminbot.AdminBot
import io.github.xf8b.adminbot.api.commands.AbstractCommand
import reactor.core.publisher.Mono

class PermissionUtil {
    companion object {
        @JvmStatic
        fun isMemberHigher(adminBot: AdminBot, guild: Guild, member: Member, otherMember: Member): Boolean {
            return getAdministratorLevel(adminBot, guild, member) > getAdministratorLevel(adminBot, guild, otherMember)
        }

        @JvmStatic
        fun canMemberUseCommand(adminBot: AdminBot, guild: Guild, member: Member, command: AbstractCommand): Boolean =
                getAdministratorLevel(adminBot, guild, member) >= command.administratorLevelRequired

        @JvmStatic
        fun isAdministrator(adminBot: AdminBot, guild: Guild, member: Member): Boolean {
            if (member.id == guild.ownerId) return true
            val guildId = guild.id.asString()
            val mongoCollection = adminBot.mongoDatabase.getCollection("administratorRoles")
            return member.roles.map(Role::getId).any { roleId: Snowflake ->
                Mono.from(mongoCollection.find(Filters.and(
                        Filters.eq("roleId", roleId.asLong()),
                        Filters.eq("guildId", guildId.toLong())
                ))).map { true }.defaultIfEmpty(false).block()!!
            }.block()!!
        }

        @JvmStatic
        fun getAdministratorLevel(adminBot: AdminBot, guild: Guild, member: Member): Int {
            if (member.id == guild.ownerId) return 4
            val guildId = guild.id.asString()
            val mongoCollection = adminBot.mongoDatabase.getCollection("administratorRoles")
            return member.roles.map(Role::getId).map { roleId: Snowflake ->
                Mono.from(mongoCollection.find(Filters.and(
                        Filters.eq("roleId", roleId.asLong()),
                        Filters.eq("guildId", guildId.toLong())
                ))).map { it.getInteger("level") }.defaultIfEmpty(0).block()!!
            }.sort().blockLast()!!
        }
    }
}