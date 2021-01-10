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

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import org.apache.commons.lang3.StringUtils
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

object InputParsing {
    private const val WEBHOOK_REGEX = "https://discordapp\\.com/api/webhooks/(\\d+)/(.+)"

    fun parseUserId(guild: Publisher<Guild>, input: String): Mono<Long> {
        val cleanedInput = input.removePrefix("<@").removePrefix("!").removeSuffix(">")

        return if (cleanedInput.length == 18 && StringUtils.isNumeric(cleanedInput)) {
            cleanedInput.toLong().toMono()
        } else {
            val membersMatched = guild.toMono()
                .flatMapMany(Guild::requestMembers)
                .filter {
                    it.username == input
                            || it.nickname.map(input::equals).orElse(false)
                            || it.tag == input
                }
                .cache()

            membersMatched.singleOrEmpty()
                .onErrorResume<IndexOutOfBoundsException, Member> {
                    membersMatched.filter { it.tag == input }.singleOrEmpty()
                }
                .map { it.id.asLong() }
        }
    }

    fun parseRoleId(guild: Publisher<Guild>, input: String): Mono<Long> {
        val cleanedInput = input.removePrefix("<@&").removeSuffix(">")

        return if (cleanedInput.length == 18 && StringUtils.isNumeric(cleanedInput)) {
            cleanedInput.toLong().toMono()
        } else {
            guild.toMono().flatMapMany(Guild::getRoles)
                .filter { role -> role.name == input }
                .map { role -> role.id.asLong() }
                .singleOrEmpty()
        }
    }

    /**
     * Parses the ID and token of a webhook from the [webhookUrl].
     */
    fun parseWebhookUrl(webhookUrl: String) = if (webhookUrl matches WEBHOOK_REGEX.toRegex()) {
        val (id, token) = WEBHOOK_REGEX.toRegex().find(webhookUrl)!!.destructured

        id.toSnowflake() to token
    } else {
        throw IllegalArgumentException("Invalid webhook URL!")
    }
}