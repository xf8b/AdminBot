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

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import discord4j.rest.util.Color
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.data.Warn
import io.github.xf8b.xf8bot.database.actions.add.AddWarningAction
import io.github.xf8b.xf8bot.util.ExceptionPredicates.isClientExceptionWithCode
import io.github.xf8b.xf8bot.util.InputParsing.parseUserId
import io.github.xf8b.xf8bot.util.PermissionUtil.isMemberHigherOrEqual
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.tagWithDisplayName
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toMono

class WarnCommand : AbstractCommand(
    name = "\${prefix}warn",
    description = "Warns the specified member with the specified reason, or `No warn reason was provided` if there was none.",
    commandType = CommandType.ADMINISTRATION,
    flags = ImmutableList.of(MEMBER, REASON),
    administratorLevelRequired = 1
) {
    companion object {
        private val MEMBER = StringFlag(
            shortName = "m",
            longName = "member"
        )
        private val REASON = StringFlag(
            shortName = "r",
            longName = "reason",
            validityPredicate = { it != "all" },
            errorMessageFunction = {
                if (it == "all") {
                    "Sorry, but this warn reason is reserved."
                } else {
                    Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            },
            required = false
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val memberWhoWarnedId = event.member.orElseThrow().id
        val reason = event.getValueOfFlag(REASON).orElse("No warn reason was provided.")

        return parseUserId(event.guild, event.getValueOfFlag(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap {
                event.guild.flatMap { guild ->
                    guild.getMemberById(it)
                        .onErrorResume(isClientExceptionWithCode(10007)) {
                            event.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then() // yes i know, very hacky
                                .cast()
                        } // unknown member
                        .filterWhen { member ->
                            isMemberHigherOrEqual(
                                event.xf8bot,
                                guild,
                                firstMember = event.member.get(),
                                secondMember = member
                            ).filter { it }
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("Cannot warn member because the member is higher than or equal to you!")
                                }.thenReturn(false))
                        }
                        .flatMap { member ->
                            val privateChannelMono: Mono<*> = member.privateChannel
                                .filterWhen {
                                    if (member.isBot) {
                                        false.toMono()
                                    } else {
                                        event.client.self.map { self ->
                                            member != self
                                        }
                                    }
                                }
                                .flatMap {
                                    it.createEmbedDsl {
                                        title("You were warned!")

                                        field("Server", guild.name, false)
                                        field("Reason", reason, false)

                                        footer(
                                            "Warned by: ${event.member.get().tagWithDisplayName}",
                                            event.member.get().avatarUrl
                                        )
                                        timestamp()
                                        color(Color.RED)
                                    }
                                }
                                .onErrorResume(isClientExceptionWithCode(50007)) { Mono.empty() } // cannot send to user
                            event.xf8bot
                                .botDatabase
                                .execute(AddWarningAction(Warn(guild.id, member.id, memberWhoWarnedId, reason)))
                                .toMono()
                                .then(privateChannelMono)
                                .then(event.channel.flatMap {
                                    it.createMessage("Successfully warned ${member.displayName}.")
                                }).then()
                        }.then()
                }
            }
    }
}