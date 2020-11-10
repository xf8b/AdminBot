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

package io.github.xf8b.xf8bot.commands.info

import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono

class InfoCommand : AbstractCommand(
    name = "\${prefix}information",
    description = "Shows some information about me.",
    commandType = CommandType.INFO,
    aliases = "\${prefix}info".toSingletonImmutableList(),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> = context.prefix.flatMap { prefix ->
        context.channel.flatMap { channel ->
            context.client.self.flatMap { self ->
                channel.createEmbed { embedCreateSpec ->
                    embedCreateSpec.setTitle("Information")
                        .setAuthor(self.username, "https://github.com/xf8b/xf8bot/", self.avatarUrl)
                        .setDescription("xf8bot is a general purpose bot. Originally known as AdminBot.")
                        .addField("Current Version", context.xf8bot.version.toStringVersion(), true)
                        .addField("Build Metadata", context.xf8bot.version.buildMetadata.let {
                            if (it.isBlank()) {
                                "No build metadata"
                            } else {
                                it
                            }
                        }, true)
                        .addField("Pre Release", context.xf8bot.version.preRelease.isNotBlank().toString(), true)
                        .addField("License", "GNU AGPL v3, or at your option, any later version", false)
                        .addField("Current Prefix", "`$prefix`", false)
                        .addField("Discord Framework", "Discord4J, https://discord4j.com", true)
                        .addField("Discord4J Version", "3.2.0-SNAPSHOT", true)
                        .addField("Total Amount of Commands", context.xf8bot.commandRegistry.size.toString(), false)
                        .addField("Documentation", "https://xf8b.github.io/documentation/xf8bot/", true)
                        .addField("GitHub Repository", "https://github.com/xf8b/xf8bot/", true)
                        .setFooter(
                            "Made by xf8b#9420 and open source contributors",
                            "https://cdn.discordapp.com/avatars/332600665412993045/d1de6c46d40fcb4c6200f86cb5a073af.png"
                        )
                        .setUrl("https://xf8b.github.io/documentation/xf8bot/")
                        .setColor(Color.BLUE)
                }
            }
        }
    }.then()
}