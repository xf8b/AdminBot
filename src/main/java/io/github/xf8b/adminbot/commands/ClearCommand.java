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

import com.google.common.collect.Range;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.Argument;
import io.github.xf8b.adminbot.api.commands.arguments.IntegerArgument;
import io.github.xf8b.adminbot.exceptions.ThisShouldNotHaveBeenThrownException;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClearCommand extends AbstractCommand {
    private static final ExecutorService CLEAR_THREAD_POOL = Executors.newCachedThreadPool();
    private static final IntegerArgument AMOUNT = IntegerArgument.builder()
            .setIndex(Range.singleton(1))
            .setName("amount")
            .setValidityPredicate(value -> {
                try {
                    int amount = Integer.parseInt(value);
                    return amount >= 2;
                } catch (NumberFormatException exception) {
                    return false;
                }
            })
            .setInvalidValueErrorMessageFunction(invalidValue -> {
                try {
                    int amount = Integer.parseInt(invalidValue);
                    if (amount < 2) {
                        return "Sorry, but you cannot clear less than 2 messages.";
                    } else {
                        throw new ThisShouldNotHaveBeenThrownException();
                    }
                } catch (NumberFormatException exception) {
                    return Argument.DEFAULT_INVALID_VALUE_ERROR_MESSAGE;
                }
            })
            .build();

    public ClearCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}clear")
                .setDescription("Clears the specified amount of messages. The amount of messages to be cleared cannot exceed 100, or be below 1.")
                .setCommandType(CommandType.ADMINISTRATION)
                .addAlias("${prefix}purge")
                .setMinimumAmountOfArgs(1)
                .addArgument(AMOUNT)
                .setBotRequiredPermissions(Permission.MANAGE_MESSAGES)
                .setAdministratorLevelRequired(2));
    }

    public static void shutdownClearThreadPool() {
        CLEAR_THREAD_POOL.shutdown();
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        int amountToClear = event.getValueOfArgument(AMOUNT)
                .orElseThrow(ThisShouldNotHaveBeenThrownException::new);
        long amountOfMessagesPurged = channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(amountToClear)
                .count()
                .blockOptional()
                .orElseThrow(ThisShouldNotHaveBeenThrownException::new);
        return channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(amountToClear)
                .transform(((TextChannel) channel)::bulkDeleteMessages)
                .flatMap(Message::delete)
                .doOnComplete(() -> channel.createMessage("Successfully purged " + amountOfMessagesPurged + " message(s).")
                        .delayElement(Duration.ofSeconds(3))
                        .flatMap(Message::delete)
                        .subscribe())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10008), throwable -> Flux.empty()) //unknown message
                .subscribeOn(Schedulers.boundedElastic())
                .then();
        //TODO: see if this threading works
    }
}
