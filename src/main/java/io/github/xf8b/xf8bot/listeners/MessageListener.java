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

package io.github.xf8b.xf8bot.listeners;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.MongoCommandException;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import io.github.xf8b.xf8bot.Xf8bot;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.CommandRegistry;
import io.github.xf8b.xf8bot.api.commands.arguments.Argument;
import io.github.xf8b.xf8bot.api.commands.flags.Flag;
import io.github.xf8b.xf8bot.commands.InfoCommand;
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException;
import io.github.xf8b.xf8bot.settings.CommandHandlerChecks;
import io.github.xf8b.xf8bot.settings.DisableChecks;
import io.github.xf8b.xf8bot.util.PermissionUtil;
import io.github.xf8b.xf8bot.util.Result;
import io.github.xf8b.xf8bot.util.parser.ArgumentParser;
import io.github.xf8b.xf8bot.util.parser.FlagParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
public class MessageListener {
    @NotNull
    private final Xf8bot xf8bot;
    @NotNull
    private final CommandRegistry commandRegistry;
    private static final ExecutorService COMMAND_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("Command Pool Thread-%d")
            .build());
    private static final ArgumentParser ARGUMENT_PARSER = new ArgumentParser();
    private static final FlagParser FLAG_PARSER = new FlagParser();

    public static void shutdownCommandThreadPool() {
        COMMAND_THREAD_POOL.shutdown();
    }

    public Mono<MessageCreateEvent> onMessageCreateEvent(@NotNull MessageCreateEvent event) {
        //TODO: reactify all the classes
        //TODO: make exception handler
        //TODO: add spam protection
        Message message = event.getMessage();
        String content = message.getContent();
        Guild guild = event.getGuild().block();
        String guildId = guild.getId().asString();
        if (content.trim().equals("<@!" + event.getClient().getSelfId().asString() + "> help")) {
            InfoCommand commandHandler = commandRegistry.getCommandHandler(InfoCommand.class);
            return onCommandFired(event, commandHandler, guildId, content).thenReturn(event);
        }
        MongoCollection<Document> mongoCollection = xf8bot.getMongoDatabase().getCollection("prefixes");
        Mono.from(mongoCollection.find(Filters.eq("guildId", Long.parseLong(guildId))))
                .cast(Object.class)
                .switchIfEmpty(Mono.from(mongoCollection.insertOne(new Document()
                        .append("guildId", Long.parseLong(guildId))
                        .append("prefix", Xf8bot.DEFAULT_PREFIX))))
                .block();
        String commandType = content.trim().split(" ")[0];
        for (AbstractCommand commandHandler : commandRegistry) {
            String name = commandHandler.getNameWithPrefix(xf8bot, guildId);
            List<String> aliases = commandHandler.getAliasesWithPrefixes(xf8bot, guildId);
            if (commandType.equalsIgnoreCase(name)) {
                return onCommandFired(event, commandHandler, guildId, content).thenReturn(event);
            } else if (!aliases.isEmpty()) {
                for (String alias : aliases) {
                    if (commandType.equalsIgnoreCase(alias)) {
                        return onCommandFired(event, commandHandler, guildId, content).thenReturn(event);
                    }
                }
            }
        }
        return Mono.just(event);
    }

    private Mono<Void> onCommandFired(@NotNull MessageCreateEvent event, @NotNull AbstractCommand commandHandler, @NotNull String guildId, @NotNull String content) {
        Result<Map<Flag<?>, Object>> flagParseResult = FLAG_PARSER.parse(commandHandler, content);
        Result<Map<Argument<?>, Object>> argumentParseResult = ARGUMENT_PARSER.parse(commandHandler, content);
        if (flagParseResult.getResultType() != Result.ResultType.FAILURE && argumentParseResult.getResultType() != Result.ResultType.FAILURE) {
            CommandFiredEvent commandFiredEvent = new CommandFiredEvent(
                    xf8bot,
                    flagParseResult.getResult(),
                    argumentParseResult.getResult(),
                    event
            );
            return commandFiredEvent.getGuild()
                    .flatMap(Guild::getSelfMember)
                    .flatMap(Member::getBasePermissions)
                    .map(permissions -> permissions.containsAll(commandHandler.getBotRequiredPermissions()))
                    .flatMap(bool -> {
                        if (commandHandler.getClass().getAnnotation(DisableChecks.class) != null) {
                            if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).value())
                                    .contains(CommandHandlerChecks.BOT_HAS_REQUIRED_PERMISSIONS)) {
                                return Mono.just(bool);
                            }
                        }
                        if (!bool) {
                            commandFiredEvent.getChannel()
                                    .flatMap(messageChannel -> messageChannel
                                            .createMessage(String.format("Could not execute command \"%s\" because of insufficient permissions!", commandHandler.getNameWithPrefix(xf8bot, guildId))))
                                    .subscribe();
                            return Mono.empty();
                        } else {
                            return Mono.just(true);
                        }
                    })
                    .flatMap(bool -> {
                        if (commandHandler.getClass().getAnnotation(DisableChecks.class) != null) {
                            if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).value())
                                    .contains(CommandHandlerChecks.IS_ADMINISTRATOR)) {
                                return Mono.just(bool);
                            }
                        }
                        if (commandHandler.requiresAdministrator()) {
                            if (!PermissionUtil.canMemberUseCommand(xf8bot, commandFiredEvent.getGuild().block(), commandFiredEvent.getMember().get(), commandHandler)) {
                                commandFiredEvent.getChannel()
                                        .flatMap(messageChannel -> messageChannel.createMessage("Sorry, you don't have high enough permissions.")).subscribe();
                                return Mono.empty();
                            }
                        }
                        return Mono.just(bool);
                    })
                    .flatMap(bool -> {
                        if (commandHandler.getClass().getAnnotation(DisableChecks.class) != null) {
                            if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).value())
                                    .contains(CommandHandlerChecks.IS_BOT_ADMINISTRATOR)) {
                                return Mono.just(bool);
                            }
                        }
                        if (commandHandler.isBotAdministratorOnly()) {
                            if (!commandFiredEvent.getXf8bot().isBotAdministrator(commandFiredEvent.getMember().get().getId())) {
                                commandFiredEvent.getChannel()
                                        .flatMap(messageChannel -> messageChannel.createMessage("Sorry, you aren't a administrator of xf8bot."))
                                        .subscribe();
                                return Mono.empty();
                            }
                        }
                        return Mono.just(bool);
                    })
                    .flatMap(bool -> {
                        if (commandHandler.getClass().getAnnotation(DisableChecks.class) != null) {
                            if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).value())
                                    .contains(CommandHandlerChecks.SURPASSES_MINIMUM_AMOUNT_OF_ARGUMENTS)) {
                                return Mono.just(bool);
                            }
                        }
                        if (content.trim().split(" ").length < commandHandler.getMinimumAmountOfArgs() + 1) {
                            commandFiredEvent.getMessage().getChannel()
                                    .flatMap(messageChannel -> messageChannel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + commandHandler.getUsageWithPrefix(xf8bot, guildId) + "`."))
                                    .subscribe();
                            return Mono.empty();
                        } else {
                            return Mono.just(bool);
                        }
                    })
                    .flatMap($ -> commandHandler.onCommandFired(commandFiredEvent))
                    .doOnError(throwable -> LOGGER.error("An error happened while handling commands!", throwable))
                    .onErrorResume(ClientException.class, t -> commandFiredEvent.getChannel()
                            .flatMap(messageChannel -> messageChannel.createMessage("Client exception happened while handling command: " + t.getErrorResponse().get().getFields()))
                            .then())
                    .onErrorResume(MongoCommandException.class, t -> commandFiredEvent.getChannel()
                            .flatMap(messageChannel -> messageChannel.createMessage("Database error happened while handling command: " + t.getErrorCodeName()))
                            .then())
                    .onErrorResume(ThisShouldNotHaveBeenThrownException.class, t -> commandFiredEvent.getChannel()
                            .flatMap(messageChannel -> messageChannel.createMessage("Something has horribly gone wrong. Please report this to the bot developer with the log."))
                            .then())
                    .onErrorResume(t -> commandFiredEvent.getChannel()
                            .flatMap(messageChannel -> messageChannel.createMessage("Exception happened while handling command: " + t.getMessage()))
                            .then());
        } else {
            if (flagParseResult.getResultType() == Result.ResultType.FAILURE) {
                return event.getMessage()
                        .getChannel()
                        .flatMap(channel -> channel.createMessage(flagParseResult.getErrorMessage()))
                        .then();
            } else if (argumentParseResult.getResultType() == Result.ResultType.FAILURE) {
                return event.getMessage()
                        .getChannel()
                        .flatMap(channel -> channel.createMessage(argumentParseResult.getErrorMessage()))
                        .then();
            } else {
                throw new ThisShouldNotHaveBeenThrownException();
            }
        }
    }
}
