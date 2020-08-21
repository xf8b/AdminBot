package io.github.xf8b.adminbot.handlers;

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
import io.github.xf8b.adminbot.events.CommandFiredEvent;
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
    public MemberInfoCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}memberinfo")
                .setUsage("${prefix}memberinfo <member>")
                .setDescription("Shows information about the member.")
                .setCommandType(CommandType.OTHER)
                .addAlias("${prefix}userinfo")
                .setMinimumAmountOfArgs(1)
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS)));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String content = event.getMessage().getContent();
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String userId = String.valueOf(ParsingUtil.parseUserId(guild, content.trim().substring(content.trim().indexOf(" ") + 1).trim()));
        if (userId.equals("null")) {
            channel.createMessage("The member does not exist!").block();
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.UK)
                .withZone(ZoneOffset.UTC);
        guild.getMemberById(Snowflake.of(userId))
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
