package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
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

public class KickCommandHandler extends CommandHandler {
    public KickCommandHandler() {
        super(
                "${prefix}kick",
                "${prefix}kick <member> [reason]",
                "Kicks the specified member with the reason provided, or `No kick reason was provided` if there was none.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER,
                2
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
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
                    reason = "No kick reason was provided.";
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
                if (!guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                    channel.sendMessage("Cannot kick member due to insufficient permissions!").queue();
                    return;
                }
                String finalReason = reason;
                String finalReason1 = reason;
                guild.retrieveMemberById(userId).queue(member -> {
                    if (member == null) {
                        throw new IllegalStateException("Member is null!");
                    } else if (member == event.getMember()) {
                        channel.sendMessage("You cannot kick yourself!").queue();
                        return;
                    } else if (member.getUser() == event.getJDA().getSelfUser()) {
                        channel.sendMessage("You cannot kick AdminBot!").queue();
                        return;
                    } else if (!guild.getSelfMember().canInteract(member)) {
                        channel.sendMessage("Cannot kick member due to insufficient permissions!").queue();
                        return;
                    }
                    member.kick(finalReason1).queue(
                            success -> channel.sendMessage("Successfully kicked " + member.getUser().getName() + "!").queue(),
                            failure -> channel.sendMessage("Failed to kick " + member.getUser().getName() + ".").queue());
                    member.getUser().openPrivateChannel().queue(privateChannel -> {
                        if (member.getUser().isBot()) return;
                        MessageEmbed embed = new EmbedBuilder()
                                .setTitle("You were kicked!")
                                .addField("Server", guild.getName(), false)
                                .addField("Reason", finalReason, false)
                                .setTimestamp(Instant.now())
                                .setColor(Color.RED)
                                .build();
                        privateChannel.sendMessage(embed).queue();
                    });
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
