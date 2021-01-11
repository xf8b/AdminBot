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

package io.github.xf8b.xf8bot.commands.other

import com.google.common.collect.ImmutableList
import discord4j.rest.util.Color
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.createEmbedDsl
import reactor.core.publisher.Mono
import java.net.MalformedURLException
import java.time.Instant
import java.time.format.DateTimeParseException

class EmbedCommand : AbstractCommand(
    name = "\${prefix}embed",
    description = """
    Creates an embed in the current channel.
    Notes:
    - Fields are separated by commas. Do not put any spaces before or after the commas.
    - Fields are in the format <name>:<value>:<inline>. Inline is optional, and will default to `false`.
    - Timestamps must follow ISO 8601.
    - Color must be in decimal format. No hexadecimal allowed.
    """.trimIndent(),
    commandType = CommandType.OTHER,
    flags = ImmutableList.of(TITLE, URL, DESCRIPTION, FIELDS, FOOTER, COLOR, IMAGE, THUMBNAIL, TIMESTAMP)
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val title = event[TITLE]
        val description = event[DESCRIPTION]
        val url = event[URL]
        val fields = event[FIELDS]?.split(",")
        val footer = event[FOOTER]
        val color = event[COLOR]?.let(Color::of)
        val image = event[IMAGE]
        val thumbnail = event[THUMBNAIL]
        val timestamp = event[TIMESTAMP]?.let(Instant::parse)

        return event.channel.flatMap { channel ->
            channel.createEmbedDsl {
                if (title != null) title(title)
                if (description != null) description(description)
                if (url != null) url(url)

                if (fields != null) {
                    for (field in fields) {
                        val split = field.split(":")
                        val (name, value) = split
                        var inline = false
                        if (split.size >= 3) inline = split[2].toBoolean()

                        field(name, value, inline)
                    }
                }

                if (footer != null) footer(footer)
                if (color != null) color(color)
                if (image != null) image(image)
                if (thumbnail != null) thumbnail(thumbnail)
                if (timestamp != null) timestamp(timestamp)
            }
        }.then(event.message.delete())
    }

    companion object {
        private val TITLE = StringFlag(
            shortName = "title",
            longName = "title",
            required = false
        )

        private val DESCRIPTION = StringFlag(
            shortName = "d",
            longName = "description",
            required = false
        )

        private val URL = StringFlag(
            shortName = "u",
            longName = "url",
            validityPredicate = {
                try {
                    java.net.URL(it)

                    true
                } catch (exception: MalformedURLException) {
                    false
                }
            },
            required = false
        )

        private val FIELDS = StringFlag(
            shortName = "fi",
            longName = "fields",
            required = false
        )

        private val FOOTER = StringFlag(
            shortName = "fo",
            longName = "footer",
            required = false
        )

        private val COLOR = IntegerFlag(
            shortName = "c",
            longName = "color",
            required = false
        )

        private val IMAGE = StringFlag(
            shortName = "i",
            longName = "image",
            validityPredicate = {
                try {
                    java.net.URL(it)

                    true
                } catch (exception: MalformedURLException) {
                    false
                }
            },
            required = false
        )

        private val THUMBNAIL = StringFlag(
            shortName = "th",
            longName = "thumbnail",
            validityPredicate = {
                try {
                    java.net.URL(it)

                    true
                } catch (exception: MalformedURLException) {
                    false
                }
            },
            required = false
        )

        private val TIMESTAMP = StringFlag(
            shortName = "timestamp",
            longName = "timestamp",
            validityPredicate = {
                try {
                    Instant.parse(it)

                    true
                } catch (exception: DateTimeParseException) {
                    false
                }
            },
            errorMessageFunction = { "This requires an ISO compliant timestamp, such as `2007-12-03T10:15:30.00Z`." },
            required = false
        )
    }
}
