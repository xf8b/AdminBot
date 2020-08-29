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
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ExtensionsKt;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

public class KickCommandHandler extends AbstractCommandHandler {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setRequired(false)
            .build();

    public KickCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}kick")
                .setDescription("Kicks the specified member with the reason provided, or `No kick reason was provided` if there was none.")
                .setCommandType(CommandType.OTHER)
                .setMinimumAmountOfArgs(1)
                .setFlags(ImmutableList.of(MEMBER, REASON))
                .setBotRequiredPermissions(PermissionSet.of(Permission.KICK_MEMBERS))
                .setAdministratorLevelRequired(2));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Snowflake userId = ParsingUtil.parseUserIdAndReturnSnowflake(guild, event.getValueOfFlag(MEMBER));
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        String reason = event.getValueOfFlag(REASON);
        if (reason == null) reason = "No kick reason was provided.";
        String finalReason = reason;
        String finalReason1 = reason;
        guild.getMemberById(userId)
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
                .flatMap(member -> {
                    if (member.equals(event.getMember().get())) {
                        channel.createMessage("You cannot kick yourself!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                })
                .flatMap(member -> event.getClient().getSelf().flatMap(selfMember -> {
                    if (selfMember.equals(member)) {
                        channel.createMessage("You cannot kick AdminBot!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                }))
                .flatMap(member -> guild.getSelfMember().flatMap(selfMember -> member.isHigher(selfMember).flatMap(isHigher -> {
                    if (!isHigher) {
                        return Mono.just(member);
                    } else {
                        channel.createMessage("Cannot kick member because the member is higher than me!").block();
                        return Mono.empty();
                    }
                })))
                .flatMap(member -> {
                    if (PermissionUtil.getAdministratorLevel(guild, member) <= PermissionUtil.getAdministratorLevel(guild, event.getMember().get())) {
                        return Mono.just(member);
                    } else {
                        channel.createMessage("Cannot kick member because the member is higher than you!").block();
                        return Mono.empty();
                    }
                })
                .flatMap(member -> {
                    String username = member.getDisplayName();
                    Mono<?> mono = member.kick(finalReason1)
                            .onErrorResume(throwable1 -> Mono.fromRunnable(() -> channel.createMessage("Failed to kick " + username + ".").block()))
                            .doOnSuccess(success -> channel.createMessage("Successfully kicked " + username + "!").block());
                    return member.getPrivateChannel().flatMap(privateChannel -> {
                        if (member.isBot()) return Mono.empty();
                        return privateChannel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were kicked!")
                                .setFooter("Kicked by: " + ExtensionsKt.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                .addField("Server", guild.getName(), false)
                                .addField("Reason", finalReason, false)
                                .setTimestamp(Instant.now())
                                .setColor(Color.RED));
                    }).and(mono);
                }).subscribe();
    }
}
