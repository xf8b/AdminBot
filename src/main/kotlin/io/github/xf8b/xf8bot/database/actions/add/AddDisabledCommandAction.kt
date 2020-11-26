package io.github.xf8b.xf8bot.database.actions.add

import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.api.commands.AbstractCommand

class AddDisabledCommandAction(guildId: Snowflake, command: AbstractCommand) : InsertAction(
    table = "disabledCommands",
    listOf(guildId.asLong(), command.rawName)
)