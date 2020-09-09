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
package io.github.xf8b.adminbot.data

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import discord4j.common.util.Snowflake
import io.github.xf8b.adminbot.helpers.AdministratorsDatabaseHelper
import io.github.xf8b.adminbot.helpers.PrefixesDatabaseHelper
import io.github.xf8b.adminbot.util.LoggerDelegate
import org.slf4j.Logger
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION") //for the database helper deprecations
data class GuildData(val guildId: Snowflake) {
    private val logger: Logger by LoggerDelegate()

    //TODO: impl setting that allows you to switch from `>ban bruhman | bruh` to `>ban -m bruhman -r bruh`
    private var prefix: String = DEFAULT_PREFIX
    private var administratorRoles: MutableMap<Long, Int> = HashMap()
    //private var areFlagsEnabled: Boolean = true

    init {
        //TODO: redo system?
        try {
            prefix = if (PrefixesDatabaseHelper.isNotInDatabase(guildId.asLong())) {
                PrefixesDatabaseHelper.add(guildId.asLong(), DEFAULT_PREFIX)
                DEFAULT_PREFIX
            } else {
                PrefixesDatabaseHelper.get(guildId.asLong())
            }
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read/write from/to the prefix database!", exception)
        }
        try {
            administratorRoles = AdministratorsDatabaseHelper.getAllRoles(guildId.asLong())
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read from the administrators database!", exception)
        }
    }

    companion object {
        private val GUILD_DATA_CACHE: LoadingCache<Snowflake, GuildData> = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(::GuildData)
        const val DEFAULT_PREFIX = ">"

        @JvmStatic
        fun getGuildData(guildId: String): GuildData = getGuildData(Snowflake.of(guildId))

        @JvmStatic
        fun getGuildData(guildId: Snowflake): GuildData = GUILD_DATA_CACHE.get(guildId)!!
    }

    private fun readAdministratorRoles(): Map<Long, Int> {
        try {
            administratorRoles = AdministratorsDatabaseHelper.getAllRoles(guildId.asLong())
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read from the administrators database!", exception)
        }
        return administratorRoles
    }

    fun getPrefix(): String {
        try {
            prefix = PrefixesDatabaseHelper.get(guildId.asLong())
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read from the prefix database!", exception)
        }
        return prefix
    }

    fun setPrefix(prefix: String) {
        this.prefix = prefix
        try {
            PrefixesDatabaseHelper.overwrite(guildId.asLong(), prefix)
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to write to the prefix database!", exception)
        }
    }

    fun getAdministratorRoles() = readAdministratorRoles()

    fun addAdministratorRole(administratorRoleId: Snowflake, level: Int) = addAdministratorRole(administratorRoleId.asLong(), level)

    private fun addAdministratorRole(administratorRoleId: Long, level: Int) {
        try {
            AdministratorsDatabaseHelper.add(guildId.asLong(), administratorRoleId, level)
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to write to the administrators database!", exception)
        }
        readAdministratorRoles()
    }

    fun removeAdministratorRole(administratorRoleId: Snowflake) = removeAdministratorRole(administratorRoleId.asLong())

    fun removeAdministratorRole(administratorRoleId: Long) {
        try {
            AdministratorsDatabaseHelper.remove(guildId.asLong(), administratorRoleId)
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to write to the administrators database!", exception)
        }
        readAdministratorRoles()
    }

    fun hasAdministratorRole(administratorRoleId: Snowflake): Boolean = hasAdministratorRole(administratorRoleId.asLong())

    private fun hasAdministratorRole(administratorRoleId: Long): Boolean = readAdministratorRoles().containsKey(administratorRoleId)

    fun getLevelOfAdministratorRole(administratorRoleId: Snowflake): Int = getLevelOfAdministratorRole(administratorRoleId.asLong())

    private fun getLevelOfAdministratorRole(administratorRoleId: Long): Int = readAdministratorRoles()[administratorRoleId]
            ?: error("No role with id $administratorRoleId is in the administrator roles!")
}