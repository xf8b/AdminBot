package io.github.xf8b.adminbot.util;

import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.sql.SQLException;

public class PermissionUtil {
    public static boolean isAdministrator(Guild guild, Member member) throws SQLException, ClassNotFoundException {
        boolean isAdministrator = false;
        String guildId = guild.getId();
        for (Role role : member.getRoles()) {
            String roleId = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                isAdministrator = true;
            }
        }
        if (member.isOwner()) isAdministrator = true;
        return isAdministrator;
    }

    public static int getAdministratorLevel(Guild guild, Member member) throws SQLException, ClassNotFoundException {
        int level = 0;
        String guildId = guild.getId();
        for (Role role : member.getRoles()) {
            String roleId = role.getId();
            if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, roleId)) {
                int tempLevel = AdministratorsDatabaseHelper.getLevelOfAdministratorRole(guildId, roleId);
                if (tempLevel > level) {
                    level = tempLevel;
                }
            }
        }
        if (member.isOwner()) level = 3;
        return level;
    }
}
