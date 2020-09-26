/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag;
import io.github.xf8b.xf8bot.api.commands.flags.TimeFlag;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Optional;

//TODO: fix this
public class MuteCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();

    private static final TimeFlag TIME = TimeFlag.builder()
            .setShortName("t")
            .setLongName("time")
            .build();

    public MuteCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}mute")
                .setDescription("Mutes the specified member for the specified amount of time.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(2)
                .setFlags(MEMBER, TIME)
                .setBotRequiredPermissions(Permission.MANAGE_ROLES)
                .setAdministratorLevelRequired(1));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
        //Long time = event.getValueOfFlag(TIME).get().getFirst();
        //TimeUnit timeType = event.getValueOfFlag(TIME).get().getSecond();
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        return channel.createMessage("This command is not complete yet!").then();
        /*
        return guild.getMemberById(userId.get())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .flatMap(member -> {
                    if (member == event.getMember().get()) {
                        channel.createMessage("You cannot mute yourself!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                })
                .flatMap(member -> event.getClient().getSelf().flatMap(selfMember -> {
                    if (selfMember == member) {
                        channel.createMessage("You cannot mute xf8bot!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                })).then();
                */
    }
}
