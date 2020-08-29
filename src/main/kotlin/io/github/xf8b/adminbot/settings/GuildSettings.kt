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
package io.github.xf8b.adminbot.settings

import io.github.xf8b.adminbot.helpers.PrefixesDatabaseHelper
import io.github.xf8b.adminbot.util.LoggerDelegate
import org.slf4j.Logger
import java.sql.SQLException
import java.util.*

data class GuildSettings(val guildId: String) {
    private val logger: Logger by LoggerDelegate()

    //TODO: impl setting that allows you to switch from `>ban bruhman | bruh` to `>ban -m bruhman -r bruh`
    private var prefix: String = DEFAULT_PREFIX
    private var areFlagsEnabled: Boolean = true

    private fun read() {
        try {
            prefix = PrefixesDatabaseHelper.get(guildId)
        } catch (exception: ClassNotFoundException) {
            logger.error("An exception happened while trying to read to the prefix database!", exception)
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read to the prefix database!", exception)
        }
    }

    private fun write() {
        try {
            PrefixesDatabaseHelper.overwrite(guildId, prefix)
        } catch (exception: ClassNotFoundException) {
            logger.error("An exception happened while trying to write to the prefix database!", exception)
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to write to the prefix database!", exception)
        }
    }

    fun getPrefix(): String {
        read()
        return prefix
    }

    fun setPrefix(prefix: String) {
        this.prefix = prefix
        write()
    }

    companion object {
        private val GUILD_SETTINGS: MutableSet<GuildSettings> = HashSet()
        const val DEFAULT_PREFIX = ">"

        @JvmStatic
        fun getGuildSettings(guildId: String): GuildSettings = GUILD_SETTINGS.firstOrNull {
            it.guildId == guildId
        } ?: GuildSettings(guildId)
    }

    init {
        //TODO: redo system?
        try {
            prefix = if (PrefixesDatabaseHelper.isNotInDatabase(guildId)) {
                PrefixesDatabaseHelper.add(guildId, DEFAULT_PREFIX)
                DEFAULT_PREFIX
            } else {
                PrefixesDatabaseHelper.get(guildId)
            }
        } catch (exception: ClassNotFoundException) {
            logger.error(String.format(
                    "An exception happened while trying to check if the guild with the id %s did not exist in the prefix database/while trying to insert into the prefix database!",
                    guildId
            ), exception)
        } catch (exception: SQLException) {
            logger.error(String.format(
                    "An exception happened while trying to check if the guild with the id %s did not exist in the prefix database/while trying to insert into the prefix database!",
                    guildId
            ), exception)
        }
        GUILD_SETTINGS.add(this)
    }
}