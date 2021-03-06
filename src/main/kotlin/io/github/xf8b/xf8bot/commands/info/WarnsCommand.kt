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

package io.github.xf8b.xf8bot.commands.info

import com.google.common.collect.Range
import discord4j.core.`object`.entity.Member
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.InputParser
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.data.Warn
import io.github.xf8b.xf8bot.database.actions.find.FindWarnsAction
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.extensions.JAVA_WRAPPER_TYPE
import io.github.xf8b.xf8bot.util.extensions.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.extensions.toSingletonPermissionSet
import io.github.xf8b.xf8bot.util.extensions.toSnowflake
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import java.util.*

class WarnsCommand : Command(
    name = "\${prefix}warns",
    description = "Gets the warns for the specified member.",
    commandType = CommandType.INFO,
    arguments = MEMBER.toSingletonImmutableList(),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet(),
    administratorLevelRequired = 1
) {
    companion object {
        private val MEMBER = StringArgument(
            name = "member",
            index = Range.atLeast(0),
            required = false
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> =
        InputParser.parseUserId(event.guild, event[MEMBER] ?: event.author.get().id.asString())
            .switchIfEmpty(
                event.channel
                    .flatMap { it.createMessage("No member found! This may be caused by 2+ people having the same username or nickname.") }
                    .then() // yes i know, very hacky
                    .cast())
            .flatMap { userId ->
                event.guild.flatMap { guild ->
                    guild.getMemberById(userId).onErrorResume(Checks.isClientExceptionWithCode(10007)) {
                        event.channel
                            .flatMap { it.createMessage("The member is not in the guild!") }
                            .then() // yes i know, very hacky
                            .cast()
                    } // unknown member
                }
            }
            .flatMap { member ->
                val warnsMono = event.xf8bot.botDatabase
                    .execute(FindWarnsAction(event.guildId.get(), member.id))
                    .flatMap { result ->
                        result.map { row, _ ->
                            Warn(
                                (row["guildId", Long.JAVA_WRAPPER_TYPE]!! as Long).toSnowflake(),
                                (row["memberId", Long.JAVA_WRAPPER_TYPE]!! as Long).toSnowflake(),
                                (row["warnerId", Long.JAVA_WRAPPER_TYPE]!! as Long).toSnowflake(),
                                row["reason", String::class.java]!!,
                                row["warnId", UUID::class.java]!!
                            )
                        }
                    }
                    .flatMap { warn ->
                        mono {
                            warn.getWarner(event.client)
                                .map(Member::getNicknameMention)
                                .block()!! to warn.reason and warn.warnId
                        }
                    }
                    .collectList()

                warnsMono.flatMap sendWarns@{ warns ->
                    if (warns.isNotEmpty()) {
                        event.channel.flatMap {
                            it.createEmbedDsl {
                                title("Warnings For `${member.username}`")

                                warns.forEach { (warner, reason, warnId) ->
                                    field(
                                        "`$reason`",
                                        """
                                                Warn ID: $warnId
                                                Member Who Warned/Warner: $warner
                                                """.trimIndent(),
                                        inline = true
                                    )
                                }

                                color(Color.BLUE)
                            }
                        }
                    } else {
                        event.channel.flatMap { it.createMessage("The member has no warns.") }
                    }
                }
            }.then()
}