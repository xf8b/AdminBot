package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.helper.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WarnCommandHandler extends CommandHandler {
    public WarnCommandHandler() {
        super(
                "${prefix}warn",
                "${prefix}warn <member> [reason]",
                "Warns the specified member with the specified reason, or `No warn reason was provided` if there was none.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.ADMINISTRATION,
                1
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            String guildId = guild.getId();
            Member author = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, author) &&
                    PermissionUtil.getAdministratorLevel(guild, author) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 2) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                String userId = content.trim().split(" ")[1].replaceAll("[<@!>]", "").trim();
                String reason;
                if (content.trim().split(" ").length < 3) {
                    reason = "No warn reason was provided.";
                } else {
                    reason = content.trim().substring(content.trim().indexOf(" ", content.trim().indexOf(" ") + 1) + 1).trim();
                }
                try {
                    Long.parseLong(userId);
                } catch (NumberFormatException exception) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                if (userId.equals("")) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                if (reason.equals("all")) {
                    channel.sendMessage("Sorry, but this warn reason is reserved.").queue();
                    return;
                }
                String finalReason = reason;
                guild.retrieveMemberById(userId).queue(member -> {
                    if (member == null) {
                        throw new IllegalStateException("Member is null!");
                    }
                    try {
                        if (WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, reason)) {
                            List<String> warnIds = new ArrayList<>();
                            WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId).forEach((reasonInDatabase, warnId) -> {
                                if (reasonInDatabase.equals(reason)) {
                                    warnIds.add(warnId);
                                }
                            });
                            Collections.reverse(warnIds);
                            String top = warnIds.get(0);
                            String warnId = String.valueOf(Integer.parseInt(top) + 1);
                            WarnsDatabaseHelper.insertIntoWarns(guildId, userId, warnId, reason);
                        } else {
                            WarnsDatabaseHelper.insertIntoWarns(guildId, userId, String.valueOf(0), reason);
                        }
                    } catch (ClassNotFoundException | SQLException exception) {
                        exception.printStackTrace();
                    }
                    try {
                        member.getUser().openPrivateChannel().queue(privateChannel -> {
                            if (member.getUser().isBot()) return;
                            if (member.getUser() == event.getJDA().getSelfUser()) return;
                            MessageEmbed embed = new EmbedBuilder()
                                    .setTitle("You were warned!")
                                    .addField("Server", guild.getName(), false)
                                    .addField("Reason", finalReason, false)
                                    .setTimestamp(Instant.now())
                                    .setColor(Color.RED)
                                    .build();
                            privateChannel.sendMessage(embed).queue();
                        });
                    } catch (UnsupportedOperationException ignored) {
                    }
                    channel.sendMessage("Successfully warned " + member.getAsMention() + ".").queue();
                }, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        if (((ErrorResponseException) throwable).getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                            channel.sendMessage("The member is not in the guild!").queue();
                        }
                    }
                });
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
