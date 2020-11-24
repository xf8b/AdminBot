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
import io.github.xf8b.xf8bot.util.toSnowflake
import java.net.URL
import java.nio.file.Path

class BotConfiguration(baseConfigFilePath: URL, configFilePath: Path) {
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

    @Parameter(names = ["-h", "--database-host"], description = "The host of the database server")
    var databaseHost: String

    @Parameter(names = ["-p", "--database-port"], description = "The database server's port")
    var databasePort: Int

    @Parameter(names = ["-u", "--database-username"], description = "The database server's user's username")
    var databaseUsername: String

    @Parameter(names = ["-P", "--database-password"], description = "The database server's user's password")
    var databasePassword: String

    @Parameter(names = ["-d", "--database-database"], description = "The database server's name")
    var databaseDatabase: String

    @Parameter(names = ["-e", "--encryption"], description = "Whether to enable encryption for the database")
    var encryptionEnabled: Boolean

    private val config: CommentedFileConfig = CommentedFileConfig.builder(configFilePath)
        .onFileNotFound(FileNotFoundAction.copyData(baseConfigFilePath))
        .build()

    init {
        // config is closed after this point
        // can still be used to get values, but save and load will throw an exception
        config.use { it.load() }
        token = config.getOrElse("required.token", env("BOT_TOKEN"))
            ?: error("A bot token is required!")
        activity = config
            .getOrElse("required.activity", env("BOT_ACTIVITY")) ?: error("A activity is required!")
            .replace("\${defaultPrefix}", Xf8bot.DEFAULT_PREFIX)
        logDumpWebhook = config.getOrElse("notrequired.logDumpWebhook", env("BOT_LOG_DUMP_WEBHOOK"))
            ?: ""
        botAdministrators = config
            .get<List<Long>?>("required.admins")
            ?.map(Long::toSnowflake)
            ?: listOf(
                env("BOT_ADMINISTRATOR")
                    ?.toSnowflake()
                    ?: error("Bot administrator(s) are required!")
            )
        shardingStrategy = ShardingStrategyConverter().convert(
            config.getOrElse(
                "required.sharding",
                env("BOT_SHARDING_STRATEGY")
            ) ?: error("A sharding strategy is required!")
        )
        databaseHost = config.getOrElse(
            "required.database.host",
            env("BOT_DATABASE_HOST")
        ) ?: error("A database host is required!")
        databasePort = config.getOrElse(
            "required.database.port",
            env("BOT_DATABASE_PORT")?.toInt()
        ) ?: error("A database port is required!")
        databaseUsername = config.getOrElse(
            "required.database.username",
            env("BOT_DATABASE_USERNAME")
        ) ?: error("A database username is required!")
        databasePassword = config.getOrElse(
            "required.database.password",
            env("BOT_DATABASE_PASSWORD")
        ) ?: error("A database password is required!")
        databaseDatabase = config.getOrElse(
            "required.database.database",
            env("BOT_DATABASE_DATABASE")
        ) ?: error("A database database is required!")
        encryptionEnabled = config.getOrElse(
            "required.database.enableEncryption",
            env("BOT_ENCRYPTION_ENABLED")?.toBoolean()
        ) ?: error("Bot database encryption setting is required!")
    }
}