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

package io.github.xf8b.xf8bot.util.pagination

import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.ReactionAddEvent
import io.github.xf8b.xf8bot.util.EmbedCreateDsl
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.extensions.isNotBot
import io.github.xf8b.xf8bot.util.extensions.toDsl
import reactor.core.publisher.Mono
import java.time.Duration

private const val TRACK_PREVIOUS_EMOJI = "⏮️"
private const val LEFT_ARROW_EMOJI = "⬅️"
private const val RIGHT_ARROW_EMOJI = "➡️"
private const val TRACK_NEXT_EMOJI = "⏭️"

fun MessageChannel.createPaginatedEmbed(vararg dslInitializers: EmbedCreateDsl.() -> Unit): Mono<Void> {
    val indexedDslInitializers = dslInitializers.mapIndexed { index, dsl -> index + 1 to dsl }
        .toMap()
        .mapValues<Int, EmbedCreateDsl.() -> Unit, EmbedCreateDsl.() -> Unit> { (index, dsl) ->
            {
                dsl(this)
                footer("Page $index")
            }
        }

    return this.createEmbedDsl(indexedDslInitializers[1]!!).flatMapMany { message ->
        message.addReaction(ReactionEmoji.unicode(TRACK_PREVIOUS_EMOJI))
            .then(message.addReaction(ReactionEmoji.unicode(LEFT_ARROW_EMOJI)))
            .then(message.addReaction(ReactionEmoji.unicode(RIGHT_ARROW_EMOJI)))
            .then(message.addReaction(ReactionEmoji.unicode(TRACK_NEXT_EMOJI)))
            .thenMany(message.client.on(ReactionAddEvent::class.java)
                .take(Duration.ofMinutes(2))
                .filter { event -> event.messageId == message.id }
                .filter { event -> event.member.map(Member::isNotBot).orElse(false) }
                .filter { event ->
                    event.emoji.asUnicodeEmoji()
                        .map(ReactionEmoji.Unicode::getRaw)
                        .map { emoji ->
                            emoji == TRACK_PREVIOUS_EMOJI
                                    || emoji == LEFT_ARROW_EMOJI
                                    || emoji == RIGHT_ARROW_EMOJI
                                    || emoji == TRACK_NEXT_EMOJI
                        }
                        .orElse(false)
                }
                .flatMap { event ->
                    when (event.emoji.asUnicodeEmoji().get().raw) {
                        TRACK_PREVIOUS_EMOJI -> event.message.flatMap { message ->
                            message.edit { spec ->
                                spec.setEmbed(EmbedCreateDsl().apply(indexedDslInitializers[1]!!))
                            }.then(message.removeReaction(event.emoji, event.userId))
                        }

                        LEFT_ARROW_EMOJI -> event.message.flatMap { message ->
                            message.edit { spec ->
                                indexedDslInitializers[message.embeds[0]
                                    .footer
                                    .get()
                                    .text
                                    .removePrefix("Page ")
                                    .toInt() - 1]
                                    ?.run { toDsl() }
                                    ?.let(spec::setEmbed)
                            }.then(message.removeReaction(event.emoji, event.userId))
                        }

                        RIGHT_ARROW_EMOJI -> event.message.flatMap { message ->
                            message.edit { spec ->
                                indexedDslInitializers[message.embeds[0]
                                    .footer
                                    .get()
                                    .text
                                    .removePrefix("Page ")
                                    .toInt() + 1]
                                    ?.run { toDsl() }
                                    ?.let(spec::setEmbed)
                            }.then(message.removeReaction(event.emoji, event.userId))
                        }

                        TRACK_NEXT_EMOJI -> event.message.flatMap { message ->
                            message.edit { spec ->
                                spec.setEmbed(indexedDslInitializers[indexedDslInitializers.size]!!.toDsl())
                            }.then(message.removeReaction(event.emoji, event.userId))
                        }

                        else -> Mono.empty()
                    }
                })
    }.then()
}