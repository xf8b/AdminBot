/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.util

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import org.apache.commons.lang3.tuple.Pair
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object ParsingUtil {
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
    @JvmStatic
    fun parseUserId(guildPublisher: Publisher<Guild>, stringToParse: String): Mono<Long> {
        val guildMono = guildPublisher.toMono()
        return try {
            if (stringToParse.length < 18) throw NumberFormatException()
            //after this point we know its a user id
            stringToParse.toLong().toMono()
        } catch (exception: NumberFormatException) {
            try {
                if (stringToParse.replace("[<@!>]".toRegex(), "").length < 18) {
                    throw NumberFormatException()
                }
                //after this point we know its a user mention
                stringToParse.replace("[<@!>]".toRegex(), "").toLong().toMono()
            } catch (exception1: NumberFormatException) {
                val memberWhoMatchesUsernameMono: Mono<Long> = guildMono.flatMapMany { guild: Guild ->
                    guild.requestMembers()
                        .filter { it.username.trim().equals(stringToParse, ignoreCase = true) }
                        .map(Member::getId)
                        .map(Snowflake::asLong)
                }.takeLast(1).singleOrEmpty()
                val memberWhoMatchesNicknameMono: Mono<Long> = guildMono.flatMapMany { guild: Guild ->
                    guild.members
                        .filter { it.nickname.isPresent }
                        .filter { it.nickname.get().trim().equals(stringToParse, ignoreCase = true) }
                        .map(Member::getId)
                        .map(Snowflake::asLong)
                }.takeLast(1).singleOrEmpty()

                memberWhoMatchesUsernameMono.switchIfEmpty(memberWhoMatchesNicknameMono)
            }
        }
    }

    @JvmStatic
    fun parseRoleId(guildPublisher: Publisher<Guild>, stringToParse: String): Mono<Long> {
        val guildMono = guildPublisher.toMono()
        return try {
            if (stringToParse.length < 18) throw NumberFormatException() //too lazy to copy paste stuff
            //after this point we know its a role id
            Mono.just(stringToParse.toLong())
        } catch (_: NumberFormatException) {
            try {
                if (stringToParse.replace("[<@!>]".toRegex(), "").length < 18) {
                    throw NumberFormatException()
                }
                //after this point we know its a role mention
                Mono.just(stringToParse.replace("[<@&>]".toRegex(), "").toLong())
            } catch (_: NumberFormatException) {
                guildMono.flatMap { guild: Guild ->
                    guild.roles
                        .filter { role: Role ->
                            role.name
                                .trim { it <= ' ' }
                                .equals(stringToParse, ignoreCase = true)
                        }
                        .map { obj: Role -> obj.id }
                        .map { obj: Snowflake -> obj.asLong() }
                        .takeLast(1)
                        .singleOrEmpty()
                }
            }
        }
    }

    @JvmStatic
    @Deprecated(
        "Use parseUserId!", replaceWith = ReplaceWith(
            "ParsingUtil.parseUserId",
            "io.github.xf8b.xf8bot.util.ParsingUtil"
        )
    )
    fun parseUserIdAsSnowflake(guild: Publisher<Guild>, stringToParse: String): Mono<Snowflake> {
        val id = parseUserId(guild, stringToParse)
        return id.map(Snowflake::of)
    }

    @JvmStatic
    @Deprecated(
        "Use parseRoleId!", replaceWith = ReplaceWith(
            "ParsingUtil.parseRoleId",
            "io.github.xf8b.xf8bot.util.ParsingUtil"
        )
    )
    fun parseRoleIdAsSnowflake(guild: Publisher<Guild>, stringToParse: String): Mono<Snowflake> {
        val id = parseRoleId(guild, stringToParse)
        return id.map(Snowflake::of)
    }

    /**
     * Returns the token of the webhook and the ID.
     * @param webhookUrl the url to parse the token and ID from
     * @return the token of the webhook and the ID in a [Pair]
     */
    fun parseWebhookUrl(webhookUrl: String): Pair<Snowflake, String> {
        val pattern = Pattern.compile("https://discordapp\\.com/api/webhooks/(\\d+)/(.+)")
        val matcher = pattern.matcher(webhookUrl)
        return if (!matcher.find()) {
            throw IllegalArgumentException("Invalid webhook URL!")
        } else {
            val id = matcher.group(1)
            val token = matcher.group(2)
            Pair.of(Snowflake.of(id), token)
        }
    }

    /**
     * Fixes the password in the MongoDB connection URL to be URL encoded.
     * @param connectionUrl the MongoDB connection URL to fix
     * @return the MongoDB connection URL with the password URL encoded
     */
    fun fixMongoConnectionUrl(connectionUrl: String): String {
        val pattern = Pattern.compile("mongodb(\\+srv)?://(.+):(.+)@(.+)")
        val matcher = pattern.matcher(connectionUrl)
        return if (!matcher.find()) {
            throw IllegalArgumentException("Invalid connection URL!")
        } else {
            val srv = matcher.group(1)
            val username = matcher.group(2)
            val password = URLEncoder.encode(matcher.group(3), StandardCharsets.UTF_8)
            val serverUrl = matcher.group(4)
            "mongodb$srv://$username:$password@$serverUrl"
        }
    }
}