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

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.entity.Member
import discord4j.core.util.OrderUtil
import discord4j.rest.util.Permission
import io.github.xf8b.utils.exceptions.UnexpectedException
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class MemberInfoCommand : AbstractCommand(
    name = "\${prefix}memberinfo",
    description = "Shows information about the member.",
    commandType = CommandType.INFO,
    aliases = ImmutableList.of("\${prefix}userinfo"),
    arguments = ImmutableList.of(MEMBER),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    companion object {
        private val MEMBER: StringArgument = StringArgument(
            name = "member",
            index = Range.atLeast(1),
            required = false
        )
        private val FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(Locale.US)
            .withZone(ZoneOffset.UTC)
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> =
        InputParsing.parseUserId(
            event.guild,
            event.getValueOfArgument(MEMBER).orElse(
                event.author
                    .orElseThrow(::UnexpectedException)
                    .id
                    .asString()
            )
        ).map(Long::toSnowflake)
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap { userId ->
                event.guild
                    .flatMap { it.getMemberById(userId) }
                    .onErrorResume(Checks.isClientExceptionWithCode(10007)) {
                        event.channel
                            .flatMap { it.createMessage("The member is not in the guild!") }
                            .then() // yes i know, very hacky
                            .cast()
                    } // unknown member
                    .flatMap { member: Member ->
                        val displayName = member.displayName
                        val avatarUrl = member.avatarUrl
                        val memberJoinDiscordTime = member.id.timestamp
                        val memberJoinServerTime = member.joinTime
                        val id = member.id.asString()
                        /*
                         * index   type
                         * t1      color
                         * t2      status
                         * t3      activity
                         * t4      is owner
                         * t5      roles
                         * t6      administrator level
                         */
                        val otherInfo = Mono.zip(
                            member.color,
                            member.presence.map { it.status },
                            member.presence
                                .map { it.activity }
                                .flatMap { it.toMono() }
                                .map { it.name }
                                .defaultIfEmpty("No activity."),
                            event.guild.map { member.id == it.ownerId },
                            OrderUtil.orderRoles(member.roles)
                                .map { it.mention }
                                .collectList()
                                .map { it.joinToString(separator = " ") }
                                .defaultIfEmpty("No roles"),
                            event.guild.flatMap { PermissionUtil.getAdministratorLevel(event.xf8bot, it, member) }
                        )
                        otherInfo.flatMap { info ->
                            event.channel.flatMap {
                                it.createEmbedDsl {
                                    title("Info For `${member.tagWithDisplayName}`")
                                    author(name = displayName, iconUrl = avatarUrl)

                                    field("Username", member.username, inline = true)
                                    field("Nickname", member.nickname.orElse("No nickname"), inline = true)
                                    field("Discriminator", member.discriminator.toString(), inline = true)

                                    field("ID", id, inline = false)

                                    field("Administrator Level", info.t6.toString(), inline = false)

                                    field("Is Owner", info.t4.toString(), inline = true)
                                    field("Is Bot", member.isBot.toString(), inline = true)

                                    field("Roles", info.t5, inline = false)

                                    field(
                                        "Joined Discord (UTC)",
                                        FORMATTER.format(memberJoinDiscordTime).dropLast(2),
                                        inline = true
                                    )
                                    field(
                                        "Joined Server (UTC)",
                                        FORMATTER.format(memberJoinServerTime).dropLast(2),
                                        inline = true
                                    )

                                    field(
                                        "Role Color RGB",
                                        "Red: ${info.t1.red}, Green: ${info.t1.green}, Blue: ${info.t1.blue}",
                                        inline = true
                                    )

                                    field("Avatar URL", avatarUrl, inline = true)

                                    timestamp()
                                }
                            }
                        }
                    }
            }.then()
}
