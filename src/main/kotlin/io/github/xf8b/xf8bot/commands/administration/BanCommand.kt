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

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.PermissionUtil.isMemberHigher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toMono

class BanCommand : AbstractCommand(
    name = "\${prefix}ban",
    description = "Bans the specified member with the specified reason, or `No ban reason was provided` if there was none.",
    commandType = CommandType.ADMINISTRATION,
    minimumAmountOfArgs = 1,
    flags = (MEMBER and REASON).toImmutableList(),
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
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val reason = event.getValueOfFlag(REASON).orElse("No ban reason was provided.")
        return ParsingUtil.parseUserId(event.guild, event.getValueOfFlag(MEMBER).get())
            .map { it.toSnowflake() }
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() //yes i know, very hacky
                .cast())
            .flatMap { userId: Snowflake ->
                event.guild.flatMap { guild ->
                    guild.bans.filter { it.user.id == userId }.singleOrEmpty()
                }.flatMap { event.channel.flatMap { it.createMessage("The user is already banned!") } }
                    .switchIfEmpty(event.guild.flatMap { guild ->
                        guild.getMemberById(userId)
                            .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10007)) {
                                event.channel
                                    .flatMap { it.createMessage("The member is not in the guild!") }
                                    .then() //yes i know, very hacky
                                    .cast()
                            } //unknown member
                            .filterWhen { member ->
                                (member == event.member.get()).toMono()
                                    .filter { !it }
                                    .switchIfEmpty(event.channel.flatMap {
                                        it.createMessage("You cannot ban yourself!")
                                    }.thenReturn(false))
                            }
                            .filterWhen { member ->
                                event.client.self.filterWhen { selfMember: User ->
                                    (selfMember == member).toMono()
                                        .filter { !it }
                                }.map { true }.switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("You cannot ban xf8bot!")
                                }.thenReturn(false))
                            }
                            .filterWhen { member ->
                                guild.selfMember.flatMap { member.isHigher(it) }
                                    .filter { !it }
                                    .switchIfEmpty(event.channel.flatMap {
                                        it.createMessage("Cannot ban member because the member is higher than me!")
                                    }.thenReturn(false))
                            }
                            .filterWhen { member ->
                                isMemberHigher(event.xf8bot, guild, event.member.get(), member)
                                    .filter { !it }
                                    .switchIfEmpty(event.channel.flatMap {
                                        it.createMessage("Cannot ban member because the member is equal to or higher than you!")
                                    }.thenReturn(false))
                            }
                            .flatMap { member ->
                                val username = member.displayName
                                member.privateChannel
                                    .filter { member.isNotBot }
                                    .flatMap sendDM@{ privateChannel ->
                                        privateChannel.createEmbed { embedCreateSpec ->
                                            embedCreateSpec.setTitle("You were banned!")
                                                .setFooter(
                                                    "Banned by: " + event.member.get().tagWithDisplayName,
                                                    event.member.get().avatarUrl
                                                )
                                                .addField("Server", guild.name, false)
                                                .addField("Reason", reason, false)
                                                .setTimestampToNow()
                                                .setColor(Color.RED)
                                        }
                                    }
                                    .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(50007)) { Mono.empty() } //cannot send messages to user
                                    .then(member.ban { it.setDeleteMessageDays(0).reason = reason })
                                    .then(event.channel.flatMap { it.createMessage("Successfully banned $username!") })
                            }
                    })
            }
            .then()
    }
}