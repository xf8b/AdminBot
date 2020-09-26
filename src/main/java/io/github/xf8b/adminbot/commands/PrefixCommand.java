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

package io.github.xf8b.adminbot.commands;

import com.google.common.collect.Range;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class PrefixCommand extends AbstractCommand {
    private static final StringArgument NEW_PREFIX = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("prefix")
            .setRequired(false)
            .build();

    public PrefixCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}prefix")
                .setDescription("Sets the prefix to the specified prefix.")
                .setCommandType(CommandType.OTHER)
                .setMinimumAmountOfArgs(1)
                .addArgument(NEW_PREFIX)
                .setAdministratorLevelRequired(4));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Snowflake guildId = event.getGuild().map(Guild::getId).block();
        String previousPrefix = event.getPrefix().block();
        Optional<String> newPrefix = event.getValueOfArgument(NEW_PREFIX);
        MongoCollection<Document> collection = event.getAdminBot()
                .getMongoDatabase()
                .getCollection("prefixes");
        if (newPrefix.isEmpty()) {
            //reset prefix
            return Mono.from(collection.findOneAndUpdate(
                    Filters.eq("guildId", guildId.asLong()),
                    Updates.set("prefix", AdminBot.DEFAULT_PREFIX)
            )).then(channel.createMessage("Successfully reset prefix.")).then();
        } else if (previousPrefix.equals(newPrefix.get())) {
            return channel.createMessage("You can't set the prefix to the same thing, silly.").then();
        } else {
            //set prefix
            return Mono.from(collection.findOneAndUpdate(
                    Filters.eq("guildId", guildId.asLong()),
                    Updates.set("prefix", newPrefix.get())
            )).then(channel.createMessage("Successfully set prefix from " + previousPrefix + " to " + newPrefix.get() + ".")).then();
        }
    }
}
