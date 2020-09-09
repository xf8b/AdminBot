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

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.IntegerFlag;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.data.MemberData;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.ThisShouldNotHaveBeenThrownException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveWarnCommandHandler extends AbstractCommandHandler {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag MEMBER_WHO_WARNED = StringFlag.builder()
            .setShortName("mww")
            .setLongName("memberwhowarned")
            .setRequired(false)
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .build();
    private static final IntegerFlag WARN_ID = IntegerFlag.builder()
            .setShortName("w")
            .setLongName("warnid")
            .setRequired(false)
            .build();

    public RemoveWarnCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}removewarn")
                .setDescription("Removes the specified member's warns with the warnId and reason provided. " +
                        "\nIf the reason is all, all warns will be removed. The warnId is not needed." +
                        "\nIf the warnId is all, all warns with the same reason will be removed. ")
                .setCommandType(CommandType.ADMINISTRATION)
                .setAliases("${prefix}removewarns", "${prefix}rmwarn", "${prefix}rmwarns")
                .setMinimumAmountOfArgs(2)
                .setFlags(MEMBER, MEMBER_WHO_WARNED, REASON, WARN_ID)
                .setAdministratorLevelRequired(1));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Snowflake userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER)
                .orElseThrow(ThisShouldNotHaveBeenThrownException::new));
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        Snowflake memberWhoWarnedId;
        if (event.getValueOfFlag(MEMBER_WHO_WARNED).isPresent()) {
            Snowflake tempMemberWhoWarnedId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER_WHO_WARNED).get());
            if (tempMemberWhoWarnedId == null) {
                channel.createMessage("The member who warned does not exist!").block();
                return;
            } else {
                memberWhoWarnedId = tempMemberWhoWarnedId;
            }
        } else {
            memberWhoWarnedId = null;
        }
        String reason = event.getValueOfFlag(REASON)
                .orElseThrow(ThisShouldNotHaveBeenThrownException::new);
        int warnId = event.getValueOfFlag(WARN_ID).orElse(-1);
        if (!MemberData.getMemberData(guild, userId).hasWarn(reason) && !reason.equals("all")) {
            channel.createMessage("The user does not have a warn with that reason!").block();
        } else {
            MemberData memberData = MemberData.getMemberData(guild, userId);
            memberData.removeWarn(memberWhoWarnedId, warnId, reason);
            guild.getMemberById(userId)
                    .flatMap(member -> channel.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + "."))
                    .subscribe();
        }
    }
}
