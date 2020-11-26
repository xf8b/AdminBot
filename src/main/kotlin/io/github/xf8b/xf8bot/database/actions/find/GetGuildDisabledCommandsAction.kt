package io.github.xf8b.xf8bot.database.actions.find

import discord4j.common.util.Snowflake

class GetGuildDisabledCommandsAction(guildId: Snowflake) : SelectAction(
    table = "disabledCommands",
    selectedFields = listOf("*"),
    criteria = mapOf("guildId" to guildId.asLong())
)