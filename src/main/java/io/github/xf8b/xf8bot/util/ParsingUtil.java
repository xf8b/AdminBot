/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class ParsingUtil {
    private ParsingUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Optional<Long> parseUserId(@NotNull Guild guild, @NotNull String stringToParse) {
        try {
            if (stringToParse.length() < 18) throw new NumberFormatException();
            //after this point we know its a user id
            return Optional.of(Long.parseLong(stringToParse));
        } catch (NumberFormatException exception) {
            try {
                if (stringToParse.replaceAll("[<@!>]", "").length() < 18) {
                    throw new NumberFormatException();
                }
                //after this point we know its a user mention
                return Optional.of(Long.parseLong(stringToParse.replaceAll("[<@!>]", "")));
            } catch (NumberFormatException exception1) {
                Flux<Long> memberUsernameMatchesMono = guild.getMembers()
                        .filter(member -> member.getUsername().trim().equalsIgnoreCase(stringToParse))
                        .map(Member::getId)
                        .map(Snowflake::asLong);
                Flux<Long> memberNicknameMatchesMono = guild.getMembers()
                        .filter(member -> member.getNickname().isPresent())
                        .filter(member -> member.getNickname().get().trim().equalsIgnoreCase(stringToParse))
                        .map(Member::getId)
                        .map(Snowflake::asLong);
                Long memberWhoMatchesUsername = memberUsernameMatchesMono.blockLast();
                return Optional.ofNullable(memberWhoMatchesUsername == null
                        ? memberNicknameMatchesMono.blockLast()
                        : memberWhoMatchesUsername);
            }
        }
    }

    public static Optional<Long> parseRoleId(@NotNull Guild guild, @NotNull String stringToParse) {
        try {
            if (stringToParse.length() < 18) throw new NumberFormatException(); //too lazy to copy paste stuff
            //after this point we know its a role id
            return Optional.of(Long.parseLong(stringToParse));
        } catch (NumberFormatException exception) {
            try {
                if (stringToParse.replaceAll("[<@!>]", "").length() < 18) {
                    throw new NumberFormatException();
                }
                //after this point we know its a role mention
                return Optional.of(Long.parseLong(stringToParse.replaceAll("[<@&>]", "")));
            } catch (NumberFormatException exception1) {
                return Optional.ofNullable(guild.getRoles().flatMap(role -> {
                    if (role.getName().trim().equalsIgnoreCase(stringToParse)) {
                        return Mono.just(role.getId().asLong());
                    } else {
                        return Mono.empty();
                    }
                }).blockLast());
            }
        }
    }

    @NotNull
    public static Optional<Snowflake> parseUserIdAsSnowflake(@NotNull Guild guild, @NotNull String stringToParse) {
        Optional<Long> id = parseUserId(guild, stringToParse);
        return id.isEmpty() ? Optional.empty() : Optional.of(Snowflake.of(id.get()));
    }

    @NotNull
    public static Optional<Snowflake> parseRoleIdAsSnowflake(@NotNull Guild guild, @NotNull String stringToParse) {
        Optional<Long> id = parseRoleId(guild, stringToParse);
        return id.isEmpty() ? Optional.empty() : Optional.of(Snowflake.of(id.get()));
    }

    @NotNull
    public static Pair<Snowflake, String> parseWebhookUrl(@NotNull String webhookUrl) {
        Pattern pattern = Pattern.compile("https://discordapp\\.com/api/webhooks/(\\d+)/(.+)");
        Matcher matcher = pattern.matcher(webhookUrl);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid webhook URL!");
        } else {
            String id = matcher.group(1);
            String token = matcher.group(2);
            return Pair.of(Snowflake.of(id), token);
        }
    }

    @NotNull
    public static String fixMongoConnectionUrl(@NotNull String connectionUrl) {
        Pattern pattern = Pattern.compile("mongodb\\+srv://(.+):(.+)@(.+)");
        Matcher matcher = pattern.matcher(connectionUrl);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid connection URL!");
        } else {
            String username = matcher.group(1);
            String password = URLEncoder.encode(matcher.group(2), StandardCharsets.UTF_8);
            String serverUrl = matcher.group(3);
            return "mongodb+srv://" + username + ":" + password + "@" + serverUrl;
        }
    }
}
