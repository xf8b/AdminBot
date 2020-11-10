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
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.database.BotMongoDatabase
import io.github.xf8b.xf8bot.database.actions.add.AddDocumentAction
import io.github.xf8b.xf8bot.database.actions.find.FindAllMatchingAction
import io.github.xf8b.xf8bot.database.actions.find.FindAndUpdateAction
import org.bson.Document
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

class PrefixCache(
    private val botMongoDatabase: BotMongoDatabase,
    val collectionName: String
) : ReactiveMutableCache<Snowflake, String, Mono<Void>> {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(5))
        .build<Snowflake, Mono<String>> { snowflake ->
            botMongoDatabase.execute(
                FindAllMatchingAction(
                    collectionName,
                    Filters.eq("guildId", snowflake.asLong())
                )
            ).toMono()
                .map { it.getString("prefix") }
                .switchIfEmpty(
                    botMongoDatabase.execute(
                        AddDocumentAction(
                            collectionName,
                            Document(
                                mapOf(
                                    "guildId" to snowflake.asLong(),
                                    "prefix" to Xf8bot.DEFAULT_PREFIX
                                )
                            )
                        )
                    ).toMono().thenReturn(Xf8bot.DEFAULT_PREFIX)
                )
        }

    override fun get(key: Snowflake): Mono<String> = cache.get(key)!!

    override fun set(key: Snowflake, value: String): Mono<Void> = botMongoDatabase.execute(
        FindAndUpdateAction(
            collectionName,
            Filters.eq("guildId", key.asLong()),
            Updates.set("prefix", value)
        )
    ).toMono().then(Mono.fromRunnable { cache.refresh(key) })
}