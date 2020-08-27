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

import com.google.common.collect.Range;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import io.github.xf8b.adminbot.util.ClientExceptionUtil;
import io.github.xf8b.adminbot.util.MemberUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MemberInfoCommandHandler extends AbstractCommandHandler {
    private static final StringArgument MEMBER = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("member")
            .build();

    public MemberInfoCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}memberinfo")
                .setDescription("Shows information about the member.")
                .setCommandType(CommandType.OTHER)
                .addAlias("${prefix}userinfo")
                .setMinimumAmountOfArgs(1)
                .addArgument(MEMBER)
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS)));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        Snowflake userId = ParsingUtil.parseUserIdAndReturnSnowflake(guild, event.getValueOfArgument(MEMBER));
        if (userId == null) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.UK)
                .withZone(ZoneOffset.UTC);
        guild.getMemberById(userId)
                .onErrorResume(ClientExceptionUtil.isClientExceptionWithCode(10007), throwable1 -> Mono.fromRunnable(() -> channel.createMessage("The member is not in the guild!").block())) //unknown member
                .flatMap(member -> {
                    String displayName = member.getDisplayName();
                    String avatarUrl = member.getAvatarUrl();
                    Instant memberJoinServerTime = member.getJoinTime();
                    String id = member.getId().asString();
                    Color color = member.getColor().block();
                    Status status = member.getPresence().map(Presence::getStatus).block();
                    Optional<Activity> optionalActivity = member.getPresence().map(Presence::getActivity).block();
                    String activity;
                    if (optionalActivity.isEmpty()) {
                        activity = "No activity.";
                    } else {
                        activity = optionalActivity.get().getName();
                    }
                    boolean isOwner = member.getId().equals(guild.getOwnerId());
                    AtomicReference<String> roleMentions = new AtomicReference<>("");
                    member.getRoles().map(Role::getMention).subscribe(mention -> roleMentions.set(roleMentions.get() + " " + mention));
                    return channel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("Info For Member `" + MemberUtil.getTagWithDisplayName(member) + "`")
                            .setAuthor(displayName, null, avatarUrl)
                            .addField("Is Owner:", String.valueOf(isOwner), true)
                            .addField("Is Bot:", String.valueOf(member.isBot()), true)
                            .addField("Roles:", roleMentions.get(), true)
                            .addField("Status:", StringUtils.capitalize(status.name().toLowerCase().replace("_", " ")), true)
                            .addField("Activity:", activity, true)
                            .addField("Joined Server:", formatter.format(memberJoinServerTime), true)
                            .addField("ID:", id, true)
                            .addField(
                                    "Role Color RGB:",
                                    "Red: " + color.getRed() + ", Green: " + color.getGreen() + ", Blue: " + color.getBlue(),
                                    true
                            )
                            .addField("Avatar URL:", avatarUrl, true)
                            .setTimestamp(Instant.now()));
                }).subscribe();
    }
}
