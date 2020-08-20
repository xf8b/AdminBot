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
import com.google.common.collect.ImmutableMap;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

public class ClearCommandHandler extends AbstractCommandHandler {
    public ClearCommandHandler() {
        super(
                "${prefix}clear",
                "${prefix}clear <amount>",
                "Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 100, or be below 1.",
                ImmutableMap.of(),
                ImmutableList.of("${prefix}purge"),
                CommandType.ADMINISTRATION,
                1,
                PermissionSet.of(Permission.MANAGE_MESSAGES),
                2
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String content = event.getMessage().getContent();
        MessageChannel channel = event.getChannel().block();
        int amountToClear;
        try {
            amountToClear = Integer.parseInt(content.trim().split(" ")[1].trim());
        } catch (NumberFormatException exception) {
            channel.createMessage("The amount of messages to be cleared is not a number!").block();
            return;
        }
        if (amountToClear > 100) {
            channel.createMessage("Sorry, but the limit for message clearing is 100 messages.").block();
            return;
        } else if (amountToClear < 2) {
            channel.createMessage("Sorry, but the minimum for message clearing is 2 messages.").block();
            return;
        } else if (!event.getGuild().block().getSelfMember().block().getBasePermissions().block().contains(Permission.MANAGE_MESSAGES)) {
            channel.createMessage("Cannot clear messages due to insufficient permissions!").block();
            return;
        }
        Flux<Message> deleteMessagesFlux = channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(amountToClear);
        Long amountOfMessagesPurged = deleteMessagesFlux.count().block();
        channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(amountToClear)
                .transform(((TextChannel) channel)::bulkDeleteMessages)
                .doOnComplete(() -> channel.createMessage("Successfully purged " + amountOfMessagesPurged + " message(s).")
                        .delayElement(Duration.ofSeconds(3)).flatMap(Message::delete).subscribe())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10008).and(ClientException.isStatusCode(404)), throwable -> Mono.empty()) //unknown message
                .subscribe();
    }
}
