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

import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.settings.GuildSettings;

public class InfoCommandHandler extends AbstractCommandHandler {
    public InfoCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}information")
                .setDescription("Shows some information about me.")
                .setCommandType(CommandType.OTHER)
                .addAlias("${prefix}info")
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS)));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel messageChannel = event.getChannel().block();
        String guildId = event.getGuildId().get().asString();
        String prefix = GuildSettings.getGuildSettings(guildId).getPrefix();
        int totalCommands = event.getAdminBot().getCommandRegistry().size();
        String username = event.getClient().getSelf().map(User::getUsername).block();
        String avatarUrl = event.getClient().getSelf().map(User::getAvatarUrl).block();
        messageChannel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("Information")
                .setAuthor(username, null, avatarUrl)
                .setDescription("AdminBot is a simple bot used for administration (and other things).")
                .addField("Current Version", event.getAdminBot().getVersion(), true)
                .addField("Current Prefix", "`" + prefix + "`", true)
                .addField("Total Amount of Commands", String.valueOf(totalCommands), true)
                .setFooter("Made by xf8b#9420", "https://cdn.discordapp.com/avatars/332600665412993045/d1de6c46d40fcb4c6200f86cb5a073af.png")
                .setColor(Color.BLUE)).subscribe();
    }
}
