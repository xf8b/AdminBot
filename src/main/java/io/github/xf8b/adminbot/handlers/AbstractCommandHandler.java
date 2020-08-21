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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.settings.GuildSettings;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class AbstractCommandHandler {
    @NonNull
    private final String name;
    private final String usage;
    @NonNull
    private final String description;
    @NonNull
    private final CommandType commandType;
    private final Map<String, String> actions;
    private final List<String> aliases;
    private final int minimumAmountOfArgs;
    private final PermissionSet botRequiredPermissions;
    private final int administratorLevelRequired;
    private final boolean botAdministratorOnly;

    public AbstractCommandHandler(AbstractCommandHandlerBuilder builder) {
        this.name = builder.name;
        if (builder.usage == null) {
            this.usage = name;
        } else {
            this.usage = builder.usage;
        }
        this.description = builder.description;
        this.commandType = builder.commandType;
        this.actions = ImmutableMap.copyOf(builder.actions);
        this.aliases = ImmutableList.copyOf(builder.aliases);
        this.minimumAmountOfArgs = builder.minimumAmountOfArgs;
        this.botRequiredPermissions = builder.botRequiredPermissions;
        this.administratorLevelRequired = builder.administratorLevelRequired;
        this.botAdministratorOnly = builder.botAdministratorOnly;
    }

    public static AbstractCommandHandlerBuilder builder() {
        return new AbstractCommandHandlerBuilder();
    }

    public abstract void onCommandFired(CommandFiredEvent event);

    public String getNameWithPrefix(String guildId) {
        return name.replace("${prefix}", GuildSettings.getGuildSettings(guildId).getPrefix());
    }

    public String getUsageWithPrefix(String guildId) {
        return usage.replace("${prefix}", GuildSettings.getGuildSettings(guildId).getPrefix());
    }

    public List<String> getAliasesWithPrefixes(String guildId) {
        List<String> aliasesWithPrefixes = new ArrayList<>(aliases);
        aliasesWithPrefixes.replaceAll(string -> string.replace("${prefix}", GuildSettings.getGuildSettings(guildId).getPrefix()));
        return aliasesWithPrefixes;
    }

    public boolean requiresAdministrator() {
        return administratorLevelRequired > 0;
    }

    @Getter
    @RequiredArgsConstructor
    public enum CommandType {
        ADMINISTRATION("Commands related with administration."),
        BOT_ADMINISTRATOR("Commands only for bot administrators."),
        OTHER("Other commands which do not fit in any of the above categories.");

        private final String description;
    }

    @EqualsAndHashCode
    @ToString
    public static class AbstractCommandHandlerBuilder {
        private String name = null;
        private String usage = null;
        private String description = null;
        private CommandType commandType = null;
        private Map<String, String> actions = new HashMap<>();
        private List<String> aliases = new ArrayList<>();
        private int minimumAmountOfArgs = 0;
        private PermissionSet botRequiredPermissions = PermissionSet.none();
        private int administratorLevelRequired = 0;
        private boolean botAdministratorOnly = false;

        public AbstractCommandHandlerBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public AbstractCommandHandlerBuilder setUsage(String usage) {
            this.usage = usage;
            return this;
        }

        public AbstractCommandHandlerBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public AbstractCommandHandlerBuilder setCommandType(CommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        public AbstractCommandHandlerBuilder addAction(String name, String description) {
            this.actions.put(name, description);
            return this;
        }

        public AbstractCommandHandlerBuilder setActions(Map<String, String> actions) {
            this.actions = actions;
            return this;
        }

        public AbstractCommandHandlerBuilder addAlias(String alias) {
            this.aliases.add(alias);
            return this;
        }

        public AbstractCommandHandlerBuilder setAliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public AbstractCommandHandlerBuilder setMinimumAmountOfArgs(int minimumAmountOfArgs) {
            this.minimumAmountOfArgs = minimumAmountOfArgs;
            return this;
        }

        public AbstractCommandHandlerBuilder setBotRequiredPermissions(PermissionSet botRequiredPermissions) {
            this.botRequiredPermissions = botRequiredPermissions;
            return this;
        }

        public AbstractCommandHandlerBuilder setAdministratorLevelRequired(int administratorLevelRequired) {
            this.administratorLevelRequired = administratorLevelRequired;
            return this;
        }

        public AbstractCommandHandlerBuilder setBotAdministratorOnly() {
            this.botAdministratorOnly = true;
            return this;
        }
    }
}
