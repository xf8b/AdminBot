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
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

//TODO: fix this
public class MuteCommandHandler extends AbstractCommandHandler {
    public MuteCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}mute")
                .setUsage("${prefix}mute <member> <time>")
                .setDescription("Mutes the specified member for the specified amount of time.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(2)
                .setBotRequiredPermissions(PermissionSet.of(Permission.MANAGE_ROLES))
                .setAdministratorLevelRequired(1));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String content = event.getMessage().getContent();
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String guildId = guild.getId().asString();
        String userId = content.trim().split(" ")[1].trim().replaceAll("[<@!>]", "");
        String time = content.trim().split(" ")[2].trim().replaceAll("[a-zA-Z]", "").trim();
        String tempTimeType = content.trim().split(" ")[2].trim().replaceAll("\\d", "").trim();
        TimeUnit timeType;
        switch (tempTimeType.toLowerCase()) {
            case "d":
            case "day":
            case "days":
                timeType = TimeUnit.DAYS;
                break;
            case "h":
            case "hr":
            case "hrs":
            case "hours":
                timeType = TimeUnit.HOURS;
                break;
            case "m":
            case "mins":
            case "minutes":
                timeType = TimeUnit.MINUTES;
                break;
            case "s":
            case "sec":
            case "secs":
            case "second":
            case "seconds":
                timeType = TimeUnit.SECONDS;
                break;
            default:
                channel.createMessage("The time specified is invalid!").block();
                return;
        }
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        if (userId.isEmpty()) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        guild.getMemberById(Snowflake.of(userId))
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
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
                        channel.createMessage("You cannot mute AdminBot!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                }));
    }
}
