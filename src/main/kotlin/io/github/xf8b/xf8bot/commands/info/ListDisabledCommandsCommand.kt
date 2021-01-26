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
import io.github.xf8b.xf8bot.database.actions.find.GetGuildDisabledCommandsAction
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.immutableListOf
import io.github.xf8b.xf8bot.util.extensions.toSingletonPermissionSet
import io.github.xf8b.xf8bot.util.extensions.updatedRows
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class ListDisabledCommandsCommand : Command(
    name = "\${prefix}listdisabledcommands",
    description = "Shows all the disabled commands for this guild.",
    commandType = CommandType.INFO,
    aliases = immutableListOf("\${prefix}disabled", "\${prefix}listdisabled", "\${prefix}disabledcommands"),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.xf8bot.botDatabase
        .execute(GetGuildDisabledCommandsAction(event.guildId.get()))
        .filterWhen { result -> result.updatedRows }
        .flatMap { result -> result.map { row, _ -> row["command", String::class.java]!! }.toMono() }
        .collectList()
        .flatMap { disabledCommands ->
            event.channel.flatMap { channel ->
                channel.createEmbedDsl {
                    title("Disabled Commands")

                    field("Commands", disabledCommands.joinToString(separator = "\n") { "`$it`" }, inline = false)

                    footer("Disabled commands cannot be used by anyone, unless they have an administrator level of 4.")
                    color(Color.RED)
                    timestamp()
                }
            }
        }
        .switchIfEmpty(event.channel.flatMap { it.createMessage("There are no disabled commands.") })
        .then()
}