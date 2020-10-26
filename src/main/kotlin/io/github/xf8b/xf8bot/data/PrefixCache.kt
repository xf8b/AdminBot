/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.data

import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.reactivestreams.client.MongoCollection
import discord4j.common.util.Snowflake
import org.bson.Document
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

class PrefixCache(private val mongoCollection: MongoCollection<Document>) {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(5))
        .build<Snowflake, Mono<String>> { snowflake ->
            mongoCollection.find(Filters.eq("guildId", snowflake.asLong())).toMono()
                .map { it.getString("prefix") }
                .switchIfEmpty(
                    mongoCollection.insertOne(
                        Document()
                            .append("guildId", snowflake.asLong())
                            .append("prefix", Xf8bot.DEFAULT_PREFIX)
                    ).toMono().thenReturn(Xf8bot.DEFAULT_PREFIX)
                )
        }

    fun getPrefix(snowflake: Snowflake): Mono<String> = cache.get(snowflake)!!

    fun setPrefix(snowflake: Snowflake, prefix: String): Mono<Void> = mongoCollection.findOneAndUpdate(
        Filters.eq("guildId", snowflake.asLong()),
        Updates.set("prefix", prefix)
    ).toMono().then(Mono.fromRunnable { cache.refresh(snowflake) })
}