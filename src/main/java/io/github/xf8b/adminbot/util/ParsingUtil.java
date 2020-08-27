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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.arguments.Argument;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.jodah.typetools.TypeResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                Long memberWhoMatchesUsername = memberUsernameMatchesMono.blockLast();
                if (memberWhoMatchesUsername == null) {
                    return memberNicknameMatchesMono.blockLast();
                } else {
                    return memberWhoMatchesUsername;
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

    @Nullable
    public Snowflake parseUserIdAndReturnSnowflake(Guild guild, String stringToParse) {
        Long id = parseUserId(guild, stringToParse);
        if (id == null) {
            return null;
        } else {
            return Snowflake.of(id);
        }
    }

    @Nullable
    public Snowflake parseRoleIdAndReturnSnowflake(Guild guild, String stringToParse) {
        Long id = parseRoleId(guild, stringToParse);
        if (id == null) {
            return null;
        } else {
            return Snowflake.of(id);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Map<Flag<T>, T> parseFlags(MessageChannel messageChannel, AbstractCommandHandler commandHandler, String messageContent) {
        Map<Flag<T>, T> flagMap = new HashMap<>();
        List<String> invalidFlags = new ArrayList<>();
        Map<Flag<T>, T> invalidValues = new HashMap<>();
        List<Flag<T>> missingFlags = new ArrayList<>();
        Matcher matcher = Pattern.compile(Flag.REGEX).matcher(messageContent);
        while (matcher.find()) {
            String flagName = matcher.group(2);
            Flag<T> flag;
            if (matcher.group(1).equals("--")) {
                flag = (Flag<T>) commandHandler.getFlags()
                        .stream()
                        .filter(flag1 -> flag1.longName().equals(flagName))
                        .findFirst()
                        .orElse(null);
            } else {
                flag = (Flag<T>) commandHandler.getFlags()
                        .stream()
                        .filter(flag1 -> flag1.shortName().equals(flagName))
                        .findFirst()
                        .orElse(null);
            }
            if (flag == null) {
                invalidFlags.add(flagName);
                break;
            }
            String tempValue = matcher.group(3).trim();
            T value;
            if (tempValue.matches("\"[\\w ]+\"")) {
                if (TypeResolver.resolveRawArgument(Flag.class, flag.getClass()) == String.class) {
                    value = (T) tempValue.substring(1, tempValue.length() - 1);
                } else {
                    invalidValues.put(flag, (T) tempValue);
                    break;
                }
            } else {
                if (flag.isValidValue(tempValue)) {
                    value = flag.parse(tempValue);
                } else {
                    invalidValues.put(flag, (T) tempValue);
                    break;
                }
            }
            flagMap.put(flag, value);
        }
        commandHandler.getFlags().forEach(flag -> {
            if (!flagMap.containsKey(flag)) {
                if (flag.isRequired()) {
                    missingFlags.add((Flag<T>) flag);
                }
            }
        });
        if (!missingFlags.isEmpty()) {
            StringBuilder invalidFlagsNames = new StringBuilder();
            missingFlags.forEach(flag -> invalidFlagsNames.append("`").append(flag.shortName()).append("`")
                    .append("/")
                    .append("`").append(flag.longName()).append("`")
                    .append(" "));
            messageChannel.createMessage(String.format(
                    "Missing flag(s) %s!",
                    invalidFlagsNames.toString().trim()
            )).subscribe();
            return null;
        } else if (!invalidFlags.isEmpty()) {
            messageChannel.createMessage(String.format("Invalid flag(s) `%s`!",
                    String.join(", ", invalidFlags)))
                    .subscribe();
            return null;
        } else if (!invalidValues.isEmpty()) {
            StringBuilder invalidValuesFormatted = new StringBuilder();
            invalidValues.forEach((flag, invalidValue) -> {
                Class<?> clazz = TypeResolver.resolveRawArgument(Flag.class, flag.getClass());
                invalidValuesFormatted.append("Flag: ")
                        .append("`").append(flag.shortName()).append("`")
                        .append("/")
                        .append("`").append(flag.longName()).append("`")
                        .append(" , Error message: ")
                        .append(String.format(
                                flag.getInvalidValueErrorMessage((String) invalidValue),
                                ((String) invalidValue).trim(),
                                clazz.getSimpleName()
                        ))
                        .append(" ");
            });
            messageChannel.createMessage(String.format("Invalid value(s): %s", invalidValuesFormatted.toString()))
                    .subscribe();
            return null;
        } else {
            return ImmutableMap.copyOf(flagMap);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Map<Argument<T>, T> parseArguments(MessageChannel messageChannel, AbstractCommandHandler commandHandler, String messageContent) {
        Map<Argument<T>, T> flagMap = new HashMap<>();
        Map<Argument<T>, T> invalidValues = new HashMap<>();
        List<Argument<T>> missingArguments = new ArrayList<>();
        String[] strings = messageContent.replaceAll(Flag.REGEX, "").split(" ");
        List<Argument<?>> arguments = commandHandler.getArguments();
        arguments.forEach(argument -> {
            try {
                StringBuilder stringAtIndexOfArgument = new StringBuilder();
                //should be fixed
                //todo fix if it breaks
                if (!argument.index().hasUpperBound()) {
                    stringAtIndexOfArgument.append(Arrays.stream(strings)
                            .skip(argument.index().lowerEndpoint())
                            .collect(Collectors.joining()))
                            .append(" ");
                } else {
                    for (int i = argument.index().lowerEndpoint(); argument.index().contains(i); i++) {
                        stringAtIndexOfArgument.append(strings[i]).append(" ");
                    }
                }
                if (argument.isValidValue(stringAtIndexOfArgument.toString().trim())) {
                    flagMap.put((Argument<T>) argument, (T) argument.parse(stringAtIndexOfArgument.toString().trim()));
                } else {
                    invalidValues.put((Argument<T>) argument, (T) stringAtIndexOfArgument.toString().trim());
                }
            } catch (IndexOutOfBoundsException exception) {
                if (argument.isRequired()) {
                    missingArguments.add((Argument<T>) argument);
                }
            }
        });
        if (!missingArguments.isEmpty()) {
            List<String> missingArgumentsIndexes = missingArguments.stream()
                    .map(Argument::index)
                    .map(Range::toString)
                    .collect(Collectors.toUnmodifiableList());
            messageChannel.createMessage(String.format("Missing argument(s) at indexes %s!", String.join(", ", missingArgumentsIndexes)))
                    .subscribe();
            return null;
        } else if (!invalidValues.isEmpty()) {
            StringBuilder invalidValuesFormatted = new StringBuilder();
            invalidValues.forEach((argument, invalidValue) -> {
                Class<?> clazz = TypeResolver.resolveRawArgument(Argument.class, argument.getClass());
                invalidValuesFormatted.append("Argument at index ")
                        .append(argument.index().toString())
                        .append(", Error message: ")
                        .append(String.format(
                                argument.getInvalidValueErrorMessage((String) invalidValue),
                                ((String) invalidValue).trim(),
                                argument.index(),
                                clazz.getSimpleName()
                        ))
                        .append(" ");
            });
            messageChannel.createMessage(String.format("Invalid value(s): %s", invalidValuesFormatted.toString()))
                    .subscribe();
            return null;
        } else {
            return ImmutableMap.copyOf(flagMap);
        }
    }
}
