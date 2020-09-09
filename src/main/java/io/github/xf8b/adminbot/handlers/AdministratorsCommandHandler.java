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
import io.github.xf8b.adminbot.data.GuildData;
import io.github.xf8b.adminbot.settings.CommandHandlerChecks;
import io.github.xf8b.adminbot.settings.DisableChecks;
import io.github.xf8b.adminbot.util.MapUtil;
import io.github.xf8b.adminbot.util.ParsingUtil;
import io.github.xf8b.adminbot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;

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
                .setFlags(ROLE, ADMINISTRATOR_LEVEL)
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS))
                .setAdministratorLevelRequired(4));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        Guild guild = event.getGuild().block();
        String guildId = guild.getId().asString();
        Member member = event.getMember().get();
        String action = event.getValueOfArgument(ACTION).get();
        boolean isAdministrator = PermissionUtil.canMemberUseCommand(guild, member, this);
        switch (action) {
            case "add", "addrole" -> {
                if (isAdministrator) {
                    if (event.getValueOfFlag(ROLE).isEmpty() || event.getValueOfFlag(ADMINISTRATOR_LEVEL).isEmpty()) {
                        channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                        return;
                    }
                    Snowflake roleId = ParsingUtil.parseRoleIdAsSnowflake(guild, event.getValueOfFlag(ROLE).get());
                    if (roleId == null) {
                        channel.createMessage("The role does not exist!").block();
                        return;
                    }
                    String roleName = guild.getRoleById(roleId).map(Role::getName).block();
                    int level = event.getValueOfFlag(ADMINISTRATOR_LEVEL).get();
                    if (GuildData.getGuildData(guildId).hasAdministratorRole(roleId)) {
                        channel.createMessage("The role already has been added as an administrator role.").block();
                    } else {
                        GuildData.getGuildData(guildId).addAdministratorRole(roleId, level);
                        channel.createMessage("Successfully added " + roleName + " to the list of administrator roles.").block();
                    }
                } else {
                    channel.createMessage("Sorry, you don't have high enough permissions.").block();
                }
            }
            case "rm", "remove", "removerole" -> {
                if (isAdministrator) {
                    if (event.getValueOfFlag(ROLE).isEmpty()) {
                        channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
                        return;
                    }
                    Snowflake roleId = ParsingUtil.parseRoleIdAsSnowflake(guild, event.getValueOfFlag(ROLE).get());
                    if (roleId == null) {
                        channel.createMessage("The role does not exist!").block();
                        return;
                    }
                    String roleName = guild.getRoleById(roleId).map(Role::getName).block();
                    if (!GuildData.getGuildData(guildId).hasAdministratorRole(roleId)) {
                        channel.createMessage("The role has not been added as an administrator role!").block();
                    } else {
                        GuildData.getGuildData(guildId).removeAdministratorRole(roleId);
                        channel.createMessage("Successfully removed " + roleName + " from the list of administrator roles.").block();
                    }
                } else {
                    channel.createMessage("Sorry, you don't have high enough permissions.").block();
                }
            }
            case "rdr", "rmdel", "removedeletedroles" -> {
                if (isAdministrator) {
                    int amountOfRemovedRoles = 0;
                    GuildData guildData = GuildData.getGuildData(guildId);
                    for (Long roleId : GuildData.getGuildData(guildId).getAdministratorRoles().keySet()) {
                        if (guild.getRoleById(Snowflake.of(roleId)).blockOptional().isEmpty()) {
                            guildData.removeAdministratorRole(roleId);
                            amountOfRemovedRoles++;
                        }
                    }
                    channel.createMessage(String.format("Successfully removed %d deleted roles from the list of administrator roles.", amountOfRemovedRoles)).block();
                } else {
                    channel.createMessage("Sorry, you don't have high enough permissions.").block();
                }
            }
            case "ls", "list", "listroles", "get", "getroles" -> {
                Map<Long, Integer> administratorRoles = GuildData.getGuildData(guildId).getAdministratorRoles();
                if (administratorRoles.isEmpty()) {
                    channel.createMessage("The only administrator is the owner.").block();
                } else {
                    administratorRoles = MapUtil.sortByValue(administratorRoles);
                    Arrays.sort(administratorRoles.keySet().toArray(new Long[0]));
                    Arrays.sort(administratorRoles.values().toArray(new Integer[0]));
                    String roleNames = administratorRoles.keySet()
                            .stream()
                            .map(roleId -> "<@&" + roleId + ">")
                            .collect(Collectors.joining("\n"))
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
            }
            default -> channel.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(guildId) + "`.").block();
        }
    }
}
