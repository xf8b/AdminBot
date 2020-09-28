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
import discord4j.rest.util.Permission;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class NicknameCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag NICKNAME = StringFlag.builder()
            .setShortName("n")
            .setLongName("nickname")
            .setRequired(false)
            .build();

    public NicknameCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}nickname")
                .setDescription("Sets the nickname of the specified member, or resets it if none was provided.")
                .setCommandType(CommandType.ADMINISTRATION)
                .addAlias("${prefix}nick")
                .setMinimumAmountOfArgs(1)
                .setFlags(MEMBER, NICKNAME)
                .setBotRequiredPermissions(Permission.MANAGE_NICKNAMES)
                .setAdministratorLevelRequired(1));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        Optional<String> nickname = event.getValueOfFlag(NICKNAME);
        boolean resetNickname = nickname.isEmpty();
        return guild.getMemberById(userId.get())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .flatMap(member -> guild.getSelfMember().flatMap(selfMember -> member.isHigher(selfMember).flatMap(isHigher -> {
                    if (!isHigher) {
                        return Mono.just(member);
                    } else {
                        channel.createMessage("Cannot set/reset nickname of member because member is higher than me!").block();
                        return Mono.empty();
                    }
                })))
                .flatMap(member -> {
                    if (member.getId().equals(guild.getClient().getSelfId())) {
                        if (resetNickname) {
                            return guild.changeSelfNickname("")
                                    .doOnSuccess(unused -> channel.createMessage("Successfully reset nickname of " + member.getDisplayName() + "!").block());
                        } else {
                            return guild.changeSelfNickname(nickname.get())
                                    .doOnSuccess(unused -> channel.createMessage("Successfully set nickname of " + member.getDisplayName() + "!").block());
                        }
                    } else {
                        if (resetNickname) {
                            return member.edit(guildMemberEditSpec -> guildMemberEditSpec.setNickname(""))
                                    .doOnSuccess(unused -> channel.createMessage("Successfully reset nickname of " + member.getDisplayName() + "!").block());
                        } else {
                            return member.edit(guildMemberEditSpec -> guildMemberEditSpec.setNickname(nickname.get()))
                                    .doOnSuccess(unused -> channel.createMessage("Successfully set nickname of " + member.getDisplayName() + "!").block());
                        }
                    }
                }).then();
    }
}
