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
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.helpers.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Objects;

@Slf4j
public class RemoveWarnCommandHandler extends AbstractCommandHandler {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .build();
    private static final StringFlag WARN_ID = StringFlag.builder()
            .setShortName("w")
            .setLongName("warnreason")
            .setRequired(false)
            .build();

    public RemoveWarnCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}removewarn")
                .setDescription("Removes the specified member's warns with the warnId and reason provided. " +
                        "\nIf the reason is all, all warns will be removed. The warnId is not needed." +
                        "\nIf the warnId is all, all warns with the same reason will be removed. ")
                .setCommandType(CommandType.ADMINISTRATION)
                .setAliases(ImmutableList.of("${prefix}removewarns", "${prefix}rmwarn", "${prefix}rmwarns"))
                .setMinimumAmountOfArgs(2)
                .setFlags(ImmutableList.of(MEMBER, REASON, WARN_ID))
                .setAdministratorLevelRequired(1));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        try {
            MessageChannel channel = event.getChannel().block();
            Guild guild = event.getGuild().block();
            String guildId = guild.getId().asString();
            Snowflake userId = ParsingUtil.parseUserIdAndReturnSnowflake(guild, event.getValueOfFlag(MEMBER));
            if (userId == null) {
                channel.createMessage("The member does not exist!").block();
                return;
            }
            String reason = event.getValueOfFlag(REASON);
            String warnId = event.getValueOfFlag(WARN_ID);
            boolean checkIfWarnExists = !reason.equals("all");
            boolean removeAllWarnsWithSameName = warnId == null;
            guild.getMemberById(userId)
                    .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                    .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())); //unknown member
            if (!WarnsDatabaseHelper.hasWarn(guildId, userId.asString(), reason) && checkIfWarnExists) {
                channel.createMessage("The user does not have a warn with that reason!").block();
            } else {
                WarnsDatabaseHelper.remove(
                        guildId,
                        userId.asString(),
                        removeAllWarnsWithSameName ? null : warnId,
                        checkIfWarnExists ? reason : null
                );
                guild.getMemberById(userId)
                        .flatMap(member -> channel.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + "."))
                        .subscribe();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            LOGGER.error("An exception happened while trying to/from read/write to the prefix database!", exception);
        }
    }
}
