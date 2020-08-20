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
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.MemberUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

//todo: fix members with multiple words in their name not working
public class BanCommandHandler extends AbstractCommandHandler {
    public BanCommandHandler() {
        super(
                "${prefix}ban",
                "${prefix}ban <member> [reason]",
                "Bans the specified member with the specified reason, or `No ban reason was provided` if there was none.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.ADMINISTRATION,
                1,
                PermissionSet.of(Permission.BAN_MEMBERS),
                3
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String content = event.getMessage().getContent();
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String userId = String.valueOf(ParsingUtil.parseUserId(guild, content.trim().split(" ")[1].trim()));
        if (userId.equals("null")) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        String reason;
        if (content.trim().split(" ").length < 3) {
            reason = "No ban reason was provided.";
        } else {
            reason = content.trim().substring(StringUtils.ordinalIndexOf(content.trim(), " ", 2) + 1).trim();
        }
        String finalReason = reason;
        guild.getBan(Snowflake.of(userId))
                .flatMap(ban -> channel.createMessage("The user is already banned!"))
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10026), throwable -> Mono.fromRunnable(() -> guild.getMemberById(Snowflake.of(userId)) //unknown ban
                        .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                        .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                        .flatMap(member -> {
                            if (member == event.getMember().get()) {
                                channel.createMessage("You cannot ban yourself!").block();
                                return Mono.empty();
                            } else {
                                return Mono.just(member);
                            }
                        })
                        .flatMap(member -> event.getClient().getSelf().flatMap(selfMember -> {
                            if (selfMember == member) {
                                channel.createMessage("You cannot ban AdminBot!").block();
                                return Mono.empty();
                            } else {
                                return Mono.just(member);
                            }
                        }))
                        .flatMap(member -> guild.getSelfMember().flatMap(selfMember -> selfMember.isHigher(member).flatMap(isHigher -> {
                            if (isHigher) {
                                return Mono.just(member);
                            } else {
                                channel.createMessage("Cannot ban member because the member is higher!").block();
                                return Mono.empty();
                            }
                        })))
                        .flatMap(member -> {
                            if (PermissionUtil.getAdministratorLevel(guild, member) <= PermissionUtil.getAdministratorLevel(guild, event.getMember().get())) {
                                return Mono.just(member);
                            } else {
                                channel.createMessage("Cannot ban member because the member is higher!").block();
                                return Mono.empty();
                            }
                        })
                        .flatMap(member -> {
                            String username = member.getDisplayName();
                            Mono<?> mono = member.ban(banQuerySpec -> banQuerySpec.setDeleteMessageDays(0).setReason(finalReason))
                                    .onErrorResume(throwable1 -> Mono.fromRunnable(() -> channel.createMessage("Failed to ban " + username + ".").block()))
                                    .flatMap(success -> channel.createMessage("Successfully banned " + username + "!"));
                            return member.getPrivateChannel().flatMap(privateChannel -> {
                                if (member.isBot()) return Mono.empty();
                                return privateChannel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were banned!")
                                        .setFooter("Banned by: " + MemberUtil.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                        .addField("Server", guild.getName(), false)
                                        .addField("Reason", finalReason, false)
                                        .setTimestamp(Instant.now())
                                        .setColor(Color.RED));
                            }).and(mono);
                        }))).subscribe();
    }
}
