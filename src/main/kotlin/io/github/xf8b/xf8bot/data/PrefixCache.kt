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

package io.github.xf8b.xf8bot.data

import com.github.benmanes.caffeine.cache.Caffeine
import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.database.BotDatabase
import io.github.xf8b.xf8bot.database.actions.add.AddPrefixAction
import io.github.xf8b.xf8bot.database.actions.find.FindPrefixAction
import io.github.xf8b.xf8bot.database.actions.update.UpdatePrefixAction
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

class PrefixCache(private val botDatabase: BotDatabase) : ReactiveMutableCache<Snowflake, String, Mono<Void>> {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(5))
        .build<Snowflake, Mono<String>> { guildId ->
            botDatabase.execute(FindPrefixAction(guildId)).flatMap { results ->
                results.getOrNull(0)
                    ?.map { row, _ -> row["prefix", String::class.java]!! }
                    ?.toMono()
                    ?: botDatabase.execute(AddPrefixAction(guildId, Xf8bot.DEFAULT_PREFIX))
                        .thenReturn(Xf8bot.DEFAULT_PREFIX)
            }.single()
        }

    override fun get(key: Snowflake): Mono<String> = cache.get(key)!!

    override fun set(key: Snowflake, value: String): Mono<Void> = botDatabase.execute(UpdatePrefixAction(key, value))
        .then(Mono.fromRunnable<Void> { cache.invalidate(key) }.subscribeOn(Schedulers.boundedElastic()))
}