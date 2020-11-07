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
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.data.Warn
import io.github.xf8b.xf8bot.database.actions.FindAllMatchingAction
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
        private val MEMBER = StringArgument(
            name = "member",
            index = Range.atLeast(1)
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> =
        parseUserId(context.guild, context.getValueOfArgument(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(context.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap { userId ->
                context.guild.flatMap { guild ->
                    guild.getMemberById(userId)
                        .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10007)) {
                            context.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then() // yes i know, very hacky
                                .cast()
                        } // unknown member
                }
            }
            .flatMap { member ->
                context.guild.flatMap {
                    val warnsMono = context.xf8bot.botMongoDatabase.execute(
                        FindAllMatchingAction(
                            collectionName = "warns",
                            Filters.and(
                                Filters.eq("guildId", context.guildId.get().asLong()),
                                Filters.eq("userId", member.id.asLong())
                            )
                        )
                    ).toFlux().map { document ->
                        Warn(
                            document.getLong("guildId").toSnowflake(),
                            document.getLong("userId").toSnowflake(),
                            document.getLong("memberWhoWarnedId").toSnowflake(),
                            document.getString("reason"),
                            document.getString("warnId")
                        )
                    }.collectList().flatMap { warns ->
                        warns.toFlux().flatMap {
                            mono {
                                it.getMemberWhoWarnedAsMember(context.client)
                                    .map { it.nicknameMention }
                                    .block()!! to it.reason and it.warnId
                            }
                        }.collectList()
                    }

                    warnsMono.flatMap sendWarns@{ warns ->
                        if (warns.isNotEmpty()) {
                            context.channel.flatMap {
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
                            context.channel.flatMap { it.createMessage("The member has no warns.") }
                        }
                    }
                }
            }.then()
}