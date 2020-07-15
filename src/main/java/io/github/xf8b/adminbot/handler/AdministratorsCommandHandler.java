package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

@CommandHandler(
        name = "${prefix}administrators",
        usage = "${prefix}administrators <action> <role>",
        description = "Adds/removes from the list of administrator roles/members",
        actions = "`addrole`: Adds the role to the database of administrator roles\n" +
                "`removerole`: Removes the role from the database of administrator roles\n" +
                "`getroles`: Gets the administrator roles from the database of administrator roles"
)
public class AdministratorsCommandHandler {
    public static void onAdministratorsCommand(MessageReceivedEvent event) throws SQLException, ClassNotFoundException {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        String commandType = content.split(" ")[1].replace(AdminBot.prefix, "");
        boolean isAdministrator = false;
        for (Role role : event.getMember().getRoles()) {
            String id = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, id)) {
                isAdministrator = true;
            }
        }
        if (event.getMember().isOwner()) isAdministrator = true;
        switch (commandType) {
            case "addrole":
                if (isAdministrator) {
                    if (event.getMessage().getMentionedRoles().isEmpty()) {
                        channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "administrators <action> [role]`").queue();
                        break;
                    }
                    Role role = event.getMessage().getMentionedRoles().get(0);
                    String roleId = event.getMessage().getMentionedRoles().get(0).getId();
                    if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                        channel.sendMessage("The role already has been added as an administrator role.").queue();
                    } else {
                        AdministratorsDatabaseHelper.addToAdministrators(guildId, roleId);
                        channel.sendMessage("Successfully added " + role.getAsMention() + " to the list of administrator roles.").queue();
                    }
                } else {
                    channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
                }
                break;
            case "removerole":
                if (isAdministrator) {
                    if (event.getMessage().getMentionedRoles().isEmpty()) {
                        channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "administrators <action> [role]`").queue();
                        break;
                    }
                    Role role = event.getMessage().getMentionedRoles().get(0);
                    String roleId = event.getMessage().getMentionedRoles().get(0).getId();
                    if (!AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                        channel.sendMessage("The role has not been added as an administrator role.").queue();
                    } else {
                        AdministratorsDatabaseHelper.removeFromAdministrators(guildId, roleId);
                        channel.sendMessage("Successfully removed " + role.getAsMention() + " from the list of administrator roles.").queue();
                    }
                } else {
                    channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
                }
                break;
            case "getroles":
                ArrayList<String> administratorRoles = AdministratorsDatabaseHelper.getAllAdministratorsForGuild(guildId);
                if (administratorRoles.isEmpty()) {
                    channel.sendMessage("The only administrator is the owner.").queue();
                } else {
                    final String[] administratorRolesFormatted = {""};
                    administratorRoles.forEach(string -> administratorRolesFormatted[0] = administratorRolesFormatted[0].concat(string + "\n"));
                    administratorRolesFormatted[0] = administratorRolesFormatted[0].replaceAll("[\n]*$", "");
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("Administrator Roles")
                            .setDescription(administratorRolesFormatted[0])
                            .setColor(Color.BLUE)
                            .build();
                    channel.sendMessage(embed).queue();
                }
                break;
            default:
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdministratorsCommandHandler.class.getAnnotation(CommandHandler.class).usage() + "`").queue();
        }
    }
}
