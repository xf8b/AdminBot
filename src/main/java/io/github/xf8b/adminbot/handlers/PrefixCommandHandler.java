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

import com.google.common.collect.Range;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import io.github.xf8b.adminbot.data.GuildData;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class PrefixCommandHandler extends AbstractCommandHandler {
    private static final StringArgument NEW_PREFIX = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("prefix")
            .setRequired(false)
            .build();

    public PrefixCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}prefix")
                .setDescription("Sets the prefix to the specified prefix.")
                .setCommandType(CommandType.OTHER)
                .setMinimumAmountOfArgs(1)
                .addArgument(NEW_PREFIX)
                .setAdministratorLevelRequired(4));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Snowflake guildId = event.getGuild().map(Guild::getId).block();
        String previousPrefix = GuildData.getGuildData(guildId).getPrefix();
        Optional<String> newPrefix = event.getValueOfArgument(NEW_PREFIX);
        if (newPrefix.isEmpty()) {
            //reset prefix
            GuildData.getGuildData(guildId).setPrefix(GuildData.DEFAULT_PREFIX);
            channel.createMessage("Successfully reset prefix.").block();
        } else if (previousPrefix.equals(newPrefix.get())) {
            channel.createMessage("You can't set the prefix to the same thing, silly.").block();
        } else {
            //set prefix
            GuildData.getGuildData(guildId).setPrefix(newPrefix.get());
            channel.createMessage("Successfully set prefix from " + previousPrefix + " to " + newPrefix.get() + ".").block();
        }
    }
}
