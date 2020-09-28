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

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ExtensionsKt;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

public class KickCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setRequired(false)
            .build();

    public KickCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}kick")
                .setDescription("Kicks the specified member with the reason provided, or `No kick reason was provided` if there was none.")
                .setCommandType(CommandType.OTHER)
                .setMinimumAmountOfArgs(1)
                .setFlags(MEMBER, REASON)
                .setBotRequiredPermissions(Permission.KICK_MEMBERS)
                .setAdministratorLevelRequired(2));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        AdminBot adminBot = event.getAdminBot();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        String reason = event.getValueOfFlag(REASON).orElse("No kick reason was provided.");
        return guild.getMemberById(userId.get())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
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
                    if (!PermissionUtil.isMemberHigher(adminBot, guild, event.getMember().get(), member)) {
                        channel.createMessage("Cannot kick member because the member is equal to or higher than you!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                })
                .flatMap(member -> {
                    String username = member.getDisplayName();
                    return member.getPrivateChannel().flatMap(privateChannel -> {
                        if (member.isBot()) {
                            return Mono.empty();
                        } else {
                            return privateChannel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were kicked!")
                                    .setFooter("Kicked by: " + ExtensionsKt.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                    .addField("Server", guild.getName(), false)
                                    .addField("Reason", reason, false)
                                    .setTimestamp(Instant.now())
                                    .setColor(Color.RED));
                        }
                    }).onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(50007), throwable -> Mono.empty()) //cannot send messages to user
                            .then(member.kick(reason)
                                    .doOnSuccess(success -> channel.createMessage("Successfully kicked " + username + "!").block()));
                }).then();
    }
}
