package io.github.xf8b.adminbot.data

import discord4j.common.util.Snowflake

data class WarnContext(val memberWhoWarnedId: Snowflake, val reason: String, val warnId: Int)