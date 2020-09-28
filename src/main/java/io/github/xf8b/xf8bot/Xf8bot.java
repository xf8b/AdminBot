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

package io.github.xf8b.xf8bot;

import com.beust.jcommander.JCommander;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.Color;
import io.github.xf8b.xf8bot.api.commands.CommandRegistry;
import io.github.xf8b.xf8bot.commands.SlapBrigadierCommand;
import io.github.xf8b.xf8bot.data.BotConfiguration;
import io.github.xf8b.xf8bot.listeners.MessageListener;
import io.github.xf8b.xf8bot.listeners.ReadyListener;
import io.github.xf8b.xf8bot.util.LogbackUtil;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Slf4j
public class Xf8bot {
    public static final String DEFAULT_PREFIX = ">";
    private final String version;
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final GatewayDiscordClient client;
    @NotNull
    private final BotConfiguration botConfiguration;
    private final MongoDatabase mongoDatabase;
    private final AudioPlayerManager audioPlayerManager;

    private Xf8bot(BotConfiguration botConfiguration) throws IOException, URISyntaxException {
        audioPlayerManager = new DefaultAudioPlayerManager();
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        //TODO: subcommands
        //TODO: member verifying system
        //TODO: use optional instead of null?
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("version.txt");
        if (url == null) throw new NullPointerException("The version file does not exist!");
        version = Files.readAllLines(Path.of(url.toURI())).get(0);
        client = DiscordClient.create(botConfiguration.getToken())
                .gateway()
                .setSharding(botConfiguration.getShardingStrategy())
                .setInitialStatus(shardInfo -> Presence.online(Activity.playing(String.format(
                        "%s | Shard ID: %d",
                        botConfiguration.getActivity(), shardInfo.getIndex()
                ))))
                .setEnabledIntents(IntentSet.nonPrivileged().or(IntentSet.of(Intent.GUILD_MEMBERS)))
                .login()
                .doOnError(throwable -> {
                    LOGGER.error("Could not login!", throwable);
                    System.exit(0);
                })
                .block();
        this.botConfiguration = botConfiguration;
        MongoClient mongoClient = MongoClients.create(ParsingUtil.fixMongoConnectionUrl(
                botConfiguration.getMongoConnectionUrl()
        ));
        mongoDatabase = mongoClient.getDatabase(botConfiguration.getMongoDatabaseName());
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        BotConfiguration botConfiguration = new BotConfiguration(
                "baseConfig.toml",
                "config.toml"
        );
        JCommander.newBuilder()
                .addObject(botConfiguration)
                .build()
                .parse(args);
        new Xf8bot(botConfiguration).start();
    }

    private void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.logout().block();
            LOGGER.info("Shutting down!");
        }));
        commandRegistry.slurpCommandHandlers("io.github.xf8b.xf8bot.commands");
        MessageListener messageListener = new MessageListener(this, commandRegistry);
        ReadyListener readyListener = new ReadyListener(
                botConfiguration.getActivity(),
                botConfiguration.getBotAdministrators(),
                version
        );
        //TODO: figure out why readyevent isnt being fired
        client.on(ReadyEvent.class)
                .flatMap(readyListener::onReadyEvent)
                .subscribe();
        client.on(MessageCreateEvent.class)
                .filter(event -> !event.getMessage().getContent().isEmpty())
                .filter(event -> event.getMember().isPresent())
                .filter(event -> event.getMessage().getAuthor().isPresent())
                .filter(event -> !event.getMessage().getAuthor().get().isBot())
                .flatMap(messageListener::onMessageCreateEvent)
                .subscribe();
        CommandDispatcher<MessageCreateEvent> commandDispatcher = new CommandDispatcher<>();
        SlapBrigadierCommand.register(commandDispatcher);
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
        User self = client.getSelf().block();
        if (!botConfiguration.getLogDumpWebhook().isBlank()) {
            Pair<Snowflake, String> webhookIdAndToken = ParsingUtil.parseWebhookUrl(botConfiguration.getLogDumpWebhook());
            //TODO: move logging to webhooks
            client.getWebhookByIdWithToken(webhookIdAndToken.getLeft(), webhookIdAndToken.getRight())
                    .flatMap(webhook -> webhook.execute(webhookExecuteSpec -> webhookExecuteSpec.setAvatarUrl(self.getAvatarUrl())
                            .setUsername(self.getUsername())
                            .addEmbed(embedCreateSpec -> embedCreateSpec.setTitle(":warning: Bot was restarted! :warning:")
                                    .setDescription("This is a new run!")
                                    .setColor(Color.YELLOW)
                                    .setTimestamp(Instant.now()))))
                    .subscribe();
        }
        LogbackUtil.setupDiscordAppender(botConfiguration.getLogDumpWebhook(), self.getUsername(), self.getAvatarUrl());
        client.onDisconnect()
                .doOnSuccess(ignored -> LOGGER.info("Successfully disconnected!"))
                .block();
    }

    public boolean isBotAdministrator(Snowflake snowflake) {
        return botConfiguration.getBotAdministrators().contains(snowflake);
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public String getVersion() {
        return this.version;
    }

    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    public GatewayDiscordClient getClient() {
        return this.client;
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }
}
