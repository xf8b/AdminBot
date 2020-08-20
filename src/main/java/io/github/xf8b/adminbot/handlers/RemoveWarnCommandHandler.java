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
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.helpers.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Objects;

@Slf4j
public class RemoveWarnCommandHandler extends AbstractCommandHandler {
    public RemoveWarnCommandHandler() {
        super(
                "${prefix}removewarn",
                "${prefix}removewarn <member> <reason> [warnId]",
                "Removes the specified member's warns with the warnId and reason provided. " +
                        "\nIf the reason is all, all warns will be removed. The warnId is not needed." +
                        "\nIf the warnId is all, all warns with the same reason will be removed. ",
                ImmutableMap.of(),
                ImmutableList.of("${prefix}removewarns"),
                CommandType.ADMINISTRATION,
                2,
                PermissionSet.none(),
                1
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        //TODO: fix warnid not being the correct word
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
            String reasonAndWarnId = content.trim().substring(content.trim().indexOf(" ", content.trim().indexOf(" ") + 1) + 1);
            String reason;
            String warnId;
            if (reasonAndWarnId.lastIndexOf(" ") == -1) {
                reason = reasonAndWarnId.trim();
                warnId = "all";
            } else {
                reason = reasonAndWarnId.substring(0, reasonAndWarnId.lastIndexOf(" ")).trim();
                warnId = reasonAndWarnId.trim().substring(reasonAndWarnId.lastIndexOf(" ")).trim();
            }
            boolean checkIfWarnExists = true;
            boolean removeAllWarnsWithSameName = false;
            if (reasonAndWarnId.lastIndexOf(" ") == 0) {
                reason = warnId;
                warnId = "all";
            }
            if (reason.equals("all")) {
                checkIfWarnExists = false;
                removeAllWarnsWithSameName = true;
            }
            if (warnId.trim().equals("")) {
                warnId = "all";
            }
            if (warnId.trim().equals("all")) {
                removeAllWarnsWithSameName = true;
            }
            guild.getMemberById(Snowflake.of(userId))
                    .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                    .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())); //unknown member
            if (!WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, reason) && checkIfWarnExists) {
                channel.createMessage("The user does not have a warn with that reason!").block();
            } else {
                WarnsDatabaseHelper.removeWarnsFromUserForGuild(guildId, userId, removeAllWarnsWithSameName ? null : warnId, checkIfWarnExists ? reason : null);
                guild.getMemberById(Snowflake.of(userId))
                        .flatMap(member -> channel.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + "."))
                        .subscribe();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            LOGGER.error("An exception happened while trying to/from read/write to the prefix database!", exception);
        }
    }
}
