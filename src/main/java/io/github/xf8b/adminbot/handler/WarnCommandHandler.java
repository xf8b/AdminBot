package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.helper.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}warn",
        usage = "${prefix}warn <member> [reason]",
        description = "Warns the specified member with the reason provided, or `No reason was provided` if there was none."
)
public class WarnCommandHandler {
    public static void onWarnCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
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
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "warn <member> [reason]`.").queue();
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
                reason = "No warn reason was provided.";
            }
            if (userId.equals("")) {
                channel.sendMessage("The member does not exist!").queue();
                return;
            }
            if (reason.equals("all")) {
                channel.sendMessage("Sorry, but this warn reason is reserved.").queue();
                return;
            }
            guild.retrieveMemberById(userId).queue(member -> {
                if (member == null) {
                    throw new IllegalStateException("Member is null!");
                } else if (member.getUser().isBot()) {
                    channel.sendMessage("You cannot warn bots!").queue();
                }
            });
            if (WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, reason)) {
                String warnId = String.valueOf(Integer.parseInt(WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId).get(reason).iterator().next()) + 1);
                WarnsDatabaseHelper.insertIntoWarns(guildId, userId, warnId, reason);
            } else {
                WarnsDatabaseHelper.insertIntoWarns(guildId, userId, String.valueOf(0), reason);
            }
            String finalReason = reason;
            guild.retrieveMemberById(userId).queue(member -> {
                if (!member.getUser().isBot()) {
                    member.getUser().openPrivateChannel().queue(privateChannel -> {
                        MessageEmbed embed = new EmbedBuilder()
                                .setTitle("You were warned!")
                                .addField("Server", guild.getName(), false)
                                .addField("Reason", finalReason, false)
                                .setColor(Color.RED)
                                .build();
                        privateChannel.sendMessage(embed).queue();
                        channel.sendMessage("Successfully warned " + member.getAsMention() + ".").queue();
                    });
                }
            });
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
