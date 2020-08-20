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

package io.github.xf8b.adminbot.util;

import discord4j.core.object.entity.Guild;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

@UtilityClass
@Slf4j
public class ParsingUtil {
    @Nullable
    public Long parseUserId(Guild guild, String stringToParse) {
        try {
            return Long.parseLong(stringToParse);
        } catch (NumberFormatException exception) {
            try {
                return Long.parseLong(stringToParse.replaceAll("[<@!>]", ""));
            } catch (NumberFormatException exception1) {
                Flux<Long> memberUsernameMatchesMono = guild.getMembers().flatMap(member -> {
                    if (member.getUsername().trim().equalsIgnoreCase(stringToParse)) {
                        return Mono.just(member.getId().asLong());
                    } else {
                        return Mono.empty();
                    }
                });
                Flux<Long> memberNicknameMatchesMono = guild.getMembers().flatMap(member -> {
                    if (member.getNickname().isPresent()) {
                        if (member.getNickname().get().trim().equalsIgnoreCase(stringToParse)) {
                            return Mono.just(member.getId().asLong());
                        }
                    }
                    return Mono.empty();
                });
                if (memberUsernameMatchesMono.blockLast() == null) {
                    return memberNicknameMatchesMono.blockLast();
                } else {
                    return memberUsernameMatchesMono.blockLast();
                }
            }
        }
    }

    @Nullable
    public Long parseRoleId(Guild guild, String stringToParse) {
        try {
            return Long.parseLong(stringToParse);
        } catch (NumberFormatException exception) {
            try {
                return Long.parseLong(stringToParse.replaceAll("[<@&>]", ""));
            } catch (NumberFormatException exception1) {
                return guild.getRoles().flatMap(role -> {
                    if (role.getName().trim().equalsIgnoreCase(stringToParse)) {
                        return Mono.just(role.getId().asLong());
                    } else {
                        return Mono.empty();
                    }
                }).blockLast();
            }
        }
    }
}
