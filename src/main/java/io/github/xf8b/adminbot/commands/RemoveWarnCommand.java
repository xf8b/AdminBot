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

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.IntegerFlag;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.exceptions.ThisShouldNotHaveBeenThrownException;
import io.github.xf8b.adminbot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class RemoveWarnCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag MEMBER_WHO_WARNED = StringFlag.builder()
            .setShortName("mww")
            .setLongName("memberwhowarned")
            .setRequired(false)
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .build();
    private static final IntegerFlag WARN_ID = IntegerFlag.builder()
            .setShortName("w")
            .setLongName("warnid")
            .setRequired(false)
            .build();

    public RemoveWarnCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}removewarn")
                .setDescription("""
                        Removes the specified member's warns with the warnId and reason provided.\s
                        If the reason is all, all warns will be removed. The warnId is not needed.
                        If the warnId is all, all warns with the same reason will be removed.\s
                        """)
                .setCommandType(CommandType.ADMINISTRATION)
                .setAliases("${prefix}removewarns", "${prefix}rmwarn", "${prefix}rmwarns")
                .setMinimumAmountOfArgs(2)
                .setFlags(MEMBER, MEMBER_WHO_WARNED, REASON, WARN_ID)
                .setAdministratorLevelRequired(1));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER)
                .orElseThrow(ThisShouldNotHaveBeenThrownException::new));
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        Snowflake memberWhoWarnedId;
        if (event.getValueOfFlag(MEMBER_WHO_WARNED).isPresent()) {
            Optional<Snowflake> tempMemberWhoWarnedId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER_WHO_WARNED).get());
            if (tempMemberWhoWarnedId.isEmpty()) {
                return channel.createMessage("The member who warned does not exist!").then();
            } else {
                memberWhoWarnedId = tempMemberWhoWarnedId.get();
            }
        } else {
            memberWhoWarnedId = null;
        }
        String reason = event.getValueOfFlag(REASON)
                .orElseThrow(ThisShouldNotHaveBeenThrownException::new);
        int warnId = event.getValueOfFlag(WARN_ID).orElse(-1);
        MongoCollection<Document> mongoCollection = event.getAdminBot()
                .getMongoDatabase()
                .getCollection("warns");
        Flux<Document> warnDocuments = Flux.from(mongoCollection.find(Filters.and(
                Filters.eq("guildId", guild.getId().asLong()),
                Filters.eq("userId", userId.get().asLong())
        )));
        if (reason.equalsIgnoreCase("all")) {
            return warnDocuments.flatMap(document -> Mono.from(mongoCollection.deleteOne(document)))
                    .flatMap($ -> guild.getMemberById(userId.get()))
                    .then(guild.getMemberById(userId.get()).flatMap(member -> channel.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + ".")))
                    .then();
        } else {
            return warnDocuments.filter(document -> document.get("reason", String.class).equals(reason))
                    .filter(document -> {
                        if (warnId != -1) {
                            return document.get("warnId", Integer.TYPE).equals(warnId);
                        } else {
                            return true;
                        }
                    })
                    .filter(document -> {
                        if (memberWhoWarnedId != null) {
                            return document.get("memberWhoWarnedId", Long.TYPE).equals(memberWhoWarnedId.asLong());
                        } else {
                            return true;
                        }
                    })
                    .flatMap(mongoCollection::deleteOne)
                    .cast(Object.class)
                    .doOnComplete(() -> guild.getMemberById(userId.get()).flatMap(member -> channel.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + ".")).subscribe())
                    .switchIfEmpty(channel.createMessage("The user does not have a warn with that reason!"))
                    .then();

        }
    }
}
