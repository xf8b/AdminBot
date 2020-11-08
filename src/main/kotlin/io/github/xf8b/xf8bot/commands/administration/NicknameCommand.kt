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
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.spec.GuildMemberEditSpec
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.ExceptionPredicates.isClientExceptionWithCode
import io.github.xf8b.xf8bot.util.ParsingUtil.parseUserId
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast

class NicknameCommand : AbstractCommand(
    name = "\${prefix}nickname",
    description = "Sets the nickname of the specified member, or resets it if none was provided.",
    commandType = CommandType.ADMINISTRATION,
    aliases = "\${prefix}nick".toSingletonImmutableList(),
    minimumAmountOfArgs = 1,
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

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> =
        parseUserId(context.guild, context.getValueOfFlag(MEMBER).get())
            .switchIfEmpty(context.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .map(Snowflake::of)
            .flatMap { userId: Snowflake ->
                context.guild
                    .flatMap { guild: Guild ->
                        guild.getMemberById(userId).onErrorResume(isClientExceptionWithCode(10007), {
                            context.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then()
                                .cast()
                        })
                    } // unknown member
                    .filterWhen { member: Member ->
                        context.guild.flatMap { guild: Guild ->
                            guild.selfMember
                                .flatMap { self -> member.isHigher(self) }
                                .filter { !it }
                                .switchIfEmpty(context.channel
                                    .flatMap { it.createMessage("Cannot nickname member because the member is higher than me!") }
                                    .thenReturn(false))
                        }
                    }
                    .flatMap { member: Member ->
                        val nickname = context.getValueOfFlag(NICKNAME).orElse("")
                        val reset = nickname.isBlank()
                        Mono.just(member)
                            .filter { it.id != context.client.selfId }
                            .flatMap { it.edit { spec: GuildMemberEditSpec -> spec.setNickname(nickname) } }
                            .switchIfEmpty(context.guild
                                .flatMap { it.changeSelfNickname(nickname) }
                                .thenReturn(member))
                            // FIXME not working
                            .flatMap {
                                context.channel.flatMap {
                                    it.createMessage("Successfully ${if (reset) "re" else ""}set nickname of ${member.displayName}!")
                                }
                            }
                            .then()
                    }
            }
}