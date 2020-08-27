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

package io.github.xf8b.adminbot.handlers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;

import java.util.concurrent.ThreadLocalRandom;

public class SlapBrigadierCommand implements Command<MessageCreateEvent> {
    private static final String[] ITEMS = {
            "large bat",
            "large trout",
            "wooden door",
            "metal pipe",
            "vent",
            "glass bottle"
    };

    public static void register(CommandDispatcher<MessageCreateEvent> commandDispatcher) {
        commandDispatcher.register(LiteralArgumentBuilder.<MessageCreateEvent>literal(">slap").then(
                RequiredArgumentBuilder.<MessageCreateEvent, String>argument("person", StringArgumentType.greedyString())
                        .executes(new SlapBrigadierCommand())
        ));
    }

    @Override
    public int run(CommandContext<MessageCreateEvent> context) {
        String selfDisplayName = context.getSource().getGuild().flatMap(Guild::getSelfMember).map(Member::getDisplayName).block();
        String senderUsername = context.getSource().getMember().get().getDisplayName();
        String personToSlap = StringArgumentType.getString(context, "person");
        String itemToUse = ITEMS[ThreadLocalRandom.current().nextInt(ITEMS.length)];
        if (personToSlap.equalsIgnoreCase(selfDisplayName)) {
            personToSlap = senderUsername;
            senderUsername = selfDisplayName;
        }
        String finalSenderUsername = senderUsername;
        String finalPersonToSlap = personToSlap;
        context.getSource()
                .getMessage()
                .getChannel()
                .flatMap(messageChannel -> messageChannel.createMessage(finalSenderUsername + " slapped " + finalPersonToSlap + " with a " + itemToUse + "!"))
                .subscribe();
        return SINGLE_SUCCESS;
    }
}
