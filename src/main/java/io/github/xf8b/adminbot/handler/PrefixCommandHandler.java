package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.helper.PrefixesDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;

@CommandHandler(
        name = "${prefix}prefix",
        usage = "${prefix}prefix <prefix>",
        description = "Sets the prefix to the specified prefix."
)
public class PrefixCommandHandler {
    public static void onPrefixCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        String command = content.split(" ")[0];
        String previousPrefix = AdminBot.prefix;
        String newPrefix = content.replace(command, "").trim();
        boolean isAdministrator = false;
        for (Role role : event.getMember().getRoles()) {
            String id = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, id)) {
                isAdministrator = true;
            }
        }
        if (event.getMember().isOwner()) isAdministrator = true;
        if (isAdministrator) {
            if (previousPrefix.equals(newPrefix)) {
                event.getChannel().sendMessage("You can't set the prefix to the same thing, silly.").queue();
            } else if (newPrefix.equals("")) {
                event.getChannel().sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "prefix <prefix>`").queue();
            } else {
                AdminBot.prefix = newPrefix;
                try {
                    PrefixesDatabaseHelper.overwritePrefixForGuild(event.getGuild().getId(), newPrefix);
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
                AdminBot.commandRegistry.updatePrefix();
                event.getChannel().sendMessage("Successfully set prefix from " + previousPrefix + " to " + newPrefix + ".").queue();
            }
        } else {
            channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
        }
    }
}
