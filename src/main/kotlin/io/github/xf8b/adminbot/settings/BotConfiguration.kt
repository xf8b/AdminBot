package io.github.xf8b.adminbot.settings

import com.beust.jcommander.Parameter
import com.electronwill.nightconfig.core.file.CommentedFileConfig
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import discord4j.common.util.Snowflake
import io.github.xf8b.adminbot.util.SnowflakeConverter
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

    private val config: CommentedFileConfig = CommentedFileConfig.builder(configFilePath)
            .onFileNotFound(FileNotFoundAction.copyData(File(baseConfigFilePath)))
            .autosave()
            .build()

    init {
        config.use { it.load() }
        token = get("token")
        activity = get<String>("activity").replace("\${defaultPrefix}", GuildSettings.DEFAULT_PREFIX)
        logDumpWebhook = get("logDumpWebhook")
        botAdministrators = getAndMap<Long, Snowflake>("admins", Snowflake::of)
    }

    override fun <T> get(name: String): T = checkNotNull(config.get<T?>(name)) {
        "$name does not exist in the config!"
    }

    private fun <T, E> getAndMap(name: String, mapClosure: (T) -> E): List<E> = get<List<T>>(name).map(mapClosure)

    override fun <T> set(name: String, newValue: T) {
        config.set<T>(name, newValue)
    }
}