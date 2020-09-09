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
import discord4j.rest.util.Color;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.data.MemberData;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ExtensionsKt;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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
                .setFlags(MEMBER, REASON)
                .setAdministratorLevelRequired(1));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Snowflake userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        Snowflake memberWhoWarnedId = event.getMember().orElseThrow().getId();
        String reason = event.getValueOfFlag(REASON).orElse("No warn reason was provided.");
        if (reason.equals("all")) {
            channel.createMessage("Sorry, but this warn reason is reserved.").block();
            return;
        }
        guild.getMemberById(userId)
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                .flatMap(member -> {
                    if (!PermissionUtil.isMemberHigher(guild, event.getMember().get(), member)) {
                        channel.createMessage("Cannot warn member because the member is equal to or higher than you!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                })
                .flatMap(member -> {
                    if (MemberData.getMemberData(guild, userId).hasWarn(reason)) {
                        List<String> warnIds = new ArrayList<>();
                        MemberData.getMemberData(guild, userId).getWarns().forEach(warnContext -> {
                            if (warnContext.getReason().equals(reason)) {
                                warnIds.add(String.valueOf(warnContext.getWarnId()));
                            }
                        });
                        Collections.reverse(warnIds);
                        String top = warnIds.get(0);
                        int warnId = Integer.parseInt(top) + 1;
                        MemberData.getMemberData(guild, userId).addWarn(memberWhoWarnedId, warnId, reason);
                    } else {
                        MemberData.getMemberData(guild, userId).addWarn(memberWhoWarnedId, 0, reason);
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
                                            .setFooter("Warned by: " + ExtensionsKt.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                            .addField("Server", guild.getName(), false)
                                            .addField("Reason", reason, false)
                                            .setTimestamp(Instant.now())
                                            .setColor(Color.RED)));
                    return channel.createMessage("Successfully warned " + member.getDisplayName() + ".").then(privateChannelMono);
                })
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(50007), throwable -> Mono.empty()) //cannot send to user
                .subscribe();
    }
}
