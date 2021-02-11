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