/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.handlers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.helpers.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.settings.CommandHandlerChecks;
import io.github.xf8b.adminbot.settings.DisableChecks;
import io.github.xf8b.adminbot.settings.GuildSettings;
import io.github.xf8b.adminbot.util.MapUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@DisableChecks(disabledChecks = CommandHandlerChecks.IS_ADMINISTRATOR)
public class AdministratorsCommandHandler extends AbstractCommandHandler {
    public AdministratorsCommandHandler() {
        super(
                "${prefix}administrators",
                "${prefix}administrators <action> [role] [level]",
                "Adds to, removes from, or gets the list of administrator roles.\n" +
                        "The level can be from 1 to 3. \n" +
                        "Level 1 can use `warn`, `removewarn`, `warns`, `mute`, and `nickname`.\n" +
                        "Level 2 can use all the commands for level 1 and `kick` and `clear`.\n" +
                        "Level 3 can use all the commands for level 2 and `administrators`, `ban`, `unban`, `automod`, and `prefix`.",
                ImmutableMap.of(
                        "addrole", "Adds to the list of administrator roles.",
                        "removerole", "Removes from the list of administrator roles.",
                        "removedeletedroles", "Removes deleted roles from the list of administrator roles.",
                        "getroles", "Gets the list of administrator roles."
                ),
                ImmutableList.of("${prefix}admins"),
                CommandType.ADMINISTRATION,
                1,
                PermissionSet.of(Permission.EMBED_LINKS),
                3
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        try {
            String content = event.getMessage().getContent();
            MessageChannel channel = event.getChannel().block();
            Guild guild = event.getGuild().block();
            String guildId = guild.getId().asString();
            Member member = event.getMember().get();
            String commandType = content.trim().split(" ")[1].replace(GuildSettings.getGuildSettings(guildId).getPrefix(), "").toLowerCase();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, member) &&
                    PermissionUtil.getAdministratorLevel(guild, member) >= this.getLevelRequired();
            switch (commandType.toLowerCase()) {
                case "add":
                case "addrole":
                    if (isAdministrator) {
                        if (content.trim().split(" ").length < 4) {
                            channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                            return;
                        }
                        String roleId = String.valueOf(ParsingUtil.parseRoleId(guild, content.trim().substring(content.trim().indexOf(" ", content.trim().indexOf(" ")) + 1, content.trim().lastIndexOf(" ")).trim()));
                        if (roleId == null) {
                            channel.createMessage("The role does not exist!").block();
                            return;
                        }
                        String roleName = guild.getRoleById(Snowflake.of(roleId)).map(Role::getName).block();
                        String tempLevel = content.trim().substring(content.trim().lastIndexOf(" ")).trim();
                        int level;
                        try {
                            level = Integer.parseInt(tempLevel);
                        } catch (NumberFormatException exception) {
                            channel.createMessage("The level is not a number!").block();
                            return;
                        }
                        if (level > 3 || level < 1) {
                            channel.createMessage("The level is not in bounds!").block();
                            return;
                        }
                        if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                            channel.createMessage("The role already has been added as an administrator role.").block();
                        } else {
                            AdministratorsDatabaseHelper.addToAdministrators(guildId, roleId, level);
                            channel.createMessage("Successfully added " + roleName + " to the list of administrator roles.").block();
                        }
                    } else {
                        channel.createMessage("Sorry, you don't have high enough permissions.").block();
                    }
                    break;
                case "rm":
                case "remove":
                case "removerole":
                    if (isAdministrator) {
                        if (content.trim().split(" ").length < 3) {
                            channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                            return;
                        }
                        String roleId = String.valueOf(ParsingUtil.parseRoleId(guild, content.trim().substring(content.trim().indexOf(" ", content.trim().indexOf(" ")) + 1).trim()));
                        if (roleId == null) {
                            channel.createMessage("The role does not exist!").block();
                            return;
                        }
                        String roleName = guild.getRoleById(Snowflake.of(roleId)).map(Role::getName).block();
                        if (!AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                            channel.createMessage("The role has not been added as an administrator role!").block();
                        } else {
                            AdministratorsDatabaseHelper.removeFromAdministrators(guildId, roleId);
                            channel.createMessage("Successfully removed " + roleName + " from the list of administrator roles.").block();
                        }
                    } else {
                        channel.createMessage("Sorry, you don't have high enough permissions.").block();
                    }
                    break;
                case "rdm":
                case "rmdel":
                case "removedeletedroles":
                    if (isAdministrator) {
                        for (String string : AdministratorsDatabaseHelper.getAllAdministratorsForGuild(guildId).keySet()) {
                            if (guild.getRoleById(Snowflake.of(string.replaceAll("[<@&>]", ""))).block() == null) {
                                AdministratorsDatabaseHelper.removeFromAdministrators(guildId, string.replaceAll("[<@&>]", ""));
                            }
                        }
                        channel.createMessage("Successfully removed deleted roles from the list of administrator roles.").block();
                    } else {
                        channel.createMessage("Sorry, you don't have high enough permissions.").block();
                    }
                    break;
                case "ls":
                case "list":
                case "listroles":
                case "get":
                case "getroles":
                    Map<String, Integer> administratorRoles = AdministratorsDatabaseHelper.getAllAdministratorsForGuild(guildId);
                    if (administratorRoles.isEmpty()) {
                        channel.createMessage("The only administrator is the owner.").block();
                    } else {
                        administratorRoles = MapUtil.sortByValue(administratorRoles);
                        Arrays.sort(administratorRoles.keySet().toArray(new String[0]));
                        Arrays.sort(administratorRoles.values().toArray(new Integer[0]));
                        String roleNames = String.join("\n", administratorRoles.keySet())
                                .replaceAll("\n$", "");
                        String roleLevels = administratorRoles.values()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.joining("\n"))
                                .replaceAll("\n$", "");
                        channel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("Administrator Roles")
                                .addField("Role", roleNames, true)
                                .addField("Level", roleLevels, true)
                                .setColor(Color.BLUE))
                                .block();
                    }
                    break;
                default:
                    channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                    break;
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
