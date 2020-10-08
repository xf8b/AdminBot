package io.github.xf8b.xf8bot.data

import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.reactivestreams.client.MongoCollection
import discord4j.common.util.Snowflake
import org.bson.Document
import reactor.core.publisher.Mono
import java.time.Duration

class PrefixCache(private val mongoCollection: MongoCollection<Document>) {
    private val cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .build<Snowflake, Mono<String>> { snowflake ->
                Mono.from(mongoCollection.find(Filters.eq("guildId", snowflake.asLong())))
                        .map { it.get("prefix", String::class.java) }
            }

    fun getPrefix(snowflake: Snowflake): Mono<String> = cache.get(snowflake)!!

    fun setPrefix(snowflake: Snowflake, prefix: String): Mono<Void> = Mono.from(mongoCollection.findOneAndUpdate(
            Filters.eq("guildId", snowflake.asLong()),
            Updates.set("prefix", prefix)
    )).then(Mono.fromRunnable { cache.refresh(snowflake) })
}