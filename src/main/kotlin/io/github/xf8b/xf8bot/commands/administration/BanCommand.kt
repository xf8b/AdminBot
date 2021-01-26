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

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import discord4j.common.util.Snowflake
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.utils.exceptions.UnexpectedException
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.InputParser
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.extensions.isNotBot
import io.github.xf8b.xf8bot.util.extensions.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast

class BanCommand : Command(
    name = "\${prefix}ban",
    description = "Bans the specified member with the specified reason, or `No ban reason was provided` if there was none.",
    commandType = CommandType.ADMINISTRATION,
    flags = ImmutableList.of(MEMBER, REASON, MESSAGE_DELETE_DAYS),
    botRequiredPermissions = Permission.BAN_MEMBERS.toSingletonPermissionSet(),
    administratorLevelRequired = 3
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
        private val MESSAGE_DELETE_DAYS = IntegerFlag(
            shortName = "d",
            longName = "messagesDeleteDays",
            validityPredicate = {
                try {
                    val value = it.toInt()
                    value in 0..7
                } catch (exception: NumberFormatException) {
                    false
                }
            },
            errorMessageFunction = { value: String ->
                try {
                    val level = value.toInt()
                    when {
                        level > 7 -> "The maximum amount of message delete days is 7!"
                        level < 0 -> "The minimum amount of message delete days is 0!"
                        else -> throw UnexpectedException()
                    }
                } catch (exception: NumberFormatException) {
                    Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            },
            required = false
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val reason = event[REASON] ?: "No ban reason was provided."

        return InputParser.parseUserId(event.guild, event[MEMBER]!!)
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found! This may be caused by 2+ people having the same username or nickname.") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap { userId: Snowflake ->
                event.guild.flatMap { guild ->
                    guild.bans.filter { it.user.id == userId }.singleOrEmpty()
                }.flatMap { event.channel.flatMap { it.createMessage("The user is already banned!") } }
                    .switchIfEmpty(event.guild.flatMap { guild ->
                        guild.getMemberById(userId)
                            .onErrorResume(Checks.isClientExceptionWithCode(10007)) {
                                event.channel
                                    .flatMap { it.createMessage("The member is not in the guild!") }
                                    .then() // yes i know, very hacky
                                    .cast()
                            } // unknown member
                            .filterWhen { member ->
                                Checks.canMemberUseAdministrativeActionsOn(event, member, action = "ban")
                            }
                            .filterWhen { member ->
                                Checks.canBotInteractWith(guild, member, event.channel, action = "ban")
                            }
                            .filterWhen { member ->
                                Checks.isMemberHighEnough(event, member, action = "ban")
                            }
                            .flatMap { member ->
                                member.privateChannel
                                    .filter { member.isNotBot }
                                    .flatMap sendDM@{ privateChannel ->
                                        privateChannel.createEmbedDsl {
                                            title("You were banned!")

                                            field("Server", guild.name, inline = false)
                                            field("Reason", reason, inline = false)

                                            footer(
                                                "Banned by: ${event.member.get().tag}",
                                                event.member.get().avatarUrl
                                            )
                                            timestamp()
                                            color(Color.RED)
                                        }
                                    }
                                    .onErrorResume(Checks.isClientExceptionWithCode(50007)) {
                                        Mono.empty()
                                    } // cannot send messages to user
                                    .then(member.ban {
                                        it.setDeleteMessageDays(event[MESSAGE_DELETE_DAYS] ?: 0)
                                        it.reason = reason
                                    })
                                    .then(event.channel.flatMap {
                                        it.createMessage("Successfully banned ${member.displayName}!")
                                    })
                            }
                    })
            }.then()
    }
}