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

package io.github.xf8b.xf8bot.commands.administration;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.common.util.Snowflake;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        Mono<Snowflake> userId = Mono.justOrEmpty(event.getValueOfFlag(MEMBER))
                .flatMap(it -> ParsingUtil.parseUserId(event.getGuild(), it)
                        .map(Snowflake::of)
                        .switchIfEmpty(event.getChannel().flatMap(it1 -> it1.createMessage("The member does not exist!"))
                                .then()
                                .cast(Snowflake.class)));
        Mono<Snowflake> memberWhoWarnedId = Mono.justOrEmpty(event.getValueOfFlag(MEMBER_WHO_WARNED))
                .flatMap(it -> ParsingUtil.parseUserId(event.getGuild(), it)
                        .map(Snowflake::of)
                        .switchIfEmpty(event.getChannel().flatMap(it1 -> it1.createMessage("The member who warned does not exist!"))
                                .then()
                                .cast(Snowflake.class)));
        String reason = event.getValueOfFlag(REASON).orElse(null);
        String warnId = event.getValueOfFlag(WARN_ID).orElse(null);
        MongoCollection<Document> mongoCollection = event.getXf8bot()
                .getMongoDatabase()
                .getCollection("warns");
        Mono<Bson> filter = userId.map(it -> Filters.and(
                Filters.eq("guildId", event.getGuildId().get().asLong()),
                Filters.eq("userId", it.asLong())
        )).defaultIfEmpty(Filters.eq("guildId", event.getGuildId().get().asLong()));
        Flux<Document> warnDocuments = filter.flatMapMany(it -> Flux.from(mongoCollection.find(it)));
        return Mono.zip(
                Mono.just(warnId == null),
                memberWhoWarnedId.flux().count().map(it -> it == 0L),
                userId.flux().count().map(it -> it == 0L),
                Mono.just(reason == null)
        ).filter(it -> !it.getT1() && !it.getT2() && !it.getT3() && !it.getT4())
                .flatMap($ -> {
                    if (reason != null && reason.equalsIgnoreCase("all")) {
                        return userId.flatMap(it -> warnDocuments.flatMap(document -> Mono.from(mongoCollection.deleteOne(document)))
                                .flatMap($1 -> event.getGuild().flatMap(it1 -> it1.getMemberById(it)))
                                .flatMap(member -> event.getChannel().flatMap(it1 -> it1.createMessage("Successfully removed warn(s) for " + member.getDisplayName() + ".")))
                                .switchIfEmpty(event.getChannel().flatMap(it1 -> it1.createMessage("Cannot remove all warns without a user!")))
                                .then());
                    } else {
                        return warnDocuments
                                .filter(document -> {
                                    if (warnId != null) {
                                        return document.getString("warnId").equals(warnId);
                                    } else {
                                        return true;
                                    }
                                })
                                .filter(document -> {
                                    if (reason != null) {
                                        return document.getString("reason").equals(reason);
                                    } else {
                                        return true;
                                    }
                                })
                                .filterWhen(document -> userId
                                        .map(it -> document.getLong("userId").equals(it.asLong()))
                                        .defaultIfEmpty(true))
                                .filterWhen(document -> memberWhoWarnedId
                                        .map(it -> document.getLong("memberWhoWarnedId").equals(it.asLong()))
                                        .defaultIfEmpty(true))
                                .flatMap(mongoCollection::deleteOne)
                                .cast(Object.class)
                                .flatMap($1 -> /*guild.getMemberById(userId).flatMap(member -> */event.getChannel().flatMap(it -> it.createMessage("Successfully removed warn(s)!" /*"Successfully removed warn(s) for " + member.getDisplayName() + ".")*/)))
                                .switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("The user does not have a warn with that reason!")))
                                .then();

                    }
                })
                .switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("You must have at least 1 search query!")).then());
    }
}
