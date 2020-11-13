/*
 * Copyright (c) 2020 xf8b.
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

package io.github.xf8b.xf8bot.settings

import com.beust.jcommander.Parameter
import com.electronwill.nightconfig.core.file.CommentedFileConfig
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import discord4j.common.util.Snowflake
import discord4j.core.shard.ShardingStrategy
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.settings.converter.ShardingStrategyConverter
import io.github.xf8b.xf8bot.settings.converter.SnowflakeConverter
import io.github.xf8b.xf8bot.util.env
import io.github.xf8b.xf8bot.util.envOrElse
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSnowflake
import java.net.URL
import java.nio.file.Path

class BotConfiguration(baseConfigFilePath: URL, configFilePath: Path) : Configuration {
    @Parameter(names = ["-t", "--token"], description = "The token for xf8bot to login with", password = true)
    var token: String

    @Parameter(names = ["-a", "--activity"], description = "The activity for xf8bot")
    var activity: String

    @Parameter(names = ["-w", "--logDumpWebhook"], description = "The webhook used to dump logs")
    var logDumpWebhook: String

    @Parameter(
        names = ["-A", "--admins", "-b", "--botAdministrators"],
        description = "The user IDs which are bot administrators",
        converter = SnowflakeConverter::class
    )
    var botAdministrators: List<Snowflake>

    @Parameter(
        names = ["-s", "--sharding"],
        description = "The sharding strategy to use",
        converter = ShardingStrategyConverter::class
    )
    var shardingStrategy: ShardingStrategy

    @Parameter(names = ["-c", "--mongo-connection-url"], description = "The MongoDB connection url to use")
    var mongoConnectionUrl: String

    @Parameter(names = ["-n", "--mongo-database-name"], description = "The MongoDB database to use")
    var mongoDatabaseName: String

    @Parameter(names = ["-e", "--encryption"], description = "Whether to enable encryption for the database")
    var encryptionEnabled: Boolean

    private val config: CommentedFileConfig = CommentedFileConfig.builder(configFilePath)
        .onFileNotFound(FileNotFoundAction.copyData(baseConfigFilePath))
        .build()

    init {
        // config is closed after this point
        // can still be used to get values, but save and load will throw an exception
        config.use { it.load() }
        token = envOrElse("BOT_TOKEN", get("token"))
        activity = envOrElse(
            "BOT_ACTIVITY", get<String>("activity").replace(
                "\${defaultPrefix}",
                Xf8bot.DEFAULT_PREFIX
            )
        )
        logDumpWebhook = envOrElse("BOT_LOG_DUMP_WEBHOOK", get("logDumpWebhook"))
        botAdministrators = env("BOT_ADMINISTRATOR")
            ?.toSnowflake()
            ?.toSingletonImmutableList()
            ?: get<List<Long>>("admins").map { it.toSnowflake() }
        shardingStrategy = ShardingStrategyConverter().convert(envOrElse("BOT_SHARDING_STRATEGY", get("sharding")))
        mongoConnectionUrl = envOrElse("BOT_DATABASE_URL", get("mongoConnectionUrl"))
        mongoDatabaseName = envOrElse("BOT_DATABASE_NAME", get("mongoDatabaseName"))
        encryptionEnabled = env("BOT_ENCRYPTION_ENABLED")?.toBoolean() ?: get("enableEncryption")
    }

    override fun <T> get(name: String): T = checkNotNull(getOrNull(name)) {
        "$name does not exist in the config!"
    }

    override fun <T> set(name: String, newValue: T) =
        throw UnsupportedOperationException("Cannot set value of field when config is closed!")

    private fun <T> getOrNull(name: String): T? = config.get<T?>(name)
}