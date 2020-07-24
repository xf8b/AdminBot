package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.util.MapUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

public class AdministratorsCommandHandler extends CommandHandler {
    public AdministratorsCommandHandler() {
        super(
                "${prefix}administrators",
                "${prefix}administrators <action> [role] [level]",
                "Adds to, removes from, or gets the list of administrator roles.\n" +
                        "The level can be from 1 to 3. \n" +
                        "Level 1 can use `warn`, `removewarn`, `warns`, `mute`, and `nickname`.\n" +
                        "Level 2 can use all the commands for level 1 and `kick` and `clear`.\n" +
                        "Level 3 can use all the commands for level 2 and `administrators`, `ban`, `unban`, and `prefix`.",
                ImmutableMap.of(
                        "addrole", "Adds to the list of administrator roles.",
                        "removerole", "Removes from the list of administrator roles.",
                        "removedeletedroles", "Removes deleted roles from the list of administrator roles.",
                        "getroles", "Gets the list of administrator roles."
                ),
                ImmutableList.of("${prefix}admins"),
                CommandType.ADMINISTRATION,
                3
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            Member member = event.getMember();
            String guildId = guild.getId();
            if (content.trim().split(" ").length < 2) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            String commandType = content.trim().split(" ")[1].replace(AdminBot.getInstance().prefix, "").toLowerCase();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, member) &&
                    PermissionUtil.getAdministratorLevel(guild, member) >= this.getLevelRequired();
            switch (commandType) {
                case "addRole":
                case "addrole":
                    if (isAdministrator) {
                        if (content.trim().split(" ").length < 4) {
                            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                            return;
                        }
                        String roleId = content.trim().split(" ")[2].replaceAll("[<@&|>]", "").trim();
                        String tempLevel = content.trim().split(" ")[3].trim();
                        int level;
                        try {
                            level = Integer.parseInt(tempLevel);
                        } catch (NumberFormatException exception) {
                            channel.sendMessage("The level is not a number!").queue();
                            return;
                        }
                        if (level > 3 || level < 1) {
                            channel.sendMessage("The level is not in bounds!").queue();
                            return;
                        }
                        try {
                            Long.parseLong(roleId);
                        } catch (NumberFormatException exception) {
                            channel.sendMessage("The role does not exist!").queue();
                            return;
                        }
                        if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                            channel.sendMessage("The role already has been added as an administrator role.").queue();
                        } else {
                            AdministratorsDatabaseHelper.addToAdministrators(guildId, roleId, level);
                            channel.sendMessage("Successfully added <@&" + roleId + "> to the list of administrator roles.").queue();
                        }
                    } else {
                        channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
                    }
                    break;
                case "removeRole":
                case "removerole":
                    if (isAdministrator) {
                        if (content.trim().split(" ").length < 3) {
                            channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                            return;
                        }
                        String roleId = content.trim().split(" ")[2].replaceAll("[<@!&>]", "").trim();
                        try {
                            Long.parseLong(roleId);
                        } catch (NumberFormatException exception) {
                            channel.sendMessage("The role does not exist!").queue();
                            return;
                        }
                        if (!AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                            channel.sendMessage("The role has not been added as an administrator role!").queue();
                        } else {
                            AdministratorsDatabaseHelper.removeFromAdministrators(guildId, roleId);
                            channel.sendMessage("Successfully removed <@&" + roleId + "> from the list of administrator roles.").queue();
                        }
                    } else {
                        channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
                    }
                    break;
                case "removeDeletedRoles":
                case "removedeletedroles":
                    if (isAdministrator) {
                        for (String string : AdministratorsDatabaseHelper.getAllAdministratorsForGuild(guildId).keySet()) {
                            if (guild.getRoleById(string.replaceAll("[<@&>]", "")) == null) {
                                AdministratorsDatabaseHelper.removeFromAdministrators(guildId, string.replaceAll("[<@&>]", ""));
                            }
                        }
                        channel.sendMessage("Successfully removed deleted roles from the list of administrator roles.").queue();
                    } else {
                        channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
                    }
                    break;
                case "getRoles":
                case "getroles":
                    Map<String, Integer> administratorRoles = AdministratorsDatabaseHelper.getAllAdministratorsForGuild(guildId);
                    if (administratorRoles.isEmpty()) {
                        channel.sendMessage("The only administrator is the owner.").queue();
                    } else {
                        administratorRoles = MapUtil.sortByValue(administratorRoles);
                        final String[] administratorRolesFormatted = {"", ""};
                        Arrays.sort(administratorRoles.keySet().toArray(new String[0]));
                        Arrays.sort(administratorRoles.values().toArray(new Integer[0]));
                        administratorRoles.forEach((roleMention, level) -> {
                            administratorRolesFormatted[0] = administratorRolesFormatted[0].concat(roleMention + "\n");
                            administratorRolesFormatted[1] = administratorRolesFormatted[1].concat(level + "\n");
                        });
                        administratorRolesFormatted[0] = administratorRolesFormatted[0].replaceAll("[\n]*$", "");
                        administratorRolesFormatted[1] = administratorRolesFormatted[1].replaceAll("[\n]*$", "");
                        MessageEmbed embed = new EmbedBuilder()
                                .setTitle("Administrator Roles")
                                .addField("Role", administratorRolesFormatted[0], true)
                                .addField("Level", administratorRolesFormatted[1], true)
                                .setColor(Color.BLUE)
                                .build();
                        channel.sendMessage(embed).queue();
                    }
                    break;
                default:
                    channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                    break;
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
