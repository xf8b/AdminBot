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
import discord4j.core.`object`.entity.User
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.PermissionUtil.isMemberHigherOrEqual
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.extra.bool.not

class KickCommand : AbstractCommand(
    name = "\${prefix}kick",
    description = "Kicks the specified member with the reason provided, or `No kick reason was provided` if there was none.",
    commandType = CommandType.ADMINISTRATION,
    flags = ImmutableList.of(MEMBER, REASON),
    botRequiredPermissions = Permission.KICK_MEMBERS.toSingletonPermissionSet(),
    administratorLevelRequired = 2
) {
    companion object {
        private val MEMBER = StringFlag(
            shortName = "m",
            longName = "member"
        )
        private val REASON = StringFlag(
            shortName = "r",
            longName = "reason",
            required = false
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val reason = event.getValueOfFlag(REASON).orElse("No kick reason was provided.")
        return InputParsing.parseUserId(event.guild, event.getValueOfFlag(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap { userId ->
                event.guild.flatMap { guild ->
                    guild.getMemberById(userId)
                        .onErrorResume(Checks.isClientExceptionWithCode(10007)) {
                            event.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then() // yes i know, very hacky
                                .cast()
                        } // unknown member
                        .filterWhen { member ->
                            (member == event.member.get()).toMono()
                                .filter { !it }
                                .not()
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("You cannot kick yourself!")
                                }.thenReturn(false))
                        }
                        .filterWhen { member ->
                            event.client.self
                                .filterWhen { selfMember: User ->
                                    (selfMember == member).toMono()
                                        .filter { !it }
                                        .not()
                                }
                                .map { true }
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("You cannot kick xf8bot!")
                                }.thenReturn(false))
                        }
                        .filterWhen { member ->
                            guild.selfMember.flatMap { member.isHigher(it) }
                                .filter { !it }
                                .not()
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("Cannot kick member because the member is higher than me!")
                                }.thenReturn(false))
                        }
                        .filterWhen { member ->
                            isMemberHigherOrEqual(event.xf8bot, guild, member, event.member.get())
                                .filter { !it }
                                .not()
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("Cannot kick member because the member is equal to or higher than you!")
                                }.thenReturn(false))
                        }
                        .flatMap { member ->
                            member.privateChannel
                                .filter { member.isNotBot }
                                .flatMap sendDM@{ privateChannel ->
                                    privateChannel.createEmbedDsl {
                                        title("You were kicked!")

                                        field("Server", guild.name, false)
                                        field("Reason", reason, false)

                                        footer(
                                            "Kicked by: ${event.member.get().tagWithDisplayName}",
                                            event.member.get().avatarUrl
                                        )
                                        timestamp()
                                        color(Color.RED)
                                    }
                                }
                                .onErrorResume(Checks.isClientExceptionWithCode(50007)) {
                                    Mono.empty()
                                } // cannot send messages to user
                                .then(member.kick(reason))
                                .then(event.channel.flatMap {
                                    it.createMessage("Successfully kicked ${member.displayName}!")
                                })
                        }
                }
            }.then()
    }
}