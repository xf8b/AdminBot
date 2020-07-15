package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@CommandHandler(
        name = "${prefix}clear",
        usage = "${prefix}clear <amount>",
        description = "Deletes the specified amount of messages from the channel. Clears over 100 messages are not allowed."
)
public class ClearCommandHandler {
    public static void onClearCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
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
            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "clear <amount>`.").queue();
            return;
        }
        if (isAdministrator) {
            int amountToClear;
            try {
                amountToClear = Integer.parseInt(content.replace(command, "").trim());
            } catch (NumberFormatException exception) {
                channel.sendMessage("The amount of messages to be cleared is not a number!").queue();
                return;
            }
            if (amountToClear > 100) {
                channel.sendMessage("Sorry, but the limit for message clearing is 100 messages.").queue();
                return;
            } else if (amountToClear < 1) {
                channel.sendMessage("Sorry, but the minimum for message clearing is 1 message.").queue();
                return;
            }
            channel.getHistory().retrievePast(amountToClear).queue(messages -> {
                try {
                    int amountOfMessagesPurged = messages.size();
                    channel.purgeMessages(messages);
                    channel.sendMessage("Successfully purged " + amountOfMessagesPurged + " message(s).")
                            .queue(message -> message.delete().queueAfter(3, TimeUnit.SECONDS));
                } catch (InsufficientPermissionException exception) {
                    channel.sendMessage("Cannot clear messages due to insufficient permissions!").queue();
                }
            });
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
