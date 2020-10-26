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

package io.github.xf8b.xf8bot

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.spi.FilterReply
import com.beust.jcommander.JCommander
import com.github.napstr.logback.DiscordAppender
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoDatabase
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.Webhook
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.gateway.ShardInfo
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.util.Color
import discord4j.store.caffeine.CaffeineStoreService
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.findAndRegister
import io.github.xf8b.xf8bot.commands.other.SlapBrigadierCommand
import io.github.xf8b.xf8bot.data.PrefixCache
import io.github.xf8b.xf8bot.listeners.MessageListener
import io.github.xf8b.xf8bot.listeners.ReadyListener
import io.github.xf8b.xf8bot.settings.BotConfiguration
import io.github.xf8b.xf8bot.util.*
import io.micrometer.core.instrument.Metrics.globalRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class Xf8bot private constructor(botConfiguration: BotConfiguration) {
    val version: String
    val commandRegistry = CommandRegistry()
    val client: GatewayDiscordClient
    private val botConfiguration: BotConfiguration
    val mongoDatabase: MongoDatabase
    val prefixCache: PrefixCache
    val audioPlayerManager: AudioPlayerManager

    init {
        audioPlayerManager = DefaultAudioPlayerManager()
        audioPlayerManager.configuration.frameBufferFactory =
            AudioFrameBufferFactory { bufferDuration: Int, format: AudioDataFormat, stopping: AtomicBoolean ->
                NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
            }
        AudioSourceManagers.registerRemoteSources(audioPlayerManager)
        //TODO: bass and loop commands
        //TODO: subcommands
        //TODO: member verifying system
        //TODO: use optional instead of null?
        val classLoader = Thread.currentThread().contextClassLoader
        val inputStream = classLoader.getResourceAsStream("version.txt")
            ?: throw NullPointerException("The version file does not exist!")
        Scanner(inputStream).use { scanner ->
            version = scanner.nextLine()
        }
        client = DiscordClient.create(botConfiguration.token)
            .gateway()
            .setSharding(botConfiguration.shardingStrategy)
            .setStoreService(CaffeineStoreService())
            .setInitialStatus { shardInfo: ShardInfo ->
                Presence.online(Activity.playing("${botConfiguration.activity} | Shard ID: ${shardInfo.index}"))
            }
            .setEnabledIntents(IntentSet.nonPrivileged().or(IntentSet.of(Intent.GUILD_MEMBERS)))
            .login()
            .doOnError {
                LOGGER.error("Could not login!", it)
                exitProcess(0)
            }.block()!!
        //TODO: coroutines
        this.botConfiguration = botConfiguration
        val mongoClient = MongoClients.create(
            ParsingUtil.fixMongoConnectionUrl(
                botConfiguration.mongoConnectionUrl
            )
        )
        mongoDatabase = mongoClient.getDatabase(botConfiguration.mongoDatabaseName)
        prefixCache = PrefixCache(mongoDatabase.getCollection("prefixes"))
    }

    companion object {
        const val DEFAULT_PREFIX = ">"
        private val LOGGER: Logger by LoggerDelegate()

        @Throws(IOException::class, URISyntaxException::class)
        @JvmStatic
        fun main(vararg args: String) {
            Schedulers.enableMetrics()
            val classLoader = Thread.currentThread().contextClassLoader
            val url = classLoader.getResource("baseConfig.toml")
                ?: throw NullPointerException("The base config file does not exist!")
            val botConfiguration = BotConfiguration(
                url,
                Path.of(System.getProperty("user.dir")).resolve("config.toml")
            )
            JCommander.newBuilder()
                .addObject(botConfiguration)
                .build()
                .parse(*args)
            Xf8bot(botConfiguration).start().block()
        }
    }

    private fun start(): Mono<Void> {
        Runtime.getRuntime().addShutdownHook(Thread {
            client.logout().block()
            LOGGER.info("Shutting down!")
        })
        commandRegistry.findAndRegister("io.github.xf8b.xf8bot.commands")
        val messageListener = MessageListener(this, commandRegistry)
        val readyListener = ReadyListener(
            botConfiguration.activity,
            botConfiguration.botAdministrators,
            version
        )
        //TODO: figure out why readyevent isnt being fired
        val readyPublisher: Publisher<*> = client.on<ReadyEvent>()
            .flatMap { readyListener.onEventFired(it) }
        val messageCreateEventPublisher: Publisher<*> = client.on<MessageCreateEvent>()
            .filter { it.message.content.isNotEmpty() }
            .filter { it.member.isPresent }
            .filter { it.message.author.isPresent }
            .filter { it.message.author.get().isNotBot }
            .flatMap { messageListener.onEventFired(it) }
            .onErrorContinue { throwable, _ ->
                LOGGER.error("Error happened while handling message create events", throwable)
            } //TODO remove
        val commandDispatcher = CommandDispatcher<MessageCreateEvent>()
        SlapBrigadierCommand.register(commandDispatcher)
        val brigadierMessageCreatePublisher: Publisher<*> = client.on<MessageCreateEvent>()
            .filter { it.message.content.isNotEmpty() }
            .filter { it.member.isPresent }
            .filter { it.message.author.isPresent }
            .filter { it.message.author.get().isNotBot }
            .filter { it.message.content.startsWith(">slap") }
            .flatMap {
                Mono.defer {
                    try {
                        Mono.fromRunnable { commandDispatcher.execute(it.message.content, it) }
                    } catch (exception: CommandSyntaxException) {
                        it.message.channel.flatMap {
                            it.createMessage("CommandSyntaxException: $exception")
                        }
                    }
                }
            }
        val webhookPublisher: Publisher<*> = client.self.flatMap { self: User ->
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            val discordAsync = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
                .getAppender("ASYNC_DISCORD") as AsyncAppender
            val discordAppender = discordAsync.getAppender("DISCORD") as DiscordAppender
            discordAppender.username = self.username
            discordAppender.avatarUrl = self.avatarUrl
            val webhookUrl = botConfiguration.logDumpWebhook
            discordAppender.addFilter(FunctionalFilter {
                if (webhookUrl.trim().isBlank()) {
                    FilterReply.DENY
                } else {
                    FilterReply.NEUTRAL
                }
            })
            if (!webhookUrl.isBlank()) {
                discordAppender.webhookUri = webhookUrl
                val webhookIdAndToken = ParsingUtil.parseWebhookUrl(webhookUrl)
                //TODO: move logging to webhooks
                client.getWebhookByIdWithToken(webhookIdAndToken.left, webhookIdAndToken.right)
                        .flatMap { webhook: Webhook ->
                            webhook.execute { webhookExecuteSpec: WebhookExecuteSpec ->
                                webhookExecuteSpec.setAvatarUrl(self.avatarUrl)
                                        .setUsername(self.username)
                                        .addEmbed { embedCreateSpec: EmbedCreateSpec ->
                                            embedCreateSpec.setTitle(":warning: Bot was restarted! :warning:")
                                                    .setDescription("This is a new run!")
                                                    .setColor(Color.YELLOW)
                                                    .setTimestamp(Instant.now())
                                        }
                            }
                        }
            } else {
                Mono.empty()
            }
        }
        val disconnectPublisher: Publisher<*> = client.onDisconnect()
            .doOnSuccess { LOGGER.info("Successfully disconnected!") }
        return Mono.`when`(
            readyPublisher,
            messageCreateEventPublisher,
            brigadierMessageCreatePublisher,
            webhookPublisher,
            disconnectPublisher
        )
    }

    fun isBotAdministrator(snowflake: Snowflake): Boolean =
        botConfiguration.botAdministrators.contains(snowflake)
}