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
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import org.apache.commons.lang3.tuple.Pair
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

object InputParsing {
    /**
     * Parses a user ID/finds a person with the same username or nickname
     * from the passed in [stringToParse].
     *
     * The order of parsing follows:
     * 1. Try to parse the ID directly from the [stringToParse], return if
     * found
     * 2. Try to parse the ID after removing the mention from the [stringToParse],
     * return if found
     * 3. Find a member that matches the username from the [stringToParse], return
     * if found
     * 4. Find a member that matches the nickname from the [stringToParse], return
     * if found, else [Mono.empty]
     *
     * @param stringToParse the string to parse for a ID, or a member ID
     * from the guild if it isn't an id
     * @return the ID parsed from the [stringToParse] or a person that
     * matches the username/nickname.
     */
    fun parseUserId(guildPublisher: Publisher<Guild>, stringToParse: String, ignoreCase: Boolean = false): Mono<Long> {
        val guildMono = guildPublisher.toMono()

        return try {
            if (stringToParse.length != 18) throw NumberFormatException()
            // after this point we know its a user id
            stringToParse.toLong().toMono()
        } catch (_: NumberFormatException) {
            try {
                if (stringToParse.replace("[<@!>]".toRegex(), "").length != 18) {
                    throw NumberFormatException()
                }
                // after this point we know its a user mention
                stringToParse.replace("[<@!>]".toRegex(), "").toLong().toMono()
            } catch (_: NumberFormatException) {
                /*
                val memberWhoMatchesUsernameMono: Mono<Long> = guildMono.flatMapMany { guild ->
                    guild.requestMembers()
                        .filter { it.username.trim().equals(stringToParse, ignoreCase) }
                        .map(Member::getId)
                        .map(Snowflake::asLong)
                }.takeLast(1).singleOrEmpty()
                val memberWhoMatchesNicknameMono: Mono<Long> = guildMono.flatMapMany { guild ->
                    guild.members
                        .filter { it.nickname.isPresent }
                        .filter { it.nickname.get().trim().equals(stringToParse, ignoreCase) }
                        .map(Member::getId)
                        .map(Snowflake::asLong)
                }.takeLast(1).singleOrEmpty()

                memberWhoMatchesUsernameMono.switchIfEmpty(memberWhoMatchesNicknameMono)
                */

                guildMono.flatMapMany { guild ->
                    guild.requestMembers()
                        .filter { it.tag.equals(stringToParse, ignoreCase) }
                        .map(Member::getId)
                        .map(Snowflake::asLong)
                }.singleOrEmpty()
            }
        }
    }

    fun parseRoleId(guildPublisher: Publisher<Guild>, stringToParse: String, ignoreCase: Boolean = false): Mono<Long> {
        val guildMono = guildPublisher.toMono()
        return try {
            if (stringToParse.length != 18) throw NumberFormatException() // too lazy to copy paste stuff
            // after this point we know its a role id
            Mono.just(stringToParse.toLong())
        } catch (_: NumberFormatException) {
            try {
                if (stringToParse.replace("[<@&>]".toRegex(), "").length != 18) {
                    throw NumberFormatException()
                }
                // after this point we know its a role mention
                Mono.just(stringToParse.replace("[<@&>]".toRegex(), "").toLong())
            } catch (_: NumberFormatException) {
                guildMono.flatMap { guild ->
                    guild.roles
                        .filter { it.name.trim().equals(stringToParse, ignoreCase) }
                        .map { it.id }
                        .map { it.asLong() }
                        .takeLast(1)
                        .singleOrEmpty()
                }
            }
        }
    }

    /**
     * Returns the token of the webhook and the ID.
     * @param webhookUrl the url to parse the token and ID from
     * @return the token of the webhook and the ID in a [Pair]
     */
    fun parseWebhookUrl(webhookUrl: String): Double<Snowflake, String> {
        val regex = "https://discordapp\\.com/api/webhooks/(\\d+)/(.+)".toRegex()
        return if (!(webhookUrl matches regex)) {
            throw IllegalArgumentException("Invalid webhook URL!")
        } else {
            val matchResult = regex.find(webhookUrl)!!.destructured
            val id = matchResult.component1()
            val token = matchResult.component2()
            Snowflake.of(id) to token
        }
    }

}