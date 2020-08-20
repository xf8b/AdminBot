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
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.settings.GuildSettings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrefixCommandHandler extends AbstractCommandHandler {
    public PrefixCommandHandler() {
        super(
                "${prefix}prefix",
                "${prefix}prefix <prefix>",
                "Sets the prefix to the specified prefix.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER,
                1,
                PermissionSet.none(),
                3
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        Message message = event.getMessage();
        String content = message.getContent();
        MessageChannel channel = event.getChannel().block();
        String guildId = event.getGuild().map(Guild::getId).map(Snowflake::asString).block();
        String previousPrefix = GuildSettings.getGuildSettings(guildId).getPrefix();
        String newPrefix = content.trim().split(" ")[1].trim();
        if (previousPrefix.equals(newPrefix)) {
            channel.createMessage("You can't set the prefix to the same thing, silly.").block();
            return;
        }
        GuildSettings.getGuildSettings(guildId).setPrefix(newPrefix);
        channel.createMessage("Successfully set prefix from " + previousPrefix + " to " + newPrefix + ".").block();
    }
}
