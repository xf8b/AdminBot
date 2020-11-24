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
    private val actions: MutableList<MessageCreateSpec.() -> MessageCreateSpec> = ArrayList()

    fun content(content: String) = actions.add { setContent(content) }.let { }

    fun nonce(nonce: Snowflake) = actions.add { setNonce(nonce) }.let { }

    fun tts(tts: Boolean) = actions.add { setTts(tts) }.let { }

    fun allowedMentions(allowedMentions: AllowedMentions) = actions.add {
        setAllowedMentions(allowedMentions)
    }.let { }

    fun embed(consumer: EmbedCreateDsl.() -> Unit) = actions.add {
        val dsl = EmbedCreateDsl()
        consumer(dsl)
        setEmbed(dsl)
    }.let { }

    fun messageReference(messageId: Snowflake) = actions.add { setMessageReference(messageId) }.let { }

    // file
    fun file(fileName: String, inputStream: InputStream) = actions.add {
        addFile(fileName, inputStream)
    }.let { }

    fun spoilerFile(fileName: String, inputStream: InputStream) = actions.add {
        addFileSpoiler(fileName, inputStream)
    }.let { }

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    override fun accept(t: MessageCreateSpec) = actions.forEach { it(t) }
}

class EmbedCreateDsl : Consumer<EmbedCreateSpec> {
    private val actions: MutableList<EmbedCreateSpec.() -> EmbedCreateSpec> = ArrayList()

    fun title(title: String) = actions.add { setTitle(title) }.let { }

    fun url(url: String) = actions.add { setUrl(url) }.let { }

    fun description(description: String) = actions.add { setDescription(description) }.let { }

    fun footer(footer: String, iconUrl: String? = null) = actions.add { setFooter(footer, iconUrl) }.let { }

    fun field(name: String, value: String, inline: Boolean) = actions.add { addField(name, value, inline) }.let { }

    fun color(color: Color) = actions.add { setColor(color) }.let { }

    fun timestamp(timestamp: Instant = Instant.now()) = actions.add { setTimestamp(timestamp) }.let { }

    fun author(name: String, url: String? = null, iconUrl: String? = null) = actions.add {
        setAuthor(name, url, iconUrl)
    }.let { }

    fun image(url: String) = actions.add { setImage(url) }.let { }

    fun thumbnail(url: String) = actions.add { setThumbnail(url) }.let { }

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    override fun accept(t: EmbedCreateSpec) = actions.forEach { it(t) }
}

class WebhookExecuteDsl : Consumer<WebhookExecuteSpec> {
    private val actions: MutableList<WebhookExecuteSpec.() -> WebhookExecuteSpec> = ArrayList()

    fun content(content: String) = actions.add { setContent(content) }.let { }

    fun tts(tts: Boolean) = actions.add { setTts(tts) }.let { }

    fun file(fileName: String, inputStream: InputStream) = actions.add {
        addFile(fileName, inputStream)
    }.let { }

    fun spoilerFile(fileName: String, inputStream: InputStream) = actions.add {
        addFileSpoiler(fileName, inputStream)
    }.let { }

    fun embed(consumer: EmbedCreateDsl.() -> Unit) = actions.add {
        val dsl = EmbedCreateDsl()
        consumer(dsl)
        addEmbed(dsl)
    }.let { }

    fun username(username: String) = actions.add { setUsername(username) }.let { }

    fun avatarUrl(url: String) = actions.add { setAvatarUrl(url) }.let { }

    fun allowedMentions(allowedMentions: AllowedMentions) = actions.add {
        setAllowedMentions(allowedMentions)
    }.let { }

    override fun accept(t: WebhookExecuteSpec) = actions.forEach { it(t) }
}

fun MessageChannel.createMessageDsl(dslClosure: MessageCreateDsl.() -> Unit): Mono<Message> {
    val dsl = MessageCreateDsl()
    dslClosure(dsl)
    return createMessage(dsl)
}

fun MessageChannel.createEmbedDsl(dslClosure: EmbedCreateDsl.() -> Unit): Mono<Message> {
    val dsl = EmbedCreateDsl()
    dslClosure(dsl)
    return createEmbed(dsl)
}

fun Webhook.executeDsl(dslClosure: WebhookExecuteDsl.() -> Unit): Mono<Void> {
    val dsl = WebhookExecuteDsl()
    dslClosure(dsl)
    return execute(dsl)
}