package io.github.xf8b.xf8bot.util.extensions

import discord4j.core.`object`.Embed
import discord4j.core.spec.EmbedCreateSpec
import io.github.xf8b.utils.optional.toNullable
import io.github.xf8b.xf8bot.util.EmbedCreateDsl

fun (EmbedCreateDsl.() -> Unit).toDsl() = EmbedCreateDsl().apply(this)

fun EmbedCreateDsl.toSpec() = EmbedCreateSpec().apply(this::accept)

fun Embed.toDsl() = EmbedCreateDsl().also { dsl ->
    this.description.ifPresent(dsl::description)
    this.url.ifPresent(dsl::url)
    this.timestamp.ifPresent(dsl::timestamp)
    this.color.ifPresent(dsl::color)
    this.footer.ifPresent { footer -> dsl.footer(footer.text, footer.iconUrl.toNullable()) }
    this.image.ifPresent { image -> dsl.image(image.url) }
    this.thumbnail.ifPresent { thumbnail -> dsl.thumbnail(thumbnail.url) }
}