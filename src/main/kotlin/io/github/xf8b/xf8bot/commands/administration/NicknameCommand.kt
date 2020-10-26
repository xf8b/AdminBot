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
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
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
        private val MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build()
        private val NICKNAME = StringFlag.builder()
            .setShortName("n")
            .setLongName("nickname")
            .setNotRequired()
            .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        return parseUserId(event.guild, event.getValueOfFlag(MEMBER).get())
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() //yes i know, very hacky
                .cast())
            .map(Snowflake::of)
            .flatMap { userId: Snowflake ->
                event.guild.flatMap { guild: Guild ->
                    guild.getMemberById(userId).onErrorResume(isClientExceptionWithCode(10007), {
                        event.channel
                            .flatMap { it.createMessage("The member is not in the guild!") }
                            .then()
                            .cast()
                    })
                }
            } //unknown member
            .filterWhen { member: Member ->
                event.guild.flatMap { guild: Guild ->
                    guild.selfMember
                        .flatMap { otherMember: Member -> member.isHigher(otherMember) }
                        .filter { !it }
                        .switchIfEmpty(event.channel
                            .flatMap { it.createMessage("Cannot nickname member because the member is higher than me!") }
                            .thenReturn(false))
                }
            }
            .flatMap { member: Member ->
                val nickname = event.getValueOfFlag(NICKNAME).orElse("")
                val reset = nickname == ""
                Mono.just(member)
                    .filter { it.id != event.client.selfId }
                    .flatMap { it.edit { spec: GuildMemberEditSpec -> spec.setNickname(nickname) } }
                    .switchIfEmpty(event.guild
                        .flatMap { it.changeSelfNickname(nickname) }
                        .thenReturn(member))
                    .flatMap {
                        event.channel.flatMap {
                            it.createMessage("Successfully " + (if (reset) "re" else "") + "set nickname of " + member.displayName + "!")
                        }
                    }
                    .then()
            }
    }
}