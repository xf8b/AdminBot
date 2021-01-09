package io.github.xf8b.xf8bot.commands.botadministrator

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
    description = "Creates an embed in the current channel",
    commandType = CommandType.BOT_ADMINISTRATOR,
    flags = ImmutableList.of(TITLE, URL, DESCRIPTION, FIELDS, FOOTER, COLOR, IMAGE, THUMBNAIL, TIMESTAMP),
    botAdministratorOnly = true
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val title = event[TITLE]!!
        val description = event[DESCRIPTION]!!
        val url = event[URL]
        val fields = event[FIELDS]?.split(",")
        val footer = event[FOOTER]
        val color = event[COLOR]?.let(Color::of)
        val image = event[IMAGE]
        val thumbnail = event[THUMBNAIL]
        val timestamp = event[TIMESTAMP]?.let(Instant::parse)

        return event.channel.flatMap { channel ->
            channel.createEmbedDsl {
                title(title)
                description(description)
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
                if (timestamp != null) timestamp(timestamp) else timestamp()
            }
        }.then(event.message.delete())
    }

    companion object {
        private val TITLE = StringFlag(
            shortName = "title",
            longName = "title"
        )

        private val DESCRIPTION = StringFlag(
            shortName = "d",
            longName = "description"
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