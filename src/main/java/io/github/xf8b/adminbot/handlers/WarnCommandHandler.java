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
import discord4j.rest.util.Color;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.helpers.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.MemberUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class WarnCommandHandler extends AbstractCommandHandler {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setValidityPredicate(value -> !value.equals("all"))
            .setInvalidValueErrorMessageFunction(invalidValue -> {
                if (invalidValue.equals("all")) {
                    return "Sorry, but this warn reason is reserved.";
                } else {
                    return Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE;
                }
            })
            .setRequired(false)
            .build();

    public WarnCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}warn")
                .setDescription("Warns the specified member with the specified reason, or `No warn reason was provided` if there was none.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .setFlags(ImmutableList.of(MEMBER, REASON))
                .setAdministratorLevelRequired(1));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String guildId = guild.getId().asString();
        Snowflake userId = ParsingUtil.parseUserIdAndReturnSnowflake(guild, event.getValueOfFlag(MEMBER));
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        String reason = event.getValueOfFlag(REASON);
        if (reason == null) reason = "No warn reason was provided.";
        if (reason.equals("all")) {
            channel.createMessage("Sorry, but this warn reason is reserved.").block();
            return;
        }
        String finalReason = reason;
        guild.getMemberById(userId)
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                .flatMap(member -> {
                    if (PermissionUtil.getAdministratorLevel(guild, member) <= PermissionUtil.getAdministratorLevel(guild, event.getMember().get())) {
                        return Mono.just(member);
                    } else {
                        channel.createMessage("Cannot warn member because the member is higher than you!").block();
                        return Mono.empty();
                    }
                })
                .flatMap(member -> {
                    try {
                        if (WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId.asString(), finalReason)) {
                            List<String> warnIds = new ArrayList<>();
                            WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId.asString()).forEach((reasonInDatabase, warnId) -> {
                                if (reasonInDatabase.equals(finalReason)) {
                                    warnIds.add(warnId);
                                }
                            });
                            Collections.reverse(warnIds);
                            String top = warnIds.get(0);
                            String warnId = String.valueOf(Integer.parseInt(top) + 1);
                            WarnsDatabaseHelper.insertIntoWarns(guildId, userId.asString(), warnId, finalReason);
                        } else {
                            WarnsDatabaseHelper.insertIntoWarns(guildId, userId.asString(), String.valueOf(0), finalReason);
                        }
                    } catch (ClassNotFoundException | SQLException exception) {
                        LOGGER.error("An error happened while trying to read/write to/from the warns database!", exception);
                    }
                    Mono<?> privateChannelMono = member.getPrivateChannel()
                            .flatMap(privateChannel -> {
                                if (member.isBot()) {
                                    return Mono.empty();
                                } else if (member.equals(event.getClient().getSelf().block())) {
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
