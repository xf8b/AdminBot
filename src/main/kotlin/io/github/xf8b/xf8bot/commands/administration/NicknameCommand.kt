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
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.Checks
import io.github.xf8b.xf8bot.util.InputParsing
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast

class NicknameCommand : Command(
    name = "\${prefix}nickname",
    description = "Sets the nickname of the specified member, or resets it if none was provided.",
    commandType = CommandType.ADMINISTRATION,
    aliases = "\${prefix}nick".toSingletonImmutableList(),
    flags = ImmutableList.of(MEMBER, NICKNAME),
    botRequiredPermissions = Permission.MANAGE_NICKNAMES.toSingletonPermissionSet(),
    administratorLevelRequired = 1
) {
    companion object {
        private val MEMBER = StringFlag(
            shortName = "m",
            longName = "member"
        )
        private val NICKNAME = StringFlag(
            shortName = "n",
            longName = "nickname",
            required = false
        )
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val nickname = event[NICKNAME]
        val reset = nickname?.isBlank() ?: true

        return InputParsing.parseUserId(event.guild, event[MEMBER]!!)
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found! This may be caused by 2+ people having the same username or nickname.") }
                .then() // yes i know, very hacky
                .cast())
            .map(Snowflake::of)
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
                            Checks.canBotInteractWith(guild, member, event.channel, action = "nickname")
                        }
                        .filterWhen { member -> guild.selfMember.map { it.id != member.id } }
                        .flatMap { member ->
                            member.edit { it.setNickname(nickname) }.flatMap {
                                event.channel.flatMap {
                                    it.createMessage("Successfully ${if (reset) "re" else ""}set nickname of ${member.displayName}!")
                                }.then()
                            }
                        }
                        .switchIfEmpty(guild.changeSelfNickname(nickname).then())
                }
            }
    }
}