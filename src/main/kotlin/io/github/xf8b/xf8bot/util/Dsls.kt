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

package io.github.xf8b.xf8bot.util

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Webhook
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateSpec
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.rest.util.AllowedMentions
import discord4j.rest.util.Color
import reactor.core.publisher.Mono
import java.io.InputStream
import java.time.Instant
import java.util.function.Consumer

class MessageCreateDsl : Consumer<MessageCreateSpec> {
    private val actions = mutableListOf<MessageCreateSpec.() -> Unit>()

    fun content(content: String) {
        actions.add { setContent(content) }
    }

    fun nonce(nonce: Snowflake) {
        actions.add { setNonce(nonce) }
    }

    fun tts(tts: Boolean) {
        actions.add { setTts(tts) }
    }

    fun allowedMentions(allowedMentions: AllowedMentions) {
        actions.add { setAllowedMentions(allowedMentions) }
    }

    fun embed(consumer: EmbedCreateDsl.() -> Unit) {
        actions.add { setEmbed(EmbedCreateDsl().apply(consumer)) }
    }

    fun messageReference(messageId: Snowflake) {
        actions.add { setMessageReference(messageId) }
    }

    // file
    fun file(fileName: String, inputStream: InputStream) {
        actions.add { addFile(fileName, inputStream) }
    }

    fun spoilerFile(fileName: String, inputStream: InputStream) {
        actions.add { addFileSpoiler(fileName, inputStream) }
    }

    override fun accept(t: MessageCreateSpec) {
        for (action in actions) action(t)
    }
}

class EmbedCreateDsl : Consumer<EmbedCreateSpec> {
    private val actions = mutableListOf<EmbedCreateSpec.() -> Unit>()

    fun title(title: String) {
        actions.add { setTitle(title) }
    }

    fun url(url: String) {
        actions.add { setUrl(url) }
    }

    fun description(description: String) {
        actions.add { setDescription(description) }
    }

    fun footer(footer: String, iconUrl: String? = null) {
        actions.add { setFooter(footer, iconUrl) }
    }

    fun field(name: String, value: String, inline: Boolean) {
        actions.add { addField(name, value, inline) }
    }

    fun color(color: Color) {
        actions.add { setColor(color) }
    }

    fun timestamp(timestamp: Instant = Instant.now()) {
        actions.add { setTimestamp(timestamp) }
    }

    fun author(name: String, url: String? = null, iconUrl: String? = null) {
        actions.add { setAuthor(name, url, iconUrl) }
    }

    fun image(url: String) {
        actions.add { setImage(url) }
    }

    fun thumbnail(url: String) {
        actions.add { setThumbnail(url) }
    }

    fun copy(other: EmbedCreateDsl) {
        this.actions.clear()
        this.actions += other.actions
    }

    override fun accept(t: EmbedCreateSpec) {
        for (action in actions) action(t)
    }
}

class WebhookExecuteDsl : Consumer<WebhookExecuteSpec> {
    private val actions = mutableListOf<WebhookExecuteSpec.() -> Unit>()

    fun content(content: String) {
        actions.add { setContent(content) }
    }

    fun tts(tts: Boolean) {
        actions.add { setTts(tts) }
    }

    fun file(fileName: String, inputStream: InputStream) {
        actions.add { addFile(fileName, inputStream) }
    }

    fun spoilerFile(fileName: String, inputStream: InputStream) {
        actions.add { addFileSpoiler(fileName, inputStream) }
    }

    fun embed(consumer: EmbedCreateDsl.() -> Unit) {
        actions.add { addEmbed(EmbedCreateDsl().apply(consumer)) }
    }

    fun username(username: String) {
        actions.add { setUsername(username) }
    }

    fun avatarUrl(url: String) {
        actions.add { setAvatarUrl(url) }
    }

    fun allowedMentions(allowedMentions: AllowedMentions) {
        actions.add { setAllowedMentions(allowedMentions) }
    }

    override fun accept(t: WebhookExecuteSpec) {
        for (action in actions) action(t)
    }
}

fun MessageChannel.createMessageDsl(init: MessageCreateDsl.() -> Unit): Mono<Message> =
    createMessage(MessageCreateDsl().apply(init))

fun MessageChannel.createEmbedDsl(init: EmbedCreateDsl.() -> Unit): Mono<Message> =
    createEmbed(EmbedCreateDsl().apply(init))

fun Webhook.executeDsl(init: WebhookExecuteDsl.() -> Unit): Mono<Void> = execute(WebhookExecuteDsl().apply(init))