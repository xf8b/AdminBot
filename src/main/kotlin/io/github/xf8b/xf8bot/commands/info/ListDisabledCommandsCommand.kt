package io.github.xf8b.xf8bot.commands.info

import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.database.actions.find.GetGuildDisabledCommandsAction
import io.github.xf8b.xf8bot.util.createEmbedDsl
import io.github.xf8b.xf8bot.util.hasUpdatedRows
import io.github.xf8b.xf8bot.util.immutableListOf
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class ListDisabledCommandsCommand : AbstractCommand(
    name = "\${prefix}listdisabledcommands",
    description = "Shows all the disabled commands for this guild.",
    commandType = CommandType.INFO,
    aliases = immutableListOf("\${prefix}disabled", "\${prefix}listdisabled", "\${prefix}disabledcommands"),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.xf8bot.botDatabase
        .execute(GetGuildDisabledCommandsAction(event.guildId.get()))
        .filter { it.isNotEmpty() }
        .filterWhen { it[0].hasUpdatedRows }
        .flatMapMany { results ->
            results.toFlux().flatMap { it.map { row, _ -> row["command", String::class.java] } }
        }
        .collectList()
        .filter { it.isNotEmpty() }
        .flatMap { disabledCommands ->
            event.channel.flatMap { channel ->
                channel.createEmbedDsl {
                    title("Disabled Commands")

                    field("Commands", disabledCommands.joinToString(separator = "\n") { "`$it`" }, false)

                    footer("Disabled commands cannot be used by anyone, unless they have an administrator level of 4.")
                    color(Color.RED)
                    timestamp()
                }
            }
        }
        .switchIfEmpty(event.channel.flatMap { it.createMessage("There are no disabled commands.") })
        .then()
}