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

import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono

class InfoCommand : Command(
    name = "\${prefix}information",
    description = "Shows some information about me.",
    commandType = CommandType.INFO,
    aliases = "\${prefix}info".toSingletonImmutableList(),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.prefix.flatMap { prefix ->
        event.channel.flatMap { channel ->
            event.client.self.flatMap { self ->
                channel.createEmbedDsl {
                    author(self.username, url = "https://github.com/xf8b/xf8bot/", self.avatarUrl)

                    title("Information")
                    url("https://xf8b.github.io/documentation/xf8bot/")
                    description("xf8bot is a general purpose bot. Originally known as AdminBot.")

                    field("Current Version", event.xf8bot.version.toStringVersion(), inline = true)
                    field(
                        "Build Metadata",
                        event.xf8bot.version.buildMetadata
                            .takeUnless(String::isBlank)
                            ?: "No build metadata",
                        inline = true
                    )
                    field("Pre Release", event.xf8bot.version.preRelease.isNotBlank().toString(), inline = true)

                    field(
                        "License",
                        "GNU Affero General Public License v3, or at your option, any later version",
                        inline = false
                    )

                    field("Current Prefix", "`$prefix`", inline = false)

                    field("Discord Framework", "[Discord4J](https://discord4j.com)", inline = true)
                    field("Discord4J Version", "3.2.0-SNAPSHOT", inline = true)

                    field("Total Amount of Commands", event.xf8bot.commandRegistry.size.toString(), inline = false)

                    field(
                        "Documentation",
                        "[Website](https://xf8b.github.io/documentation/xf8bot/)",
                        inline = true
                    )
                    field("GitHub Repository", "https://github.com/xf8b/xf8bot/", inline = true)

                    footer(
                        "Made by xf8b#9420 and open source contributors",
                        iconUrl = "https://cdn.discordapp.com/avatars/332600665412993045/d1de6c46d40fcb4c6200f86cb5a073af.png"
                    )
                    color(Color.BLUE)
                }
            }
        }
    }.then()
}