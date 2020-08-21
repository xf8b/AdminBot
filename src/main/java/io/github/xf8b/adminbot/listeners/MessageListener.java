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
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.handlers.AbstractCommandHandler;
import io.github.xf8b.adminbot.handlers.InfoCommandHandler;
import io.github.xf8b.adminbot.helpers.PrefixesDatabaseHelper;
import io.github.xf8b.adminbot.settings.CommandHandlerChecks;
import io.github.xf8b.adminbot.settings.DisableChecks;
import io.github.xf8b.adminbot.settings.GuildSettings;
import io.github.xf8b.adminbot.util.CommandRegistry;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
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

    public void onMessageCreateEvent(MessageCreateEvent event) {
        //TODO: reactify all the classes
        //TODO: make exception handler
        //TODO: add spam protection
        Message message = event.getMessage();
        String content = message.getContent();
        String guildId = event.getGuild().map(Guild::getId)
                .map(Snowflake::asString)
                .block();
        try {
            if (PrefixesDatabaseHelper.doesGuildNotExistInDatabase(guildId)) {
                new GuildSettings(guildId);
            }
        } catch (ClassNotFoundException | SQLException exception) {
            LOGGER.error("An exception happened while reading from the prefixes database!", exception);
        }
        if (content.trim().replaceAll("<@!" + event.getClient().getSelfId().asString() + ">", "").trim().equals("help")) {
            CommandFiredEvent commandFiredEvent = new CommandFiredEvent(adminBot, event);
            commandRegistry.getCommandHandler(InfoCommandHandler.class).onCommandFired(commandFiredEvent);
        }
        String commandType = content.trim().split(" ")[0];
        for (AbstractCommandHandler commandHandler : commandRegistry) {
            String name = commandHandler.getNameWithPrefix(guildId);
            List<String> aliases = commandHandler.getAliasesWithPrefixes(guildId);
            if (commandType.equalsIgnoreCase(name)) {
                CommandFiredEvent commandFiredEvent = new CommandFiredEvent(adminBot, event);
                onCommandFired(commandFiredEvent, commandHandler, guildId, content);
            } else if (!aliases.isEmpty()) {
                for (String alias : aliases) {
                    if (commandType.equalsIgnoreCase(alias)) {
                        CommandFiredEvent commandFiredEvent = new CommandFiredEvent(adminBot, event);
                        onCommandFired(commandFiredEvent, commandHandler, guildId, content);
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
                        if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).disabledChecks())
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
                        if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).disabledChecks())
                                .contains(CommandHandlerChecks.IS_ADMINISTRATOR)) {
                            return Mono.just(bool);
                        }
                    }
                    if (commandHandler.requiresAdministrator()) {
                        if (PermissionUtil.getAdministratorLevel(event.getGuild().block(), event.getMember().get()) < commandHandler.getAdministratorLevelRequired()) {
                            event.getChannel().flatMap(messageChannel -> messageChannel.createMessage("Sorry, you don't have high enough permissions.")).subscribe();
                            return Mono.empty();
                        }
                    }
                    return Mono.just(bool);
                })
                .flatMap(bool -> {
                    if (commandHandler.getClass().getAnnotation(DisableChecks.class) != null) {
                        if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).disabledChecks())
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
                        if (Arrays.asList(commandHandler.getClass().getAnnotation(DisableChecks.class).disabledChecks())
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
                .subscribe(bool -> COMMAND_THREAD_POOL.submit(() -> commandHandler.onCommandFired(event)));
    }

    public static void shutdownCommandThreadPool() {
        COMMAND_THREAD_POOL.shutdown();
    }
}
