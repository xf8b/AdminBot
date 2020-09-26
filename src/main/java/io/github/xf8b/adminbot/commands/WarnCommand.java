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
import discord4j.rest.util.Color;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.api.commands.AbstractCommand;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.ExtensionsKt;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class WarnCommand extends AbstractCommand {
    private static final StringFlag MEMBER = StringFlag.builder()
            .setShortName("m")
            .setLongName("member")
            .build();
    private static final StringFlag REASON = StringFlag.builder()
            .setShortName("r")
            .setLongName("reason")
            .setValidityPredicate(value -> !value.equals("all"))
            .setInvalidValueErrorMessageFunction(invalidValue -> {
                if (invalidValue.equals("all")) {
                    return "Sorry, but this warn reason is reserved.";
                } else {
                    return Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE;
                }
            })
            .setRequired(false)
            .build();

    public WarnCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}warn")
                .setDescription("Warns the specified member with the specified reason, or `No warn reason was provided` if there was none.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .setFlags(MEMBER, REASON)
                .setAdministratorLevelRequired(1));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        AdminBot adminBot = event.getAdminBot();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        channel.createMessage("User ID: " + userId.get().asLong()).subscribe();
        Snowflake memberWhoWarnedId = event.getMember().orElseThrow().getId();
        String reason = event.getValueOfFlag(REASON).orElse("No warn reason was provided.");
        if (reason.equals("all")) {
            return channel.createMessage("Sorry, but this warn reason is reserved.").then();
        }
        MongoCollection<Document> mongoCollection = event.getAdminBot()
                .getMongoDatabase()
                .getCollection("warns");
        return guild.getMemberById(userId.get())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .flatMap(member -> {
                    if (!PermissionUtil.isMemberHigher(adminBot, guild, event.getMember().get(), member)) {
                        channel.createMessage("Cannot warn member because the member is equal to or higher than you!").block();
                        return Mono.empty();
                    } else {
                        return Mono.just(member);
                    }
                })
                .flatMap(member -> {
                    Mono<?> privateChannelMono = member.getPrivateChannel()
                            .flatMap(privateChannel -> {
                                if (member.isBot()) {
                                    return Mono.empty();
                                } else if (member.equals(event.getClient().getSelf().block())) {
                                    return Mono.empty();
                                } else {
                                    return Mono.just(privateChannel);
                                }
                            })
                            .flatMap(privateChannel -> privateChannel
                                    .createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("You were warned!")
                                            .setFooter("Warned by: " + ExtensionsKt.getTagWithDisplayName(event.getMember().get()), event.getMember().get().getAvatarUrl())
                                            .addField("Server", guild.getName(), false)
                                            .addField("Reason", reason, false)
                                            .setTimestamp(Instant.now())
                                            .setColor(Color.RED)))
                            .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(50007), throwable -> Mono.empty()); //cannot send to user
                    return Flux.from(mongoCollection.find(Filters.eq("userId", userId)))
                            .sort(Comparator.comparing(o -> o.get("warnId", Integer.TYPE)))
                            .take(1)
                            .flatMap(document -> {
                                int warnId = document.get("warnId", Integer.TYPE) + 1;
                                return Mono.from(mongoCollection.insertOne(new Document()
                                        .append("memberWhoWarnedId", memberWhoWarnedId.asLong())
                                        .append("warnId", warnId)
                                        .append("reason", reason)));
                            })
                            .switchIfEmpty(Mono.from(mongoCollection.insertOne(new Document()
                                    .append("memberWhoWarnedId", memberWhoWarnedId.asLong())
                                    .append("warnId", 0)
                                    .append("reason", reason))))
                            .then(privateChannelMono)
                            .then(channel.createMessage("Successfully warned " + member.getDisplayName() + "."))
                            .then();
                }).then();
    }
}
