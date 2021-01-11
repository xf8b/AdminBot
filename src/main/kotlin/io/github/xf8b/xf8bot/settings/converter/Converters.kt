/*
 * Copyright (c) 2020, 2021 xf8b.
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

package io.github.xf8b.xf8bot.settings.converter

import discord4j.common.util.Snowflake
import discord4j.core.shard.ShardingStrategy
import io.github.xf8b.xf8bot.util.Converter
import io.github.xf8b.xf8bot.util.toSnowflake

class ShardingStrategyConverter : Converter<ShardingStrategy> {
    override fun convert(value: String): ShardingStrategy = when (value.toLowerCase()) {
        "recommended" -> ShardingStrategy.recommended()
        "single" -> ShardingStrategy.single()
        else -> throw IllegalArgumentException("No such sharding strategy with the name \"$value\" exists!")
    }
}

class SnowflakeConverter : Converter<Snowflake> {
    override fun convert(value: String): Snowflake = value.toSnowflake()
}