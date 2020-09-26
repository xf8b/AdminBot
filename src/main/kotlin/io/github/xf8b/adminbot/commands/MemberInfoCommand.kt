/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.commands

import com.google.common.collect.Range
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Permission
import io.github.xf8b.adminbot.api.commands.AbstractCommand
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument
import io.github.xf8b.adminbot.util.ClientExceptionUtil
import io.github.xf8b.adminbot.util.ParsingUtil
import io.github.xf8b.adminbot.util.getTagWithDisplayName
import org.apache.commons.lang3.StringUtils
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class MemberInfoCommand : AbstractCommand(builder()
        .setName("\${prefix}memberinfo")
        .setDescription("Shows information about the member.")
        .setCommandType(CommandType.OTHER)
        .addAlias("\${prefix}userinfo")
        .setMinimumAmountOfArgs(1)
        .addArgument(MEMBER)
        .setBotRequiredPermissions(Permission.EMBED_LINKS)
) {
    companion object {
        private val MEMBER: StringArgument = StringArgument.builder()
                .setIndex(Range.atLeast(1))
                .setName("member")
                .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val channel: MessageChannel = event.channel.block()!!
        val guild = event.guild.block()!!
        val userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfArgument(MEMBER).get())

        if (userId.isEmpty) {
            return channel.createMessage("The member does not exist!").then()
        }

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.UK)
                .withZone(ZoneOffset.UTC)
        return guild.getMemberById(userId.get())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007)) {
                    Mono.fromRunnable {
                        channel.createMessage("The member is not in the guild!").block()
                    }
                } //unknown member
                .flatMap { member: Member ->
                    val displayName = member.displayName
                    val avatarUrl = member.avatarUrl
                    val memberJoinDiscordTime = member.id.timestamp
                    val memberJoinServerTime = member.joinTime
                    val id = member.id.asString()
                    val color = member.color.block()!!
                    val status = member.presence
                            .map { it.status }
                            .block()!!
                    val activity = member.presence
                            .map { it.activity }
                            .block()!!
                            .map { it.name }
                            .orElse("No activity.")
                    val isOwner = member.id == guild.ownerId
                    val roleMentions = member.roles.map { it.mention }
                            .map { " " }
                            .collectList()
                            .map { it.joinToString(separator = " ") }
                            .block()!!
                    channel.createEmbed { embedCreateSpec: EmbedCreateSpec ->
                        embedCreateSpec.setTitle("Info For Member `" + member.getTagWithDisplayName() + "`")
                                .setAuthor(displayName, null, avatarUrl)
                                .addField("Is Owner:", isOwner.toString(), true)
                                .addField("Is Bot:", member.isBot.toString(), true)
                                .addField("Roles:", roleMentions, true)
                                .addField("Status:", StringUtils.capitalize(status.name.toLowerCase().replace("_", " ")), true)
                                .addField("Activity:", activity, true)
                                .addField("Joined Discord:", formatter.format(memberJoinDiscordTime), true)
                                .addField("Joined Server:", formatter.format(memberJoinServerTime), true)
                                .addField("ID:", id, true)
                                .addField(
                                        "Role Color RGB:",
                                        "Red: " + color.red + ", Green: " + color.green + ", Blue: " + color.blue,
                                        true
                                )
                                .addField("Avatar URL:", avatarUrl, true)
                                .setTimestamp(Instant.now())
                    }
                }.then()
    }
}