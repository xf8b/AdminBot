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

import com.google.common.collect.ImmutableList
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import io.github.xf8b.adminbot.api.commands.AbstractCommand
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent
import reactor.core.publisher.Mono

class InfoCommand : AbstractCommand(
        name = "\${prefix}information",
        description = "Shows some information about me.",
        commandType = CommandType.OTHER,
        aliases = ImmutableList.of("\${prefix}info"),
        botRequiredPermissions = PermissionSet.of(Permission.EMBED_LINKS)
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val prefix = event.prefix.block()!!
        val totalCommands = event.adminBot.commandRegistry.size
        val username = event.client.self.map { it.username }.block()!!
        val avatarUrl = event.client.self.map { it.avatarUrl }.block()!!
        return event.channel.flatMap {
            it.createEmbed { embedCreateSpec: EmbedCreateSpec ->
                embedCreateSpec.setTitle("Information")
                        .setAuthor(username, null, avatarUrl)
                        .setDescription("AdminBot is a simple bot used for administration (and other things).")
                        .addField("Current Version", event.adminBot.version, true)
                        .addField("Current Prefix", "`$prefix`", true)
                        .addField("Total Amount of Commands", totalCommands.toString(), true)
                        .addField("Documentation", "https://xf8b.github.io/AdminBot/docs/", true)
                        .addField("GitHub Repository", "https://github.com/xf8b/AdminBot/", true)
                        .setFooter("Made by xf8b#9420 and open source contributors", "https://cdn.discordapp.com/avatars/332600665412993045/d1de6c46d40fcb4c6200f86cb5a073af.png")
                        .setColor(Color.BLUE)
            }
        }.then()
    }
}