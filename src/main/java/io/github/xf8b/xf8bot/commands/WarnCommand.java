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
import discord4j.rest.util.Color;
import io.github.xf8b.xf8bot.Xf8bot;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.flags.Flag;
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag;
import io.github.xf8b.xf8bot.util.ClientExceptionUtil;
import io.github.xf8b.xf8bot.util.ExtensionsKt;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import io.github.xf8b.xf8bot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
            .setNotRequired()
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
        Xf8bot xf8bot = event.getXf8bot();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfFlag(MEMBER).get());
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        Snowflake memberWhoWarnedId = event.getMember().orElseThrow().getId();
        String reason = event.getValueOfFlag(REASON).orElse("No warn reason was provided.");
        if (reason.equals("all")) {
            return channel.createMessage("Sorry, but this warn reason is reserved.").then();
        }
        MongoCollection<Document> mongoCollection = event.getXf8bot()
                .getMongoDatabase()
                .getCollection("warns");
        return guild.getMemberById(userId.get())
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .filterWhen(member -> PermissionUtil.isMemberHigher(xf8bot, guild, event.getMember().get(), member)
                        .doOnNext(bool -> {
                            if (!bool) {
                                channel.createMessage("Cannot warn member because the member is equal to or higher than you!").block();
                            }
                        }))
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
                    return Flux.from(mongoCollection.find(Filters.eq("userId", userId.get().asLong())))
                            .flatMap(document -> Mono.from(mongoCollection.insertOne(new Document()
                                    .append("guildId", guild.getId().asLong())
                                    .append("userId", userId.get().asLong())
                                    .append("memberWhoWarnedId", memberWhoWarnedId.asLong())
                                    .append("warnId", UUID.randomUUID().toString())
                                    .append("reason", reason))))
                            .switchIfEmpty(Mono.from(mongoCollection.insertOne(new Document()
                                    .append("guildId", guild.getId().asLong())
                                    .append("userId", userId.get().asLong())
                                    .append("memberWhoWarnedId", memberWhoWarnedId.asLong())
                                    .append("warnId", UUID.randomUUID().toString())
                                    .append("reason", reason))))
                            .then(privateChannelMono)
                            .then(channel.createMessage("Successfully warned " + member.getDisplayName() + "."))
                            .then();
                }).then();
    }
}
