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
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
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
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.util.Color
import io.github.xf8b.utils.semver.SemanticVersion
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.findAndRegister
import io.github.xf8b.xf8bot.commands.other.SlapBrigadierCommand
import io.github.xf8b.xf8bot.data.PrefixCache
import io.github.xf8b.xf8bot.database.BotMongoDatabase
import io.github.xf8b.xf8bot.listeners.MessageListener
import io.github.xf8b.xf8bot.listeners.ReadyListener
import io.github.xf8b.xf8bot.listeners.RoleDeleteListener
import io.github.xf8b.xf8bot.settings.BotConfiguration
import io.github.xf8b.xf8bot.util.*
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import kotlin.system.exitProcess

class Xf8bot private constructor(botConfiguration: BotConfiguration) {
    val client: GatewayDiscordClient
    val version: SemanticVersion
    private val botConfiguration: BotConfiguration
    val commandRegistry = CommandRegistry()
    private val mongoClient: MongoClient
    val botMongoDatabase: BotMongoDatabase
    val prefixCache: PrefixCache
    val audioPlayerManager: AudioPlayerManager
    val keysetHandle: KeysetHandle

    companion object {
        const val DEFAULT_PREFIX = ">"
        private val LOGGER: Logger by LoggerDelegate()

        @Throws(IOException::class, URISyntaxException::class)
        @JvmStatic
        fun main(vararg args: String) {
            AeadConfig.register()
            Schedulers.enableMetrics()
            val classLoader = Thread.currentThread().contextClassLoader
            val url = classLoader.getResource("baseConfig.toml")
                ?: throw NullPointerException("The base config file does not exist!")
            val botConfiguration = BotConfiguration(
                url,
                getUserDirAndResolve("config.toml")
            )
            JCommander.newBuilder()
                .addObject(botConfiguration)
                .build()
                .parse(*args)
            Xf8bot(botConfiguration).start().block()
        }
    }

    init {
        audioPlayerManager = DefaultAudioPlayerManager()
        audioPlayerManager.configuration.frameBufferFactory =
            AudioFrameBufferFactory { bufferDuration, format, stopping ->
                NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
            }
        AudioSourceManagers.registerRemoteSources(audioPlayerManager)
        // TODO: bass and loop commands
        // TODO: subcommands
        // TODO: member verifying system
        // TODO: use optional instead of null?
        // TODO: leveling system
        val classLoader = Thread.currentThread().contextClassLoader
        val inputStream = classLoader.getResourceAsStream("version.txt")
            ?: throw NullPointerException("The version file does not exist!")
        Scanner(inputStream).use { version = SemanticVersion(it.nextLine()) }
        client = DiscordClient.create(botConfiguration.token)
            .gateway()
            .setSharding(botConfiguration.shardingStrategy)
            .setInitialStatus { shardInfo ->
                Presence.online(Activity.playing("${botConfiguration.activity} | Shard ID: ${shardInfo.index}"))
            }
            .setEnabledIntents(
                IntentSet.of(
                    Intent.GUILDS,
                    Intent.GUILD_MEMBERS,
                    Intent.GUILD_MESSAGES,
                    //Intent.GUILD_VOICE_STATES
                )
            )
            .login()
            .doOnError { throwable ->
                LOGGER.error("Could not login!", throwable)
                exitProcess(1)
            }.block()!!
        this.botConfiguration = botConfiguration
        mongoClient = MongoClients.create(
            ParsingUtil.fixMongoConnectionUrl(
                botConfiguration.mongoConnectionUrl
            )
        )
        if (File("encryption_keyset.json").exists()) {
            keysetHandle = CleartextKeysetHandle.read(
                JsonKeysetReader.withPath(
                    getUserDirAndResolve("encryption_keyset.json")
                )
            )
        } else {
            keysetHandle = KeysetHandle.generateNew(AesGcmKeyManager.aes256GcmTemplate())
            CleartextKeysetHandle.write(
                keysetHandle, JsonKeysetWriter.withPath(
                    getUserDirAndResolve("encryption_keyset.json")
                )
            )
        }
        botMongoDatabase = BotMongoDatabase(
            ParsingUtil.fixMongoConnectionUrl(
                botConfiguration.mongoConnectionUrl
            ),
            botConfiguration.mongoDatabaseName,
            if (botConfiguration.encryptionEnabled) {
                keysetHandle
            } else {
                null
            }
        )
        prefixCache = PrefixCache(botMongoDatabase, "prefixes")
    }

    private fun start(): Mono<Void> {
        Runtime.getRuntime().addShutdownHook(Thread {
            client.logout().block()
            mongoClient.close()
            LOGGER.info("Shutting down!")
        })
        commandRegistry.findAndRegister("io.github.xf8b.xf8bot.commands")
        val messageListener = MessageListener(this, commandRegistry)
        val readyListener = ReadyListener(
            botConfiguration.activity,
            botConfiguration.botAdministrators,
            version
        )
        val roleDeleteListener = RoleDeleteListener(botMongoDatabase)
        // TODO: figure out why readyevent isnt being fired
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
            } // TODO remove
        val roleDeletePublisher: Publisher<*> = client.on<RoleDeleteEvent>()
            .flatMap { roleDeleteListener.onEventFired(it) }
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
                // TODO: move logging to webhooks
                client.getWebhookByIdWithToken(webhookIdAndToken.first, webhookIdAndToken.second)
                    .flatMap { webhook: Webhook ->
                        webhook.execute { webhookExecuteSpec: WebhookExecuteSpec ->
                            webhookExecuteSpec.setAvatarUrl(self.avatarUrl)
                                .setUsername(self.username)
                                .addEmbed { embedCreateSpec: EmbedCreateSpec ->
                                    embedCreateSpec.setTitle(":warning: Bot was restarted! :warning:")
                                        .setDescription("This is a new run!")
                                        .setColor(Color.YELLOW)
                                        .setTimestampToNow()
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
            roleDeletePublisher,
            brigadierMessageCreatePublisher,
            webhookPublisher,
            disconnectPublisher
        )
    }

    fun isBotAdministrator(snowflake: Snowflake): Boolean =
        botConfiguration.botAdministrators.contains(snowflake)
}