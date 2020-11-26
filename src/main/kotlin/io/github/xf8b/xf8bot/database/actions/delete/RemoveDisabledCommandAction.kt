package io.github.xf8b.xf8bot.database.actions.delete

import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.api.commands.AbstractCommand

class RemoveDisabledCommandAction(guildId: Snowflake, command: AbstractCommand) : DeleteAction(
    table = "disabledCommands",
    criteria = mapOf("guildId" to guildId.asLong(), "command" to command.rawName)
)