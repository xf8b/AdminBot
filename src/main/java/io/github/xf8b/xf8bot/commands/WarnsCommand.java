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

import com.google.common.collect.Range;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument;
import io.github.xf8b.xf8bot.data.WarnContext;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class WarnsCommand extends AbstractCommand {
    private static final StringArgument MEMBER = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("member")
            .build();

    public WarnsCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}warns")
                .setDescription("Gets the warns for the specified member.")
                .setCommandType(CommandType.ADMINISTRATION)
                .setMinimumAmountOfArgs(1)
                .addArgument(MEMBER)
                .setBotRequiredPermissions(Permission.EMBED_LINKS)
                .setAdministratorLevelRequired(1));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Optional<Snowflake> userId = ParsingUtil.parseUserIdAsSnowflake(guild, event.getValueOfArgument(MEMBER).get());
        if (userId.isEmpty()) {
            return channel.createMessage("The member does not exist!").then();
        }
        String username = guild.getMemberById(userId.get())
                .map(Member::getDisplayName)
                .block();
        MongoCollection<Document> mongoCollection = event.getXf8bot()
                .getMongoDatabase()
                .getCollection("warns");
        Flux<WarnContext> warnsFlux = Flux.from(mongoCollection.find(Filters.and(
                Filters.eq("guildId", guild.getId().asLong()),
                Filters.eq("userId", userId.get().asLong())
        ))).map(document -> new WarnContext(
                Snowflake.of(document.getLong("memberWhoWarnedId")),
                document.getString("reason"),
                document.getString("warnId")
        ));
        if (Objects.equals(warnsFlux.count().block(), 0L)) {
            return channel.createMessage("The user has no warnings.").then();
        } else {
            return channel.createEmbed(embedCreateSpec -> {
                embedCreateSpec.setTitle("Warnings For `" + username + "`").setColor(Color.BLUE);
                List<WarnContext> warns = warnsFlux.collectList().block();
                warns.forEach(warnContext -> embedCreateSpec.addField(
                        "`" + warnContext.getReason() + "`",
                        "Warn ID: " + warnContext.getWarnId() + "\n" +
                        "Member Who Warned: " + guild.getMemberById(warnContext.getMemberWhoWarnedId())
                                .map(Member::getNicknameMention)
                                .block(),
                        true
                ));
            }).then();
        }
    }
}