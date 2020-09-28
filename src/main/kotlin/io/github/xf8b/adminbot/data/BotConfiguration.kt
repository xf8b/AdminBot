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

package io.github.xf8b.adminbot.data

import com.beust.jcommander.Parameter
import com.electronwill.nightconfig.core.file.CommentedFileConfig
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import discord4j.common.util.Snowflake
import discord4j.core.shard.ShardingStrategy
import io.github.xf8b.adminbot.AdminBot
import io.github.xf8b.adminbot.util.SnowflakeConverter
import io.github.xf8b.adminbot.util.converter.ShardingStrategyConverter
import java.io.File

class BotConfiguration(baseConfigFilePath: String, configFilePath: String) : Configuration {
    @Parameter(names = ["-t", "--token"], description = "The token for AdminBot to login with", password = true)
    var token: String

    @Parameter(names = ["-a", "--activity"], description = "The activity for AdminBot")
    var activity: String

    @Parameter(names = ["-w", "--logDumpWebhook"], description = "The webhook used to dump logs")
    var logDumpWebhook: String

    @Parameter(names = ["-A", "--admins", "-b", "--botAdministrators"], description = "The user IDs which are bot administrators", converter = SnowflakeConverter::class)
    var botAdministrators: List<Snowflake>

    @Parameter(names = ["-s", "--sharding"], description = "The sharding strategy to use", converter = ShardingStrategyConverter::class)
    var shardingStrategy: ShardingStrategy

    @Parameter(names = ["-c", "--mongo-connection-url"], description = "The MongoDB connection url to use")
    var mongoConnectionUrl: String

    @Parameter(names = ["-n", "--mongo-database-name"], description = "The MongoDB database to use")
    var mongoDatabaseName: String

    private val config: CommentedFileConfig = CommentedFileConfig.builder(configFilePath)
            .onFileNotFound(FileNotFoundAction.copyData(File(baseConfigFilePath)))
            .autosave()
            .build()

    init {
        //config is closed after this point
        //can still be used to get values, but save and load will throw an exception
        config.use { it.load() }
        token = get("token")
        activity = get<String>("activity").replace("\${defaultPrefix}", AdminBot.DEFAULT_PREFIX)
        logDumpWebhook = get("logDumpWebhook")
        botAdministrators = getAndMap<Long, Snowflake>("admins", Snowflake::of)
        shardingStrategy = ShardingStrategyConverter().convert(get("sharding"))
        mongoConnectionUrl = get("mongoConnectionUrl")
        mongoDatabaseName = get("mongoDatabaseName")
    }

    override fun <T> get(name: String): T = checkNotNull(config.get<T?>(name)) {
        "$name does not exist in the config!"
    }

    private fun <T, E> getAndMap(name: String, mapClosure: (T) -> E): List<E> = get<List<T>>(name).map(mapClosure)

    override fun <T> set(name: String, newValue: T) {
        config.set<T>(name, newValue)
    }
}