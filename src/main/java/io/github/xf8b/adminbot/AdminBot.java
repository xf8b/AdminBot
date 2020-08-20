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

package io.github.xf8b.adminbot;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.shard.ShardingStrategy;
import io.github.xf8b.adminbot.handlers.SlapBrigadierCommand;
import io.github.xf8b.adminbot.listeners.MessageListener;
import io.github.xf8b.adminbot.settings.GuildSettings;
import io.github.xf8b.adminbot.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AdminBot {
    @Getter
    private final String version;
    @Getter
    private final CommandRegistry commandRegistry = new CommandRegistry();
    @Getter
    private final GatewayDiscordClient client;
    private final BotSettings botSettings;

    private static class BotSettings {
        @Parameter(names = {"-t", "--token"}, description = "The token for AdminBot to login with", password = true)
        private String token = ConfigUtil.readToken();
        @Parameter(names = {"-a", "--activity"}, description = "The activity for AdminBot")
        private String activity = ConfigUtil.readActivity().replace("${defaultPrefix}", GuildSettings.DEFAULT_PREFIX);
        @Parameter(names = {"-w", "--logDumpWebhook"}, description = "The webhook used to dump logs")
        private String logDumpWebhook = ConfigUtil.readLogDumpWebhook();
        @Parameter(names = {"-A", "--admins"}, description = "The user IDs which are admins", converter = SnowflakeConverter.class)
        private List<Snowflake> admins = ConfigUtil.readAdmins();
    }

    private AdminBot(BotSettings botSettings) throws IOException, URISyntaxException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource("version.txt");
        if (url == null) throw new NullPointerException("The version file does not exist!");
        version = Files.readAllLines(Paths.get(url.toURI())).get(0);
        //TODO: make command flags
        //TODO: make command arguments parser
        //TODO: make ban, kick, and warn commands not allow you to do it if you are lower than the person you are trying to ban/kick/warn
        client = DiscordClient.create(botSettings.token)
                .gateway()
                .setSharding(ShardingStrategy.recommended())
                .setInitialStatus(shardInfo -> Presence.online(Activity.playing(String.format(
                        "%s | Shard ID: %d",
                        botSettings.activity, shardInfo.getIndex()
                ))))
                .login()
                .doOnError(throwable -> {
                    LOGGER.error("Could not login!", throwable);
                    ShutdownHandler.shutdownWithError(throwable);
                })
                .block();
        this.botSettings = botSettings;
        //@formatter:off
        //if (!botSettings.logDumpWebhook.trim().equals("")) {
            /*
            client.getWebhookById(Snowflake.of(botSettings.logDumpWebhook)).subscribe(webhook -> {
                webhook.edit(webhookEditSpec -> webhookEditSpec.setAvatar(client.getSelf().block().getAvatar().block())
                        .setName("AdminBot"))
                        .block();
                       */
            /*
            * "\n" +
            * "------------------\n" +
            * "Bot was restarted!\n" +
            * "------------------\n" +
            * "\n"
            */
            //});
        //}
        //@formatter:on
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ShutdownHandler.shutdown();
            client.logout().block();
        }));
        FileUtil.createFolders();
        FileUtil.createFiles();
        commandRegistry.slurpCommandHandlers("io.github.xf8b.adminbot.handlers");
        MessageListener messageListener = new MessageListener(this, commandRegistry);
        client.on(ReadyEvent.class).subscribe(event -> {
            LOGGER.info("Successfully started AdminBot version {}!", version);
            LOGGER.info("Logged in as {}#{}.", event.getSelf().getUsername(), event.getSelf().getDiscriminator());
            LOGGER.info("Logged into {} guilds.", event.getGuilds().size());
            LOGGER.info("Total shards: {}", event.getShardInfo().getCount());
            LOGGER.info("Bot arguments: ");
            LOGGER.info("Activity: {}", botSettings.activity);
            LOGGER.info("Bot administrators: {}", botSettings.admins.stream()
                    .map(Snowflake::asLong)
                    .collect(Collectors.toUnmodifiableList()));
        });
        client.on(MessageCreateEvent.class)
                .filter(event -> !event.getMessage().getContent().isEmpty())
                .filter(event -> event.getMember().isPresent())
                .filter(event -> event.getMessage().getAuthor().isPresent())
                .filter(event -> !event.getMessage().getAuthor().get().isBot())
                .subscribe(messageListener::onMessageCreateEvent);
        CommandDispatcher<MessageCreateEvent> commandDispatcher = new CommandDispatcher<>();
        new SlapBrigadierCommand().register(commandDispatcher);
        client.on(MessageCreateEvent.class).subscribe(messageCreateEvent -> {
            if (messageCreateEvent.getMember().isEmpty()) return;
            if (messageCreateEvent.getMember().get().isBot()) return;
            if (!messageCreateEvent.getMessage().getContent().startsWith(">slap")) return;
            try {
                commandDispatcher.execute(messageCreateEvent.getMessage().getContent(), messageCreateEvent);
            } catch (CommandSyntaxException exception) {
                messageCreateEvent.getMessage()
                        .getChannel()
                        .flatMap(messageChannel -> messageChannel.createMessage("CommandSyntaxException: " + exception))
                        .subscribe();
            }
        });
        //TODO: move logging util to webhooks
        User self = client.getSelf().block();
        LogbackUtil.setupDiscordAppender(botSettings.logDumpWebhook, self.getUsername(), self.getAvatarUrl());
        client.onDisconnect().block();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        BotSettings botSettings = new BotSettings();
        JCommander.newBuilder()
                .addObject(botSettings)
                .build()
                .parse(args);
        new AdminBot(botSettings);
    }

    public boolean isAdmin(Snowflake snowflake) {
        return botSettings.admins.contains(snowflake);
    }
}
