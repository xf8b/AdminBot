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

package io.github.xf8b.xf8bot.commands.info

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import discord4j.core.`object`.entity.Member
import discord4j.core.util.OrderUtil
import discord4j.rest.util.Permission
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.util.*
import org.apache.commons.lang3.StringUtils
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
    minimumAmountOfArgs = 1,
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    companion object {
        private val MEMBER: StringArgument = StringArgument(
            name = "member",
            index = Range.atLeast(1)
        )
    }

    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.UK)
            .withZone(ZoneOffset.UTC)
        return ParsingUtil.parseUserId(context.guild, context.getValueOfArgument(MEMBER).get())
            .map(Long::toSnowflake)
            .switchIfEmpty(context.channel
                .flatMap { it.createMessage("No member found!") }
                .then() // yes i know, very hacky
                .cast())
            .flatMap { userId ->
                context.guild
                    .flatMap { it.getMemberById(userId) }
                    .onErrorResume(ExceptionPredicates.isClientExceptionWithCode(10007)) {
                        context.channel
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
                        val otherInfo = Mono.zip(
                            member.color,
                            member.presence.map { it.status },
                            member.presence.map { it.activity }.map { optional ->
                                optional?.map { it.name }
                                    ?.toValueOrNull()
                                    ?: "No activity."
                            },
                            context.guild.map { member.id == it.ownerId },
                            OrderUtil.orderRoles(member.roles)
                                .map { it.mention }
                                .collectList()
                                .map { it.joinToString(separator = " ") }
                                .defaultIfEmpty("No roles")
                        )
                        otherInfo.flatMap { info ->
                            context.channel.flatMap {
                                it.createEmbed { embedCreateSpec ->
                                    embedCreateSpec.setTitle("Info For Member `${member.tagWithDisplayName}`")
                                        .setAuthor(displayName, null, avatarUrl)
                                        .addField("Is Owner:", info.t4.toString(), true)
                                        .addField("Is Bot:", member.isBot.toString(), true)
                                        .addField("Roles:", info.t5, true)
                                        .addField(
                                            "Status:", StringUtils.capitalize(
                                                info.t2
                                                    .name
                                                    .toLowerCase()
                                                    .replace("_", " ")
                                            ), true
                                        )
                                        .addField("Activity:", info.t3, true)
                                        .addField(
                                            "Joined Discord (UTC):",
                                            formatter.format(memberJoinDiscordTime),
                                            true
                                        )
                                        .addField(
                                            "Joined Server (UTC):",
                                            formatter.format(memberJoinServerTime),
                                            true
                                        )
                                        .addField("ID:", id, true)
                                        .addField(
                                            "Role Color RGB:",
                                            "Red: ${info.t1.red}, Green: ${info.t1.green}, Blue: ${info.t1.blue}",
                                            true
                                        )
                                        .addField("Avatar URL:", avatarUrl, true)
                                        .setTimestampToNow()
                                }
                            }
                        }
                    }
            }.then()
    }
}
