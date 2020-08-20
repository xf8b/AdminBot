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

package io.github.xf8b.adminbot.handlers;

import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.settings.GuildSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public abstract class AbstractCommandHandler {
    private final String name;
    private final String usage;
    private final String description;
    private final Map<String, String> actions;
    private final List<String> aliases;
    private final CommandType commandType;
    private final int minimumAmountOfArgs;
    private final PermissionSet botRequiredPermissions;
    private final int levelRequired;

    public abstract void onCommandFired(CommandFiredEvent event);

    public String getNameWithPrefix(String guildId) {
        return name.replace("${prefix}", GuildSettings.getGuildSettings(guildId).getPrefix());
    }

    public String getUsageWithPrefix(String guildId) {
        return usage.replace("${prefix}", GuildSettings.getGuildSettings(guildId).getPrefix());
    }

    public List<String> getAliasesWithPrefixes(String guildId) {
        List<String> aliasesWithPrefixes = new ArrayList<>();
        for (String string : aliases) {
            aliasesWithPrefixes.add(string.replace("${prefix}", GuildSettings.getGuildSettings(guildId).getPrefix()));
        }
        return aliasesWithPrefixes;
    }

    public boolean requiresAdministrator() {
        return levelRequired > 0;
    }

    public enum CommandType {
        ADMINISTRATION("Commands related with administration."),
        OTHER("Other commands which do not fit in any of the above categories.");

        private final String description;

        CommandType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
