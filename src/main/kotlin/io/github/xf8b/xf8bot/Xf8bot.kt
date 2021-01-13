/*
 * Copyright (c) 2020, 2021 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.spi.FilterReply
import com.beust.jcommander.JCommander
import com.github.napstr.logback.DiscordAppender
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.util.AllowedMentions
import discord4j.rest.util.Color
import io.github.xf8b.utils.semver.SemanticVersion
import io.github.xf8b.xf8bot.api.commands.Bot
import io.github.xf8b.xf8bot.api.commands.CommandRegistry
import io.github.xf8b.xf8bot.api.commands.findAndRegister
import io.github.xf8b.xf8bot.data.PrefixCache
import io.github.xf8b.xf8bot.database.BotDatabase
import io.github.xf8b.xf8bot.listeners.DeletionListener
import io.github.xf8b.xf8bot.listeners.MessageListener
import io.github.xf8b.xf8bot.listeners.ReadyListener
import io.github.xf8b.xf8bot.settings.BotConfiguration
import io.github.xf8b.xf8bot.util.*
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*
import kotlin.system.exitProcess

// TODO: bass and loop commands
// TODO: subcommands
// TODO: member verifying system
// TODO: impl encryption
// TODO: reactify all the classes
// TODO: add spam protection
// TODO: leveling system
// TODO: create webhook for bans, warns, etc?
class Xf8bot private constructor(private val botConfiguration: BotConfiguration) : Bot {
    val commandRegistry = CommandRegistry()
    val version = Scanner(resource("version.txt") ?: error("The version file does not exist!"))
        .use { SemanticVersion(it.nextLine()) }

    /*
    private val keySetHandle = if (Files.exists(getUserDirAndResolve("encryption_keyset.json"))) {
        CleartextKeysetHandle.read(JsonKeysetReader.withPath(getUserDirAndResolve("encryption_keyset.json")))
    } else {
        KeysetHandle.generateNew(AesGcmKeyManager.aes256GcmTemplate()).also {
            CleartextKeysetHandle.write(
                it,
                JsonKeysetWriter.withPath(getUserDirAndResolve("encryption_keyset.json"))
            )
        }
    }
    */

    val botDatabase = BotDatabase(
        ConnectionPool(
            ConnectionPoolConfiguration.builder(
                PostgresqlConnectionFactory(
                    PostgresqlConnectionConfiguration.builder()
                        .host(botConfiguration.databaseHost)
                        .port(botConfiguration.databasePort)
                        .username(botConfiguration.databaseUsername)
                        .password(botConfiguration.databasePassword)
                        .database(botConfiguration.databaseDatabase)
                        .build()
                )
            ).build()
        ),
        // if (botConfiguration.encryptionEnabled) keySetHandle else null
    )
    val client = DiscordClient.builder(botConfiguration.token)
        .setDefaultAllowedMentions(AllowedMentions.suppressAll())
        .build()
        .gateway()
        .setSharding(botConfiguration.shardingStrategy)
        .setInitialStatus { shardInfo ->
            Presence.online(Activity.playing("${botConfiguration.activity} | Shard ID: ${shardInfo.index}"))
        }
        .setEnabledIntents(
            IntentSet.of(
                // required for: role delete, guilds
                Intent.GUILDS,
                // required for: guild members
                Intent.GUILD_MEMBERS,
                // required for: message create, bulk delete
                Intent.GUILD_MESSAGES,
                // required for: guild voice states
                Intent.GUILD_VOICE_STATES
            )
        )
        .login()
        .doOnError {
            LOGGER.error("Could not login!", it)
            exitProcess(1)
        }
        .block()!!
    val prefixCache = PrefixCache(botDatabase)
    val audioPlayerManager = DefaultAudioPlayerManager()
        .apply { configuration.frameBufferFactory = AudioFrameBufferFactory(::NonAllocatingAudioFrameBuffer) }
        .also(AudioSourceManagers::registerRemoteSources)

    companion object {
        const val DEFAULT_PREFIX = ">"
        private val LOGGER by LoggerDelegate()

        @JvmStatic
        fun main(vararg args: String) {
            // AeadConfig.register()
            val classLoader = Thread.currentThread().contextClassLoader
            val url = classLoader.getResource("baseConfig.toml")
                ?: throw NullPointerException("The base config file does not exist!")
            val botConfiguration = BotConfiguration(url, getUserDirAndResolve("config.toml"))

            JCommander.newBuilder()
                .addObject(botConfiguration)
                .build()
                .parse(*args)

            Xf8bot(botConfiguration).start().block()
        }
    }

    override fun start(): Mono<Void> {
        Runtime.getRuntime().addShutdownHook(Thread {
            client.logout().block()
            LOGGER.info("Shutting down!")
        })

        commandRegistry.findAndRegister(packagePrefix = "io.github.xf8b.xf8bot.commands")
        val messageListener = MessageListener(this, commandRegistry)
        val readyListener = ReadyListener(
            botConfiguration.activity,
            botConfiguration.botAdministrators,
            version
        )
        val deletionListener = DeletionListener(botDatabase)

        val webhookPublisher = client.self.flatMap { self ->
            setupLogging(self.username, self.avatarUrl)

            if (botConfiguration.logDumpWebhook.isNotBlank()) {
                val (webhookId, token) = InputParsing.parseWebhookUrl(botConfiguration.logDumpWebhook)

                client.getWebhookByIdWithToken(webhookId, token).flatMap { webhook ->
                    webhook.executeDsl {
                        username(self.username)
                        avatarUrl(self.avatarUrl)

                        embed {
                            title(":warning: Bot was restarted! :warning:")
                            description("This is a new run!")

                            color(Color.YELLOW)
                            timestamp()
                        }
                    }
                }
            } else {
                Mono.empty()
            }
        }
        val onDisconnect = client.onDisconnect().doOnSuccess { LOGGER.info("Successfully disconnected!") }

        return Mono.`when`(
            client.on(readyListener),
            client.on(messageListener),
            client.on(deletionListener),
            webhookPublisher,
            onDisconnect
        )
    }

    private fun setupLogging(username: String, avatarUrl: String) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val asyncDiscord = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            .getAppender("ASYNC_DISCORD") as AsyncAppender
        val discordAppender = asyncDiscord.getAppender("DISCORD") as DiscordAppender

        discordAppender.apply {
            this.username = username
            this.avatarUrl = avatarUrl

            if (botConfiguration.logDumpWebhook.isNotBlank()) {
                this.webhookUri = botConfiguration.logDumpWebhook
            }

            discordAppender.addFilter(FunctionalFilter {
                if (botConfiguration.logDumpWebhook.isBlank()) FilterReply.DENY
                else FilterReply.NEUTRAL
            })
        }
    }

    fun isBotAdministrator(id: Snowflake) = botConfiguration.botAdministrators.contains(id)
}