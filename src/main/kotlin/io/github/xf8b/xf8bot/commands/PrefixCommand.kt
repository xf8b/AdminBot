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

package io.github.xf8b.xf8bot.commands

import com.google.common.collect.ImmutableList
import com.google.common.collect.Range
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.xf8b.xf8bot.Xf8bot
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import reactor.core.publisher.Mono

class PrefixCommand : AbstractCommand(
        name = "\${prefix}prefix",
        description = "Sets the prefix to the specified prefix.",
        commandType = CommandType.OTHER,
        minimumAmountOfArgs = 1,
        arguments = ImmutableList.of(NEW_PREFIX),
        administratorLevelRequired = 4
) {
    companion object {
        private val NEW_PREFIX = StringArgument.builder()
                .setIndex(Range.atLeast(1))
                .setName("prefix")
                .setRequired(false)
                .build()
    }

    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val channelMono: Mono<MessageChannel> = event.channel
        val guildId = event.guild.map { it.id }.block()!!
        val previousPrefix = event.prefix.block()!!
        val newPrefix = event.getValueOfArgument(NEW_PREFIX)
        val collection = event.xf8bot
                .mongoDatabase
                .getCollection("prefixes")
        return when {
            //reset prefix
            newPrefix.isEmpty -> Mono.from(collection.findOneAndUpdate(
                    Filters.eq("guildId", guildId.asLong()),
                    Updates.set("prefix", Xf8bot.DEFAULT_PREFIX)
            )).then(channelMono.flatMap {
                it.createMessage("Successfully reset prefix.")
            }).then()
            previousPrefix == newPrefix.get() -> channelMono.flatMap {
                it.createMessage("You can't set the prefix to the same thing, silly.")
            }.then()
            //set prefix
            else -> Mono.from(collection.findOneAndUpdate(
                    Filters.eq("guildId", guildId.asLong()),
                    Updates.set("prefix", newPrefix.get())
            )).then(channelMono.flatMap {
                it.createMessage("Successfully set prefix from " + previousPrefix + " to " + newPrefix.get() + ".")
            }).then()
        }
    }
}