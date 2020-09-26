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

package io.github.xf8b.adminbot.commands;

import com.google.common.collect.Range;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

public class UnbanCommand extends AbstractCommand {
    private static final StringArgument MEMBER = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("member")
            .build();

    public UnbanCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}unban")
                .setDescription("Unbans the specified member.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .addArgument(MEMBER)
                .setBotRequiredPermissions(Permission.BAN_MEMBERS)
                .setAdministratorLevelRequired(3));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String memberIdOrUsername = event.getValueOfArgument(MEMBER).get();
        return guild.getBans().filter(ban -> {
            boolean usernameMatches = ban.getUser().getUsername().equals(memberIdOrUsername);
            if (!usernameMatches) {
                try {
                    return ban.getUser().getId().asLong() == Long.parseLong(memberIdOrUsername);
                } catch (NumberFormatException exception) {
                    return false;
                }
            } else {
                return true;
            }
        }).take(1).flatMap(ban -> guild.unban(ban.getUser().getId())
                .then(channel.createMessage("Successfully unbanned " + ban.getUser().getUsername() + "!")))
                .switchIfEmpty(channel.createMessage("The member does not exist or is not banned!"))
                .then();
    }
}
