package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}unban",
        usage = "${prefix}unban <member>",
        description = "Unbans the specified member."
)
public class UnbanCommandHandler {
    public static void onUnbanCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
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
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "unban <member>`.").queue();
            return;
        }
        if (isAdministrator) {
            String args = content.replace(command, "").trim();
            String userId = args.replaceAll("(<@!|>)", "").replaceAll("(?<=\\s).*", "").trim();
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
            try {
                guild.retrieveBanById(userId).queue(ban -> {
                    String username = ban.getUser().getName();
                    guild.unban(userId).queue();
                    channel.sendMessage("Successfully unbanned " + username + "!").queue();
                }, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        if (((ErrorResponseException) throwable).getErrorResponse() == ErrorResponse.UNKNOWN_BAN) {
                            channel.sendMessage("The member is not banned!").queue();
                        } else if (((ErrorResponseException) throwable).getErrorResponse() == ErrorResponse.MISSING_PERMISSIONS) {
                            channel.sendMessage("Cannot unban member due to insufficient permissions!").queue();
                        }
                    }
                });
            } catch (InsufficientPermissionException exception) {
                channel.sendMessage("Cannot unban member due to insufficient permissions!").queue();
            }
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
