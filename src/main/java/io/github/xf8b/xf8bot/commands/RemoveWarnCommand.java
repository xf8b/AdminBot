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

package io.github.xf8b.xf8bot.commands;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class RemoveWarnCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .setNotRequired()
            .build();
    private static final StringFlag MEMBER_WHO_WARNED = StringFlag.builder()
            .setShortName("mww")
            .setLongName("memberWhoWarned")
            .setNotRequired()
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setNotRequired()
            .build();
    private static final StringFlag WARN_ID = StringFlag.builder()
            .setShortName("i")
            .setLongName("warnId")
            //TODO: fix
            .setValidityPredicate(s -> s.matches("\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"))
            .setInvalidValueErrorMessageFunction($ -> "The warn ID must be a UUID!")
            .setNotRequired()
            .build();

    public RemoveWarnCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}removewarn")
                .setDescription("""
                        Removes the specified member's warns with the warnId and reason provided.
                        If the reason is all, all warns will be removed. The warnId is not needed.
                        If the warnId is all, all warns with the same reason will be removed.
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
        Snowflake userId;
        if (event.getValueOfFlag(MEMBER).isPresent()) {
            Optional<Snowflake> tempUserId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
            if (tempUserId.isEmpty()) {
                return channel.createMessage("The member does not exist!").then();
            } else {
                userId = tempUserId.get();
            }
        } else {
            userId = null;
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
        String reason = event.getValueOfFlag(REASON).orElse(null);
        String warnId = event.getValueOfFlag(WARN_ID).orElse(null);
        MongoCollection<Document> mongoCollection = event.getXf8bot()
                .getMongoDatabase()
                .getCollection("warns");
        Bson filter;
        if (userId == null) {
            filter = Filters.eq("guildId", guild.getId().asLong());
        } else {
            filter = Filters.and(
                    Filters.eq("guildId", guild.getId().asLong()),
                    Filters.eq("userId", userId.asLong())
            );
        }
        Flux<Document> warnDocuments = Flux.from(mongoCollection.find(filter));
        if (warnId == null && memberWhoWarnedId == null && userId == null && reason == null) {
            return channel.createMessage("You must have at least 1 search query!").then();
        }
        if (reason != null && reason.equalsIgnoreCase("all")) {
            if (userId == null) {
                return channel.createMessage("Cannot remove all warns without a user!").then();
            }
            return warnDocuments.flatMap(document -> Mono.from(mongoCollection.deleteOne(document)))
                    .flatMap($ -> guild.getMemberById(userId))
                    .flatMap(member -> channel.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + "."))
                    .then();
        } else {
            return warnDocuments.filter(document -> {
                if (warnId != null) {
                    return document.getString("warnId").equals(warnId);
                } else {
                    return true;
                }
            }).filter(document -> {
                if (memberWhoWarnedId != null) {
                    return document.getLong("memberWhoWarnedId").equals(memberWhoWarnedId.asLong());
                } else {
                    return true;
                }
            }).filter(document -> {
                if (userId != null) {
                    return document.getLong("userId").equals(userId.asLong());
                } else {
                    return true;
                }
            }).filter(document -> {
                if (reason != null) {
                    return document.getString("reason").equals(reason);
                } else {
                    return true;
                }
            }).flatMap(mongoCollection::deleteOne)
                    .cast(Object.class)
                    .doOnComplete(() -> /*guild.getMemberById(userId).flatMap(member -> */channel.createMessage("Successfully removed warn(s)!" /*"Successfully removed warn(s) for " + member.getDisplayName() + ".")*/).subscribe())
                    .switchIfEmpty(channel.createMessage("The user does not have a warn with that reason!"))
                    .then();

        }
    }
}
