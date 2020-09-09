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

package io.github.xf8b.adminbot.listeners;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.Argument;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import io.github.xf8b.adminbot.handlers.InfoCommandHandler;
import io.github.xf8b.adminbot.settings.CommandHandlerChecks;
import io.github.xf8b.adminbot.settings.DisableChecks;
import io.github.xf8b.adminbot.util.CommandRegistry;
import io.github.xf8b.adminbot.util.PermissionUtil;
import io.github.xf8b.adminbot.util.parser.ArgumentParser;
import io.github.xf8b.adminbot.util.parser.FlagParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
public class MessageListener {
    private final AdminBot adminBot;
    private final CommandRegistry commandRegistry;
    private static final ExecutorService COMMAND_THREAD_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("Command Pool Thread-%d")
            .build());
    private static final ArgumentParser ARGUMENT_PARSER = new ArgumentParser();
    private static final FlagParser FLAG_PARSER = new FlagParser();

    public static void shutdownCommandThreadPool() {
        COMMAND_THREAD_POOL.shutdown();
    }

    public void onMessageCreateEvent(MessageCreateEvent event) {
        //TODO: reactify all the classes
        //TODO: make exception handler
        //TODO: add spam protection
        Message message = event.getMessage();
        String content = message.getContent();
        MessageChannel channel = event.getMessage().getChannel().block();
        String guildId = event.getGuild().map(Guild::getId)
                .map(Snowflake::asString)
                .block();
        if (content.trim().equals("<@!" + event.getClient().getSelfId().asString() + "> help")) {
            InfoCommandHandler commandHandler = commandRegistry.getCommandHandler(InfoCommandHandler.class);
            Map<Flag<?>, Object> flagMap = FLAG_PARSER.parse(channel, commandHandler, content);
            Map<Argument<?>, Object> argumentMap = ARGUMENT_PARSER.parse(channel, commandHandler, content);
            if (flagMap != null && argumentMap != null) {
                CommandFiredEvent commandFiredEvent = new CommandFiredEvent(adminBot, flagMap, argumentMap, event);
                commandHandler.onCommandFired(commandFiredEvent);
            }
        }
        String commandType = content.trim().split(" ")[0];
        for (AbstractCommandHandler commandHandler : commandRegistry) {
            String name = commandHandler.getNameWithPrefix(guildId);
            List<String> aliases = commandHandler.getAliasesWithPrefixes(guildId);
            if (commandType.equalsIgnoreCase(name)) {
                Map<Flag<?>, Object> flagMap = FLAG_PARSER.parse(channel, commandHandler, content);
                Map<Argument<?>, Object> argumentMap = ARGUMENT_PARSER.parse(channel, commandHandler, content);
                if (flagMap != null && argumentMap != null) {
                    CommandFiredEvent commandFiredEvent = new CommandFiredEvent(adminBot, flagMap, argumentMap, event);
                    onCommandFired(commandFiredEvent, commandHandler, guildId, content);
                }
            } else if (!aliases.isEmpty()) {
                for (String alias : aliases) {
                    if (commandType.equalsIgnoreCase(alias)) {
                        Map<Flag<?>, Object> flagMap = FLAG_PARSER.parse(channel, commandHandler, content);
                        Map<Argument<?>, Object> argumentMap = ARGUMENT_PARSER.parse(channel, commandHandler, content);
                        if (flagMap != null && argumentMap != null) {
                            CommandFiredEvent commandFiredEvent = new CommandFiredEvent(adminBot, flagMap, argumentMap, event);
                            onCommandFired(commandFiredEvent, commandHandler, guildId, content);
                        }
                    }
                }
            }
        }
    }

    private void onCommandFired(CommandFiredEvent event, AbstractCommandHandler commandHandler, String guildId, String content) {
        event.getGuild()
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
                        event.getChannel()
                                .flatMap(messageChannel -> messageChannel
                                        .createMessage(String.format("Could not execute command \"%s\" because of insufficient permissions!", commandHandler.getNameWithPrefix(guildId))))
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
                        if (!PermissionUtil.canMemberUseCommand(event.getGuild().block(), event.getMember().get(), commandHandler)) {
                            event.getChannel().flatMap(messageChannel -> messageChannel.createMessage("Sorry, you don't have high enough permissions.")).subscribe();
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
                        if (!event.getAdminBot().isBotAdministrator(event.getMember().get().getId())) {
                            event.getChannel()
                                    .flatMap(messageChannel -> messageChannel.createMessage("Sorry, you aren't a administrator of AdminBot."))
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
                        event.getMessage().getChannel()
                                .flatMap(messageChannel -> messageChannel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + commandHandler.getUsageWithPrefix(guildId) + "`."))
                                .subscribe();
                        return Mono.empty();
                    } else {
                        return Mono.just(bool);
                    }
                })
                .subscribe($ -> COMMAND_THREAD_POOL.submit(() -> commandHandler.onCommandFired(event)));
    }
}
