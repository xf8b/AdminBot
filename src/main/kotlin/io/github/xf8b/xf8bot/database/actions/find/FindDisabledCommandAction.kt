package io.github.xf8b.xf8bot.database.actions.find

import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.api.commands.AbstractCommand

class FindDisabledCommandAction(guildId: Snowflake, command: AbstractCommand) : SelectAction(
    table = "disabledCommands",
    selectedFields = listOf("*"),
    criteria = mapOf(
        "guildId" to guildId.asLong(),
        "command" to command.rawName
    )
)