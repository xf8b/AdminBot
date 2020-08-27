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
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import reactor.core.publisher.Mono;

public class UnbanCommandHandler extends AbstractCommandHandler {
    private static final StringArgument MEMBER = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("member")
            .build();

    public UnbanCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}unban")
                .setDescription("Unbans the specified member.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .addArgument(MEMBER)
                .setBotRequiredPermissions(PermissionSet.of(Permission.BAN_MEMBERS))
                .setAdministratorLevelRequired(3));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Snowflake userId = ParsingUtil.parseUserIdAndReturnSnowflake(guild, event.getValueOfArgument(MEMBER));
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        guild.getBan(userId)
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10026), throwable -> Mono.fromRunnable(() -> channel.createMessage("The member is not banned!").block())) //unknown ban
                .flatMap(ban -> {
                    String username = ban.getUser().getUsername();
                    guild.unban(userId).block();
                    return channel.createMessage("Successfully unbanned " + username + "!");
                })
                .subscribe();
    }
}
