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
import com.google.common.collect.Range;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import io.github.xf8b.adminbot.api.commands.flags.Flag;
import io.github.xf8b.adminbot.api.commands.flags.IntegerFlag;
import io.github.xf8b.adminbot.api.commands.flags.StringFlag;
import io.github.xf8b.adminbot.helpers.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.settings.CommandHandlerChecks;
import io.github.xf8b.adminbot.settings.DisableChecks;
import io.github.xf8b.adminbot.util.MapUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@DisableChecks(CommandHandlerChecks.IS_ADMINISTRATOR)
@Slf4j
public class AdministratorsCommandHandler extends AbstractCommandHandler {
    private static final StringArgument ACTION = StringArgument.builder()
            .setIndex(Range.singleton(1))
            .setName("action")
            .build();
    private static final StringFlag ROLE = StringFlag.builder()
            .setShortName("r")
            .setLongName("role")
            .setRequired(false)
            .build();
    private static final IntegerFlag ADMINISTRATOR_LEVEL = IntegerFlag.builder()
            .setShortName("l")
            .setLongName("level")
            .setRequired(false)
            .setValidityPredicate(value -> {
                try {
                    int level = Integer.parseInt(value);
                    return level <= 4 && level >= 1;
                } catch (NumberFormatException exception) {
                    return false;
                }
            })
            .setInvalidValueErrorMessageFunction(invalidValue -> {
                try {
                    int level = Integer.parseInt(invalidValue);
                    if (level > 4) {
                        return "The maximum administrator level you can assign is 4!";
                    } else if (level < 1) {
                        return "The minimum administrator level you can assign is 1!";
                    } else {
                        throw new IllegalStateException("tf");
                    }
                } catch (NumberFormatException exception) {
                    return Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE;
                }
            })
            .build();

    public AdministratorsCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}administrators")
                .setDescription("Adds to, removes from, or gets the list of administrator roles.\n" +
                        "The level can be from 1 to 4. \n" +
                        "Level 1 can use `warn`, `removewarn`, `warns`, `mute`, and `nickname`.\n" +
                        "Level 2 can use all the commands for level 1 and `kick` and `clear`.\n" +
                        "Level 3 can use all the commands for level 2 and `ban`, `unban`, and `automod`.\n" +
                        "Level 4 can use all the commands for level 3 and `administrators`, and `prefix`. This is intended for administrator/owner roles!")
                .setCommandType(CommandType.ADMINISTRATION)
                .setActions(ImmutableMap.of(
                        "addrole", "Adds to the list of administrator roles.",
                        "removerole", "Removes from the list of administrator roles.",
                        "removedeletedroles", "Removes deleted roles from the list of administrator roles.",
                        "getroles", "Gets the list of administrator roles."))
                .addAlias("${prefix}admins")
                .setMinimumAmountOfArgs(1)
                .addArgument(ACTION)
                .setFlags(ImmutableList.of(ROLE, ADMINISTRATOR_LEVEL))
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS))
                .setAdministratorLevelRequired(4));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        try {
            MessageChannel channel = event.getChannel().block();
            Guild guild = event.getGuild().block();
            String guildId = guild.getId().asString();
            Member member = event.getMember().get();
            String action = event.getValueOfArgument(ACTION);
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, member) &&
                    PermissionUtil.getAdministratorLevel(guild, member) >= this.getAdministratorLevelRequired();
            switch (action) {
                case "add":
                case "addrole":
                    if (isAdministrator) {
                        if (event.getValueOfFlag(ROLE) == null || event.getValueOfFlag(ADMINISTRATOR_LEVEL) == null) {
                            channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                            return;
                        }
                        Snowflake roleId = ParsingUtil.parseRoleIdAndReturnSnowflake(guild, event.getValueOfFlag(ROLE));
                        if (roleId == null) {
                            channel.createMessage("The role does not exist!").block();
                            return;
                        }
                        String roleName = guild.getRoleById(roleId).map(Role::getName).block();
                        int level = event.getValueOfFlag(ADMINISTRATOR_LEVEL);
                        if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId.asString())) {
                            channel.createMessage("The role already has been added as an administrator role.").block();
                        } else {
                            AdministratorsDatabaseHelper.addToAdministrators(guildId, roleId.asString(), level);
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
                        if (event.getValueOfFlag(ROLE) == null) {
                            channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                            return;
                        }
                        Snowflake roleId = ParsingUtil.parseRoleIdAndReturnSnowflake(guild, event.getValueOfFlag(ROLE));
                        if (roleId == null) {
                            channel.createMessage("The role does not exist!").block();
                            return;
                        }
                        String roleName = guild.getRoleById(roleId).map(Role::getName).block();
                        if (!AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId.asString())) {
                            channel.createMessage("The role has not been added as an administrator role!").block();
                        } else {
                            AdministratorsDatabaseHelper.removeFromAdministrators(guildId, roleId.asString());
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
            LOGGER.error("An exception happened while trying to read/write to/from the administrators database!", exception);
        }
    }
}
