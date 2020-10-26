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

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import com.mongodb.client.model.Filters
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.ExceptionPredicates.isClientExceptionWithCode
import io.github.xf8b.xf8bot.util.ParsingUtil.parseUserId
import io.github.xf8b.xf8bot.util.PermissionUtil.isMemberHigher
import io.github.xf8b.xf8bot.util.tagWithDisplayName
import io.github.xf8b.xf8bot.util.toSnowflake
import org.bson.Document
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.util.*

class WarnCommand : AbstractCommand(
    name = "\${prefix}warn",
    description = "Warns the specified member with the specified reason, or `No warn reason was provided` if there was none.",
    commandType = CommandType.ADMINISTRATION,
    minimumAmountOfArgs = 1,
    flags = ImmutableList.of(MEMBER, REASON),
    administratorLevelRequired = 1
) {
    companion object {
        private val MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build()
        private val REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setValidityPredicate { it != "all" }
            .setInvalidValueErrorMessageFunction {
                if (it == "all") {
                    "Sorry, but this warn reason is reserved."
                } else {
                    Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            }
            .setNotRequired()
            .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val memberWhoWarnedId = event.member.orElseThrow().id
        val reason = event.getValueOfFlag(REASON).orElse("No warn reason was provided.")
        if (reason == "all") {
            return event.channel.flatMap {
                it.createMessage("Sorry, but this warn reason is reserved.")
            }.then()
        }
        val mongoCollection = event.xf8bot
            .mongoDatabase
            .getCollection("warns")

        return parseUserId(event.guild, event.getValueOfFlag(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() //yes i know, very hacky
                .cast())
            .flatMap {
                event.guild.flatMap { guild ->
                    guild.getMemberById(it)
                        .onErrorResume(isClientExceptionWithCode(10007)) {
                            event.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then() //yes i know, very hacky
                                .cast()
                        } //unknown member
                        .filterWhen { member ->
                            isMemberHigher(event.xf8bot, guild, event.member.get(), member)
                                .filter { !it }
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("Cannot warn member because the member is equal to or higher than you!")
                                }.thenReturn(false))
                        }
                        .flatMap { member ->
                            val privateChannelMono: Mono<*> = member.privateChannel
                                .filterWhen {
                                    if (member.isBot) {
                                        false.toMono()
                                    } else {
                                        event.client.self.map {
                                            member != it
                                        }
                                    }
                                }
                                .flatMap {
                                    it.createEmbed { embedCreateSpec: EmbedCreateSpec ->
                                        embedCreateSpec.setTitle("You were warned!")
                                            .setFooter(
                                                "Warned by: " + event.member.get().tagWithDisplayName,
                                                event.member.get().avatarUrl
                                            )
                                            .addField("Server", guild.name, false)
                                            .addField("Reason", reason, false)
                                            .setTimestamp(Instant.now())
                                            .setColor(Color.RED)
                                    }
                                }
                                .onErrorResume(isClientExceptionWithCode(50007)) { Mono.empty() } //cannot send to user
                            Flux.from(mongoCollection.find(Filters.eq("userId", member.id.asLong())))
                                .flatMap {
                                    mongoCollection.insertOne(
                                        Document()
                                            .append("guildId", guild.id.asLong())
                                            .append("userId", member.id.asLong())
                                            .append("memberWhoWarnedId", memberWhoWarnedId.asLong())
                                            .append("warnId", UUID.randomUUID().toString())
                                            .append("reason", reason)
                                    ).toMono()
                                }
                                .switchIfEmpty(
                                    mongoCollection.insertOne(
                                        Document()
                                            .append("guildId", guild.id.asLong())
                                            .append("userId", member.id.asLong())
                                            .append("memberWhoWarnedId", memberWhoWarnedId.asLong())
                                            .append("warnId", UUID.randomUUID().toString())
                                            .append("reason", reason)
                                    ).toMono()
                                )
                                .then(privateChannelMono)
                                .then(event.channel.flatMap {
                                    it.createMessage("Successfully warned " + member.displayName + ".")
                                }).then()
                        }.then()
                }
            }
    }
}