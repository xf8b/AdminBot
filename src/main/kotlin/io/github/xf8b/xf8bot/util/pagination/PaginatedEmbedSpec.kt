package io.github.xf8b.xf8bot.util.pagination

import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.spec.EmbedCreateSpec
import io.github.xf8b.xf8bot.util.EmbedCreateDsl
import io.github.xf8b.xf8bot.util.createEmbedDsl
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeoutException

const val TRACK_PREVIOUS_EMOJI = "⏮️"
const val LEFT_ARROW_EMOJI = "⬅️"
const val RIGHT_ARROW_EMOJI = "➡️"
const val TRACK_NEXT_EMOJI = "⏭️"

fun MessageChannel.createPaginatedEmbed(vararg dslInits: EmbedCreateDsl.() -> Unit): Mono<Void> {
    val indexedDslInits = dslInits.map<EmbedCreateDsl.() -> Unit, EmbedCreateDsl.() -> Unit> { dsl ->
        {
            dsl(this)
            footer("Page 1")
        }
    }

    return this.createEmbedDsl(indexedDslInits[0]).flatMapMany { message ->
        message.client.on(ReactionAddEvent::class.java)
            .filter { event -> event.messageId == message.id }
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
                    TRACK_PREVIOUS_EMOJI -> message.edit { spec ->
                        spec.setEmbed(EmbedCreateDsl().apply(indexedDslInits[0]))
                    }.then(message.removeReaction(event.emoji, event.userId))

                    LEFT_ARROW_EMOJI -> message.edit { spec ->
                        val previousInit = indexedDslInits.find { dslInit ->
                            // i am probably commiting 26 war crimes by doing this but whatever
                            val tempSpec = EmbedCreateSpec().apply(EmbedCreateDsl().apply(dslInit)::accept)

                            tempSpec.asRequest()
                                .footer().get()
                                .text().removePrefix("Page ")
                                .toInt() == message.embeds[0]
                                .footer.get()
                                .text.removePrefix("Page ")
                                .toInt() - 1
                        }

                        if (previousInit != null) spec.setEmbed(EmbedCreateDsl().apply(previousInit))
                    }.then(message.removeReaction(event.emoji, event.userId))

                    RIGHT_ARROW_EMOJI -> message.edit { spec ->
                        val nextInit = indexedDslInits.find { dslInit ->
                            // i am probably commiting 26 war crimes by doing this but whatever
                            val tempSpec = EmbedCreateSpec().apply(EmbedCreateDsl().apply(dslInit)::accept)

                            tempSpec.asRequest()
                                .footer().get()
                                .text().removePrefix("Page ")
                                .toInt() == message.embeds[0]
                                .footer.get()
                                .text.removePrefix("Page ")
                                .toInt() + 1
                        }

                        if (nextInit != null) spec.setEmbed(EmbedCreateDsl().apply(nextInit))
                    }.then(message.removeReaction(event.emoji, event.userId))

                    TRACK_NEXT_EMOJI -> message.edit { spec ->
                        spec.setEmbed(EmbedCreateDsl().apply(indexedDslInits[indexedDslInits.size - 1]))
                    }.then(message.removeReaction(event.emoji, event.userId))

                    else -> Mono.empty()
                }
            }
            .timeout(Duration.ofMinutes(5L))
            .onErrorResume(TimeoutException::class.java) { Mono.empty() }
    }.then()
}