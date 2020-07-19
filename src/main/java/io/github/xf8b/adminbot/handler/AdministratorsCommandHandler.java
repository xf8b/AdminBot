package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class AdministratorsCommandHandler extends CommandHandler {
    public AdministratorsCommandHandler() {
        super(
                "${prefix}administrators",
                "${prefix}administrators <action> [role]",
                "Adds to, removes from, or gets the list of administrator roles.",
                ImmutableMap.of(
                        "addrole", "Adds to the list of administrator roles.",
                        "removerole", "Removes from the list of administrator roles.",
                        "getroles", "Gets the list of administrator roles."
                ),
                ImmutableList.of("${prefix}admins"),
                CommandType.ADMINISTRATION
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            String guildId = guild.getId();
            if (content.split(" ").length < 1) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsage().replace("${prefix}", AdminBot.prefix) + "`.").queue();
                return;
            }
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
                    channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "administrators <action> [role]`.").queue();
                    break;
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
