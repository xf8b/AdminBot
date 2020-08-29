package io.github.xf8b.adminbot.util.converter

import com.beust.jcommander.IStringConverter
import discord4j.core.shard.ShardingStrategy

class ShardingStrategyConverter : IStringConverter<ShardingStrategy> {
    override fun convert(value: String): ShardingStrategy = when (value.toLowerCase()) {
        "recommended" -> ShardingStrategy.recommended()
        "single" -> ShardingStrategy.single()
        else -> throw IllegalArgumentException("No such sharding strategy with the name $value exists!")
    }
}