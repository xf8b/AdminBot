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
import io.github.xf8b.adminbot.util.ParsingUtil;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class NicknameCommandHandler extends AbstractCommandHandler {
    public NicknameCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}nickname")
                .setUsage("${prefix}nickname <member> [nickname]")
                .setDescription("Sets the nickname of the specified member, or resets it if none was provided.")
                .setCommandType(CommandType.ADMINISTRATION)
                .addAlias("${prefix}nick")
                .setMinimumAmountOfArgs(1)
                .setBotRequiredPermissions(PermissionSet.of(Permission.MANAGE_NICKNAMES))
                .setAdministratorLevelRequired(1));
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
        boolean resetNickname = false;
        String nickname = "";
        if (content.trim().split(" ").length < 3) {
            resetNickname = true;
        } else {
            nickname = content.trim().substring(StringUtils.ordinalIndexOf(content.trim(), " ", 2) + 1).trim();
        }
        boolean finalResetNickname = resetNickname;
        String finalNickname = nickname;
        guild.getMemberById(Snowflake.of(userId))
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .map(member -> Objects.requireNonNull(member, "Member must not be null!"))
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
                        if (finalResetNickname) {
                            return guild.changeSelfNickname(null)
                                    .doOnSuccess(unused -> channel.createMessage("Successfully reset nickname of " + member.getDisplayName() + "!").block());
                        } else {
                            return guild.changeSelfNickname(finalNickname)
                                    .doOnSuccess(unused -> channel.createMessage("Successfully set nickname of " + member.getDisplayName() + "!").block());
                        }
                    } else {
                        if (finalResetNickname) {
                            return member.edit(guildMemberEditSpec -> guildMemberEditSpec.setNickname(null))
                                    .doOnSuccess(unused -> channel.createMessage("Successfully reset nickname of " + member.getDisplayName() + "!").block());
                        } else {
                            return member.edit(guildMemberEditSpec -> guildMemberEditSpec.setNickname(finalNickname))
                                    .doOnSuccess(unused -> channel.createMessage("Successfully set nickname of " + member.getDisplayName() + "!").block());
                        }
                    }
                })
                .subscribe();
    }
}
