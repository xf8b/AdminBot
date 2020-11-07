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

package io.github.xf8b.xf8bot.commands.other

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import discord4j.core.event.domain.message.MessageCreateEvent
import java.util.concurrent.ThreadLocalRandom

class SlapBrigadierCommand : Command<MessageCreateEvent> {
    companion object {
        private val ITEMS = arrayOf(
            "large bat",
            "large trout",
            "wooden door",
            "metal pipe",
            "vent",
            "glass bottle"
        )

        fun register(commandDispatcher: CommandDispatcher<MessageCreateEvent>) {
            commandDispatcher.register(
                LiteralArgumentBuilder.literal<MessageCreateEvent>(">slap").then(
                    RequiredArgumentBuilder.argument<MessageCreateEvent, String>(
                        "person",
                        StringArgumentType.greedyString()
                    ).executes(SlapBrigadierCommand())
                )
            )
        }
    }

    override fun run(context: CommandContext<MessageCreateEvent>): Int {
        context.source
            .guild
            .flatMap { it.selfMember }
            .map { it.displayName }
            .flatMap { selfDisplayName ->
                var senderUsername = context.source
                    .member
                    .get()
                    .displayName
                var personToSlap = StringArgumentType.getString(context, "person")
                val itemToUse = ITEMS[ThreadLocalRandom.current().nextInt(ITEMS.size)]
                if (personToSlap.equals(selfDisplayName, ignoreCase = true)) {
                    personToSlap = senderUsername
                    senderUsername = selfDisplayName
                }
                context.source
                    .message
                    .channel
                    .flatMap { it.createMessage("$senderUsername slapped $personToSlap with a $itemToUse!") }
            }.block()
        return Command.SINGLE_SUCCESS
    }
}