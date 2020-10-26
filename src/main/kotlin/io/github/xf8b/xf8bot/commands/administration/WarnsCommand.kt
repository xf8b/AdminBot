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

import com.google.common.collect.Range
import com.mongodb.client.model.Filters
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.data.Warn
import io.github.xf8b.xf8bot.util.ExceptionPredicates
import io.github.xf8b.xf8bot.util.ParsingUtil.parseUserId
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import io.github.xf8b.xf8bot.util.toSnowflake
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux

class WarnsCommand : AbstractCommand(
    name = "\${prefix}warns",
    description = "Gets the warns for the specified member.",
    commandType = CommandType.ADMINISTRATION,
    minimumAmountOfArgs = 1,
    arguments = MEMBER.toSingletonImmutableList(),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet(),
    administratorLevelRequired = 1
) {
    companion object {
        private val MEMBER = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("member")
            .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val mongoCollection = event.xf8bot
            .mongoDatabase
            .getCollection("warns")
        return parseUserId(event.guild, event.getValueOfArgument(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() //yes i know, very hacky
                .cast())
            .flatMap { userId ->
                event.guild.flatMap { guild ->
                    guild.getMemberById(userId)
                        .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10007)) {
                            event.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then() //yes i know, very hacky
                                .cast()
                        } //unknown member
                }
            }
            .flatMap { member ->
                event.guild.flatMap { guild ->
                    val warnsMono = mongoCollection.find(
                        Filters.and(
                            Filters.eq("guildId", event.guildId.get().asLong()),
                            Filters.eq("userId", member.id.asLong())
                        )
                    ).toFlux().map { document ->
                        Warn(
                            document.getLong("memberWhoWarnedId").toSnowflake(),
                            document.getString("reason"),
                            document.getString("warnId")
                        )
                    }.collectList().flatMap { warns ->
                        warns.toFlux().flatMap {
                            mono {
                                guild.getMemberById(it.memberWhoWarnedId)
                                    .map { it.nicknameMention }
                                    .block()!! to it.reason and it.warnId
                            }
                        }.collectList()
                    }

                    warnsMono.flatMap sendWarns@{ warns ->
                        if (warns.isNotEmpty()) {
                            event.channel.flatMap {
                                it.createEmbed { embedCreateSpec: EmbedCreateSpec ->
                                    embedCreateSpec.setTitle("Warnings For `${member.username}`")
                                        .setColor(Color.BLUE)

                                    warns.forEach { (memberWhoWarnedMention, reason, warnId) ->
                                        embedCreateSpec.addField(
                                            "`$reason`",
                                            """
                                                    Warn ID: $warnId
                                                    Member Who Warned:
                                                    $memberWhoWarnedMention
                                                    """.trimIndent(),
                                            true
                                        )
                                    }
                                }
                            }
                        } else {
                            event.channel.flatMap { it.createMessage("The member has no warns.") }
                        }
                    }
                }
            }.then()
    }
}