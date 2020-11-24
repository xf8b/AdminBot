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

package io.github.xf8b.xf8bot.data

import com.github.benmanes.caffeine.cache.Caffeine
import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.database.BotDatabase
import io.github.xf8b.xf8bot.database.actions.add.InsertAction
import io.github.xf8b.xf8bot.database.actions.find.SelectAction
import io.github.xf8b.xf8bot.database.actions.update.UpdateAction
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

class PrefixCache(
    private val botDatabase: BotDatabase,
    private val table: String
) : ReactiveMutableCache<Snowflake, String, Mono<Void>> {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(5))
        .build<Snowflake, Mono<String>> { snowflake ->
            botDatabase
                .execute(
                    SelectAction(
                        table,
                        listOf("prefix"),
                        mapOf("guildId" to snowflake.asLong())
                    )
                )
                .flatMap { it.getOrNull(0)?.map { row, _ -> row }?.toMono() ?: Mono.empty() }
                .map { it["prefix", String::class.java] }
                .switchIfEmpty(
                    botDatabase.execute(InsertAction(table, listOf(snowflake.asLong(), Xf8bot.DEFAULT_PREFIX)))
                        .toMono()
                        .thenReturn(Xf8bot.DEFAULT_PREFIX)
                )
                .single()
        }

    override fun get(key: Snowflake): Mono<String> = cache.get(key)!!

    override fun set(key: Snowflake, value: String): Mono<Void> = botDatabase
        .execute(
            UpdateAction(
                table,
                mapOf("prefix" to value),
                mapOf("guildId" to key.asLong())
            )
        )
        .toMono()
        .then(Mono.fromRunnable { cache.refresh(key) })
}