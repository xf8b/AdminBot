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

package io.github.xf8b.adminbot.settings;

import io.github.xf8b.adminbot.helpers.PrefixesDatabaseHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
public class GuildSettings {
    private final String guildId;
    private String prefix;
    private static final Set<GuildSettings> GUILD_SETTINGS = new HashSet<>();
    public static final String DEFAULT_PREFIX = ">";

    public GuildSettings(String guildId) {
        //TODO: redo system?
        try {
            if (PrefixesDatabaseHelper.doesGuildNotExistInDatabase(guildId)) {
                PrefixesDatabaseHelper.insertIntoPrefixes(guildId, DEFAULT_PREFIX);
                this.prefix = DEFAULT_PREFIX;
            } else {
                this.prefix = PrefixesDatabaseHelper.readFromPrefixes(guildId);
            }
        } catch (ClassNotFoundException | SQLException exception) {
            LOGGER.error(String.format("An exception happened while trying to check if the guild with the id %s did not exist in the prefix database/while trying to insert into the prefix database!", guildId), exception);
        }
        this.guildId = guildId;
        GUILD_SETTINGS.add(this);
    }

    public static GuildSettings getGuildSettings(String guildId) {
        for (GuildSettings guildSettings : GUILD_SETTINGS) {
            if (guildSettings.guildId.equals(guildId)) {
                return guildSettings;
            }
        }
        return new GuildSettings(guildId);
    }

    private void read() {
        try {
            this.prefix = PrefixesDatabaseHelper.readFromPrefixes(guildId);
        } catch (ClassNotFoundException | SQLException exception) {
            LOGGER.error("An exception happened while trying to read to the prefix database!", exception);
        }
    }

    private void write() {
        try {
            PrefixesDatabaseHelper.overwritePrefixForGuild(guildId, this.prefix);
        } catch (ClassNotFoundException | SQLException exception) {
            LOGGER.error("An exception happened while trying to write to the prefix database!", exception);
        }
    }

    public String getPrefix() {
        read();
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        write();
    }
}
