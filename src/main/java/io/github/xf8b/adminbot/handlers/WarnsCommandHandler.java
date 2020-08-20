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
import com.google.common.collect.Multimap;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.helpers.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
//TODO: show the person which warned you
public class WarnsCommandHandler extends AbstractCommandHandler {
    public WarnsCommandHandler() {
        super(
                "${prefix}warns",
                "${prefix}warns <member>",
                "Gets the warns for the specified member.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.ADMINISTRATION,
                1,
                PermissionSet.of(Permission.EMBED_LINKS),
                1
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        try {
            String content = event.getMessage().getContent();
            MessageChannel channel = event.getChannel().block();
            Guild guild = event.getGuild().block();
            String guildId = guild.getId().asString();
            String userId = String.valueOf(ParsingUtil.parseUserId(guild, content.trim().split(" ")[1].trim()));
            if (userId.equals("null")) {
                channel.createMessage("The member does not exist!").block();
                return;
            }
            AtomicReference<String> username = new AtomicReference<>("");
            guild.getMemberById(Snowflake.of(userId))
                    .map(Objects::requireNonNull)
                    .map(member -> {
                        username.set(member.getDisplayName());
                        return member;
                    })
                    .subscribe();
            Multimap<String, String> warns = WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId);
            if (warns.isEmpty()) {
                channel.createMessage("The user has no warnings.").block();
            } else {
                StringBuilder warnReasons = new StringBuilder();
                StringBuilder warnIds = new StringBuilder();
                warns.forEach((warnReason, warnId) -> {
                    warnReasons.append(warnReason).append("\n");
                    warnIds.append(warnId).append("\n");
                });
                channel.createEmbed(embedCreateSpec -> embedCreateSpec
                        .setTitle("Warnings For `" + username.get() + "`")
                        .addField("Warn ID", warnIds.toString().replaceAll("\n$", ""), true)
                        .addField("Reason", warnReasons.toString().replaceAll("\n$", ""), true)
                        .setColor(Color.BLUE))
                        .block();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            LOGGER.error("An error happened while trying to read from the warns database!", exception);
        }
    }
}
