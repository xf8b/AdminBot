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

package io.github.xf8b.adminbot.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import io.github.xf8b.adminbot.helpers.AdministratorsDatabaseHelper;
import lombok.experimental.UtilityClass;

import java.sql.SQLException;

@UtilityClass
@Slf4j
public class PermissionUtil {
    public Boolean isAdministrator(Guild guild, Member member) {
        if (member.getId().equals(guild.getOwnerId())) return true;
        String guildId = guild.getId().asString();
        return member.getRoles().map(Role::getId).map(Snowflake::asString).any(roleId -> {
            try {
                return AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId);
            } catch (ClassNotFoundException | SQLException exception) {
                LOGGER.error("An exception happened while trying to read from the administrators database!", exception);
            }
            return false;
        }).block();
    }

    public Integer getAdministratorLevel(Guild guild, Member member) {
        if (member.getId().equals(guild.getOwnerId())) return 3;
        String guildId = guild.getId().asString();
        return member.getRoles().map(Role::getId).map(Snowflake::asString).map(roleId -> {
            try {
                if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                    return AdministratorsDatabaseHelper.getLevelOfAdministratorRole(guildId, roleId);
                }
                return 0;
            } catch (ClassNotFoundException | SQLException exception) {
                LOGGER.error("An exception happened while trying to read from the administrators database!", exception);
            }
            return 0;
        }).sort().blockLast();
    }
}
