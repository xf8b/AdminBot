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

package io.github.xf8b.adminbot.api.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.api.commands.arguments.Argument;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import lombok.ToString;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@ToString
public class CommandFiredEvent extends MessageCreateEvent {
    //@Getter
    private final AdminBot adminBot;
    private final Map<Flag<?>, Object> flagMap;
    private final Map<Argument<?>, Object> argumentsMap;

    public CommandFiredEvent(AdminBot adminBot, Map<Flag<?>, Object> flagMap, Map<Argument<?>, Object> argumentsMap, MessageCreateEvent event) {
        super(event.getClient(),
                event.getShardInfo(),
                event.getMessage(),
                event.getGuildId().map(Snowflake::asLong).orElse(null),
                event.getMember().orElse(null));
        this.adminBot = adminBot;
        this.flagMap = flagMap;
        this.argumentsMap = argumentsMap;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getValueOfFlag(Flag<T> flag) {
        return (T) flagMap.get(flag);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getValueOfArgument(Argument<T> argument) {
        return (T) argumentsMap.get(argument);
    }

    public Mono<MessageChannel> getChannel() {
        return getMessage().getChannel();
    }

    public Optional<User> getAuthor() {
        return getMessage().getAuthor();
    }

    //have to use this again, because kotlin
    public AdminBot getAdminBot() {
        return adminBot;
    }
}
