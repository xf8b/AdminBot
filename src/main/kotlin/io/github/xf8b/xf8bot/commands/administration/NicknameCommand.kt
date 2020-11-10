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
import discord4j.rest.util.Permission
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.util.ExceptionPredicates
import io.github.xf8b.xf8bot.util.ParsingUtil.parseUserId
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.util.retry.Retry
import java.time.Duration

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

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val nickname = context.getValueOfFlag(NICKNAME).toValueOrNull()
        val reset = nickname?.isBlank() ?: true

        return parseUserId(context.guild, context.getValueOfFlag(MEMBER).get())
            .switchIfEmpty(context.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .map(Snowflake::of)
            .flatMap { userId ->
                context.guild.flatMap { guild ->
                    guild.getMemberById(userId)
                        .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10007)) {
                            context.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then() // yes i know, very hacky
                                .cast()
                        } // unknown member
                        .filterWhen { member ->
                            guild.selfMember.flatMap { member.isHigher(it) }
                                .filter { !it }
                                .switchIfEmpty(context.channel.flatMap {
                                    it.createMessage("Cannot nickname member because the member is higher than me!")
                                }.thenReturn(false))
                        }
                        //.filterWhen { member -> guild.selfMember.map { it.id != member.id } }
                        .flatMap { member ->
                            //guild.selfMember.flatMap { self ->
                            //if (self.id == member.id) {
                            //guild.changeSelfNickname(nickname)
                            //} else {
                            member.edit { spec -> spec.setNickname(nickname) }
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1L)))
                                // }
                                /*}*/
                                .flatMap {
                                    context.channel.flatMap {
                                        it.createMessage("Successfully ${if (reset) "re" else ""}set nickname of ${member.displayName}!")
                                    }
                                }
                        }
                }.then()
            }
        /*
        .flatMap { userId: Snowflake ->
            context.guild
                .flatMap { guild: Guild ->
                    guild.getMemberById(userId)
                        .onErrorResume(isClientExceptionWithCode(10007)) {
                            context.channel
                                .flatMap { it.createMessage("The member is not in the guild!") }
                                .then()
                                .cast()
                        } // unknown member
                        .filterWhen { member: Member ->
                            guild.selfMember
                                .flatMap { self -> member.isHigher(self) }
                                .filter { !it }
                                .switchIfEmpty(context.channel
                                    .flatMap { it.createMessage("Cannot nickname member because the member is higher than me!") }
                                    .thenReturn(false))
                        }
                        .flatMap { member: Member ->
                            val nickname = context.getValueOfFlag(NICKNAME).orElse("")
                            val reset = nickname.isBlank()

                            member.toMono()
                                .filter { it.id != context.client.selfId }
                                .flatMap { it.edit { spec: GuildMemberEditSpec -> spec.setNickname(nickname) } }
                                .switchIfEmpty(context.guild
                                    .flatMap { it.changeSelfNickname(nickname) }
                                    .thenReturn(member))
                                // FIXME not working
                                .then(context.channel.flatMap {
                                    it.createMessage("Successfully ${if (reset) "re" else ""}set nickname of ${member.displayName}!")
                                })
                        }
                }
        }.then()
        */
    }
}