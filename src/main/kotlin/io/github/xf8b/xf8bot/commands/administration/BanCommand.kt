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
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.PermissionUtil.isMemberHigherOrEqual
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toMono

class BanCommand : AbstractCommand(
    name = "\${prefix}ban",
    description = "Bans the specified member with the specified reason, or `No ban reason was provided` if there was none.",
    commandType = CommandType.ADMINISTRATION,
    minimumAmountOfArgs = 1,
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
            validityPredicate = IntegerFlag.DEFAULT_VALIDITY_PREDICATE.and {
                val value = (it as String).toInt()
                value in 0..7
            },
            invalidValueErrorMessageFunction = { value: String ->
                try {
                    val level = value.toInt()
                    when {
                        level > 7 -> "The maximum amount of message delete days is 7!"
                        level < 0 -> "The minimum amount of message delete days is 0!"
                        else -> throw ThisShouldNotHaveBeenThrownException()
                    }
                } catch (exception: NumberFormatException) {
                    Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            },
            required = false
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val reason = context.getValueOfFlag(REASON).orElse("No ban reason was provided.")
        return ParsingUtil.parseUserId(context.guild, context.getValueOfFlag(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(context.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap { userId: Snowflake ->
                context.guild.flatMap { guild ->
                    guild.bans.filter { it.user.id == userId }.singleOrEmpty()
                }.flatMap { context.channel.flatMap { it.createMessage("The user is already banned!") } }
                    .switchIfEmpty(context.guild.flatMap { guild ->
                        guild.getMemberById(userId)
                            .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10007)) {
                                context.channel
                                    .flatMap { it.createMessage("The member is not in the guild!") }
                                    .then() // yes i know, very hacky
                                    .cast()
                            } // unknown member
                            .filterWhen { member ->
                                (member == context.member.get()).toMono()
                                    .filter { !it }
                                    .switchIfEmpty(context.channel.flatMap {
                                        it.createMessage("You cannot ban yourself!")
                                    }.thenReturn(false))
                            }
                            .filterWhen { member ->
                                context.client.self.filterWhen { selfMember: User ->
                                    (selfMember == member).toMono()
                                        .filter { !it }
                                }.map { true }.switchIfEmpty(context.channel.flatMap {
                                    it.createMessage("You cannot ban xf8bot!")
                                }.thenReturn(false))
                            }
                            .filterWhen { member ->
                                guild.selfMember.flatMap { member.isHigher(it) }
                                    .filter { !it }
                                    .switchIfEmpty(context.channel.flatMap {
                                        it.createMessage("Cannot ban member because the member is higher than me!")
                                    }.thenReturn(false))
                            }
                            .filterWhen { member ->
                                isMemberHigherOrEqual(context.xf8bot, guild, context.member.get(), member)
                                    .filter { it }
                                    .switchIfEmpty(context.channel.flatMap {
                                        it.createMessage("Cannot ban member because the member is equal to or higher than you!")
                                    }.thenReturn(false))
                            }
                            .flatMap { member ->
                                val username = member.displayName
                                member.privateChannel
                                    .filter { member.isNotBot }
                                    .flatMap sendDM@{ privateChannel ->
                                        privateChannel
                                            .createEmbed { embedCreateSpec ->
                                                embedCreateSpec.setTitle("You were banned!")
                                                    .setFooter(
                                                        "Banned by: ${context.member.get().tagWithDisplayName}",
                                                        context.member.get().avatarUrl
                                                    )
                                                    .addField("Server", guild.name, false)
                                                    .addField("Reason", reason, false)
                                                    .setTimestampToNow()
                                                    .setColor(Color.RED)
                                            }
                                            .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(50007)) { Mono.empty() } // cannot send messages to user
                                    }
                                    .then(member.ban {
                                        it.setDeleteMessageDays(context.getValueOfFlag(MESSAGE_DELETE_DAYS).orElse(0))
                                        it.reason = reason
                                    })
                                    .then(context.channel.flatMap { it.createMessage("Successfully banned $username!") })
                            }
                    })
            }.then()
    }
}