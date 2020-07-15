package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}kick",
        usage = "${prefix}kick <member> [reason]",
        description = "Kicks the specified member with the reason provided, or `No reason was provided` if there was none."
)
public class KickCommandHandler {
    public static void onKickCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        boolean isAdministrator = false;
        String command = content.split(" ")[0];
        for (Role role : event.getMember().getRoles()) {
            String id = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, id)) {
                isAdministrator = true;
            }
        }
        if (event.getMember().isOwner()) isAdministrator = true;
        if (content.trim().equals(command)) {
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "kick <member> [reason]`.").queue();
            return;
        }
        if (isAdministrator) {
            String args = content.replace(command, "").trim();
            String userId = args.replaceAll("(<@!|>)", "").replaceAll("(?<=\\s).*", "").trim();
            String reason = args.replaceAll("^[^ ]* ", "").trim();
            try {
                Long.parseLong(userId);
            } catch (NumberFormatException exception) {
                channel.sendMessage("The member does not exist!").queue();
                return;
            }
            if (userId.equals(reason) || reason.equals("")) {
                reason = "No kick reason was provided.";
            }
            if (userId.equals("")) {
                channel.sendMessage("The member does not exist!").queue();
                return;
            }
            if (!guild.getMember(event.getJDA().getSelfUser()).hasPermission(Permission.KICK_MEMBERS)) {
                channel.sendMessage("Cannot kick member due to insufficient permissions!").queue();
                return;
            }
            String finalReason = reason;
            String finalReason1 = reason;
            guild.retrieveMemberById(userId).queue(member -> {
                if (member == null) {
                    throw new IllegalStateException("Member is null!");
                }
                if (member == event.getMember()) {
                    channel.sendMessage("You cannot kick yourself!").queue();
                    return;
                }
                if (member.getUser() == event.getJDA().getSelfUser()) {
                    channel.sendMessage("You cannot kick AdminBot!").queue();
                    return;
                }
                member.getUser().openPrivateChannel().queue(privateChannel -> {
                    if (member.getUser().isBot()) {
                        return;
                    }
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("You were kicked!")
                            .addField("Server", guild.getName(), false)
                            .addField("Reason", finalReason, false)
                            .setColor(Color.RED)
                            .build();
                    privateChannel.sendMessage(embed).queue();
                });
                member.kick(finalReason1).queue(
                        success -> channel.sendMessage("Successfully kicked " + member.getUser().getName() + "!").queue(),
                        failure -> channel.sendMessage("Failed to kick " + member.getUser().getName() + ".").queue());
            });
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
