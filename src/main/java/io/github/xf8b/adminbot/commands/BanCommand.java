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

public class BanCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setRequired(false)
            .build();

    public BanCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}ban")
                .setDescription("Bans the specified member with the specified reason, or `No ban reason was provided` if there was none.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .setFlags(MEMBER, REASON)
                .setBotRequiredPermissions(Permission.BAN_MEMBERS)
                .setAdministratorLevelRequired(3));
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
        String reason = event.getValueOfFlag(REASON).orElse("No ban reason was provided.");
        return guild.getBans().filter(ban -> ban.getUser().getId().equals(userId))
                .flatMap(ban -> channel.createMessage("The user is already banned!"))
                .switchIfEmpty(guild.getMemberById(userId.get())
                        .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").subscribe())) //unknown member
                        .flatMap(member -> {
                            if (member.equals(event.getMember().get())) {
                                channel.createMessage("You cannot ban yourself!").block();
                                return Mono.empty();
                            } else {
                                return Mono.just(member);
                            }
                        })
                        .flatMap(member -> event.getClient().getSelf().flatMap(selfMember -> {
                            if (selfMember.equals(member)) {
                                channel.createMessage("You cannot ban AdminBot!").block();
                                return Mono.empty();
                            } else {
                                return Mono.just(member);
                            }
                        }))
                        .flatMap(member -> guild.getSelfMember().flatMap(selfMember -> member.isHigher(selfMember).flatMap(isHigher -> {
                            if (!isHigher) {
                                return Mono.just(member);
                            } else {
                                channel.createMessage("Cannot ban member because the member is higher than me!").block();
                                return Mono.empty();
                            }
                        })))
                        .flatMap(member -> {
                            if (!PermissionUtil.isMemberHigher(adminBot, guild, event.getMember().get(), member)) {
                                channel.createMessage("Cannot ban member because the member is equal to or higher than you!").block();
                                return Mono.empty();
                            } else {
                                return Mono.just(member);
                            }
                        })
                        .flatMap(member -> {
                            String username = member.getDisplayName();
                            return member.getPrivateChannel().flatMap(privateChannel -> {
                                if (member.isBot()) return Mono.empty();
                                return privateChannel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were banned!")
                                        .setFooter("Banned by: " + ExtensionsKt.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                        .addField("Server", guild.getName(), false)
                                        .addField("Reason", reason, false)
                                        .setTimestamp(Instant.now())
                                        .setColor(Color.RED));
                            }).onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(50007), throwable -> Mono.empty()) //cannot send messages to user
                                    .then(member.ban(banQuerySpec -> banQuerySpec.setDeleteMessageDays(0)
                                            .setReason(reason)))
                                    .then(channel.createMessage("Successfully banned " + username + "!"));
                        })).then();
    }
}
