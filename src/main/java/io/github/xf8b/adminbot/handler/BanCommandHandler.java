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
        name = "${prefix}ban",
        usage = "${prefix}ban <member> [reason]",
        description = "Bans the specified member with the reason provided, or `No reason was provided` if there was none."
)
public class BanCommandHandler {
    public static void onBanCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        boolean isAdministrator = false;
        for (Role role : event.getMember().getRoles()) {
            String id = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, id)) {
                isAdministrator = true;
            }
        }
        if (event.getMember().isOwner()) isAdministrator = true;
        String command = content.split(" ")[0];
        if (content.trim().equals(command)) {
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "ban <member> [reason]`.").queue();
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
                reason = "No ban reason was provided.";
            }
            if (userId.equals("")) {
                channel.sendMessage("The member does not exist!").queue();
                return;
            }
            if (!guild.getMember(event.getJDA().getSelfUser()).hasPermission(Permission.BAN_MEMBERS)) {
                channel.sendMessage("Cannot ban member due to insufficient permissions!").queue();
                return;
            }
            String finalReason = reason;
            guild.retrieveBanById(userId).queue(ban -> channel.sendMessage("The user is already banned!").queue(),
                    throwable -> guild.retrieveMemberById(userId).queue(member -> {
                        if (member == null) {
                            throw new IllegalStateException("Member is null!");
                        }
                        if (member == event.getMember()) {
                            channel.sendMessage("You cannot ban yourself!").queue();
                            return;
                        }
                        if (member.getUser() == event.getJDA().getSelfUser()) {
                            channel.sendMessage("You cannot ban AdminBot!").queue();
                            return;
                        }
                        member.getUser().openPrivateChannel().queue(privateChannel -> {
                            if (member.getUser().isBot()) {
                                return;
                            }
                            MessageEmbed embed = new EmbedBuilder()
                                    .setTitle("You were banned!")
                                    .addField("Server", guild.getName(), false)
                                    .addField("Reason", finalReason, false)
                                    .setColor(Color.RED)
                                    .build();
                            privateChannel.sendMessage(embed).queue();
                        });
                        String username = member.getUser().getName();
                        member.ban(0, finalReason).queue(
                                success -> channel.sendMessage("Successfully banned " + username + "!").queue(),
                                failure -> channel.sendMessage("Failed to ban " + username + ".").queue()
                        );
                    }));
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
