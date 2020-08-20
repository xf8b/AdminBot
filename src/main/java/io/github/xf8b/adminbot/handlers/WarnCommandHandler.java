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
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.helpers.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.MemberUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class WarnCommandHandler extends AbstractCommandHandler {
    public WarnCommandHandler() {
        super(
                "${prefix}warn",
                "${prefix}warn <member> [reason]",
                "Warns the specified member with the specified reason, or `No warn reason was provided` if there was none.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.ADMINISTRATION,
                1,
                PermissionSet.none(),
                1
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String content = event.getMessage().getContent();
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String guildId = guild.getId().asString();
        String userId = String.valueOf(ParsingUtil.parseUserId(guild, content.trim().split(" ")[1].trim()));
        if (userId.equals("null")) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        String reason;
        if (content.trim().split(" ").length < 3) {
            reason = "No warn reason was provided.";
        } else {
            reason = content.trim().substring(StringUtils.ordinalIndexOf(content.trim(), " ", 2) + 1).trim();
        }
        if (reason.equals("all")) {
            channel.createMessage("Sorry, but this warn reason is reserved.").block();
            return;
        }
        String finalReason = reason;
        guild.getMemberById(Snowflake.of(userId))
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                .flatMap(member -> {
                    if (PermissionUtil.getAdministratorLevel(guild, member) <= PermissionUtil.getAdministratorLevel(guild, event.getMember().get())) {
                        return Mono.just(member);
                    } else {
                        channel.createMessage("Cannot warn member because the member is higher!").block();
                        return Mono.empty();
                    }
                })
                .flatMap(member -> {
                    try {
                        if (WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, reason)) {
                            List<String> warnIds = new ArrayList<>();
                            WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId).forEach((reasonInDatabase, warnId) -> {
                                if (reasonInDatabase.equals(reason)) {
                                    warnIds.add(warnId);
                                }
                            });
                            Collections.reverse(warnIds);
                            String top = warnIds.get(0);
                            String warnId = String.valueOf(Integer.parseInt(top) + 1);
                            WarnsDatabaseHelper.insertIntoWarns(guildId, userId, warnId, reason);
                        } else {
                            WarnsDatabaseHelper.insertIntoWarns(guildId, userId, String.valueOf(0), reason);
                        }
                    } catch (ClassNotFoundException | SQLException exception) {
                        LOGGER.error("An error happened while trying to read/write to/from the warns database!", exception);
                    }
                    Mono<?> privateChannelMono = member.getPrivateChannel()
                            .flatMap(privateChannel -> {
                                if (member.isBot()) {
                                    return Mono.empty();
                                } else if (member == event.getClient().getSelf().block()) {
                                    return Mono.empty();
                                } else {
                                    return Mono.just(privateChannel);
                                }
                            })
                            .flatMap(privateChannel -> privateChannel
                                    .createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were warned!")
                                            .setFooter("Warned by: " + MemberUtil.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                            .addField("Server", guild.getName(), false)
                                            .addField("Reason", finalReason, false)
                                            .setTimestamp(Instant.now())
                                            .setColor(Color.RED))
                                    .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(50007), throwable -> Mono.empty())); //cannot send to user
                    return channel.createMessage("Successfully warned " + member.getDisplayName() + ".").and(privateChannelMono);
                }).subscribe();
    }
}
