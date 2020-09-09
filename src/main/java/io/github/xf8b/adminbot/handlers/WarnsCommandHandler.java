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
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import io.github.xf8b.adminbot.data.MemberData;
import io.github.xf8b.adminbot.data.WarnContext;
import io.github.xf8b.adminbot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class WarnsCommandHandler extends AbstractCommandHandler {
    private static final StringArgument MEMBER = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("member")
            .build();

    public WarnsCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}warns")
                .setDescription("Gets the warns for the specified member.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .addArgument(MEMBER)
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS))
                .setAdministratorLevelRequired(1));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Snowflake userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfArgument(MEMBER).get());
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        AtomicReference<String> username = new AtomicReference<>("");
        guild.getMemberById(userId)
                .map(Objects::requireNonNull)
                .map(Member::getDisplayName)
                .subscribe(username::set);
        List<WarnContext> warns = MemberData.getMemberData(guild, userId).getWarns();
        if (warns.isEmpty()) {
            channel.createMessage("The user has no warnings.").block();
        } else {
            StringBuilder membersWhoWarned = new StringBuilder();
            StringBuilder warnReasons = new StringBuilder();
            StringBuilder warnIds = new StringBuilder();
            warns.forEach(warnContext -> {
                warnReasons.append(warnContext.getReason()).append("\n");
                warnIds.append(warnContext.getWarnId()).append("\n");
                membersWhoWarned.append(guild.getMemberById(warnContext.getMemberWhoWarnedId())
                        .map(Member::getNicknameMention)
                        .block())
                        .append("\n");
            });
            channel.createEmbed(embedCreateSpec -> embedCreateSpec
                    .setTitle("Warnings For `" + username.get() + "`")
                    .addField("Member Who Warned", membersWhoWarned.toString().replaceAll("\n$", ""), true)
                    .addField("Warn ID", warnIds.toString().replaceAll("\n$", ""), true)
                    .addField("Reason", warnReasons.toString().replaceAll("\n$", ""), true)
                    .setColor(Color.BLUE))
                    .block();
        }
    }
}
