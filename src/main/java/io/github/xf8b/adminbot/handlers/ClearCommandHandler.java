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

import com.google.common.collect.Range;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.Argument;
import io.github.xf8b.adminbot.api.commands.arguments.IntegerArgument;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

public class ClearCommandHandler extends AbstractCommandHandler {
    private static final IntegerArgument AMOUNT = IntegerArgument.builder()
            .setIndex(Range.singleton(1))
            .setName("amount")
            .setValidityPredicate(value -> {
                try {
                    int amount = Integer.parseInt(value);
                    return amount <= 100 && amount >= 2;
                } catch (NumberFormatException exception) {
                    return false;
                }
            })
            .setInvalidValueErrorMessageFunction(invalidValue -> {
                try {
                    int amount = Integer.parseInt(invalidValue);
                    if (amount > 100) {
                        return "Sorry, but you cannot clear more than 100 messages.";
                    } else if (amount < 2) {
                        return "Sorry, but you cannot clear less than 2 messages.";
                    } else {
                        throw new IllegalStateException("tf");
                    }
                } catch (NumberFormatException exception) {
                    return Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE;
                }
            })
            .build();

    public ClearCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}clear")
                .setDescription("Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 100, or be below 1.")
                .setCommandType(CommandType.ADMINISTRATION)
                .addAlias("${prefix}purge")
                .setMinimumAmountOfArgs(1)
                .addArgument(AMOUNT)
                .setBotRequiredPermissions(PermissionSet.of(Permission.MANAGE_MESSAGES))
                .setAdministratorLevelRequired(2));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        int amountToClear = event.getValueOfArgument(AMOUNT);
        if (!event.getGuild().block().getSelfMember().block().getBasePermissions().block().contains(Permission.MANAGE_MESSAGES)) {
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
                        .delayElement(Duration.ofSeconds(3))
                        .flatMap(Message::delete)
                        .subscribe())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10008), throwable -> Mono.empty()) //unknown message
                .subscribe();
    }
}
