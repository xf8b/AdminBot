package io.github.xf8b.adminbot.util

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler
import io.github.xf8b.adminbot.data.GuildData
import io.github.xf8b.adminbot.data.MemberData

class PermissionUtil {
    companion object {
        @JvmStatic
        fun isMemberHigher(guild: Guild, member: Member, otherMember: Member): Boolean {
            val memberData = MemberData.getMemberData(guild, member.id)
            val otherMemberData = MemberData.getMemberData(guild, otherMember.id)
            return memberData.getAdministratorLevel() > otherMemberData.getAdministratorLevel()
        }

        @JvmStatic
        fun canMemberUseCommand(guild: Guild, member: Member, commandHandler: AbstractCommandHandler): Boolean = MemberData.getMemberData(
                guild,
                member.id
        ).getAdministratorLevel() >= commandHandler.administratorLevelRequired

        @Deprecated(message = "Use MemberData#isAdministrator!", replaceWith = ReplaceWith(
                "MemberData#isAdministrator",
                "io.github.xf8b.adminbot.data.MemberData"
        ))
        @JvmStatic
        fun isAdministrator(guild: Guild, member: Member): Boolean {
            if (member.id == guild.ownerId) return true
            val guildId = guild.id.asString()
            return member.roles.map(Role::getId).any { roleId: Snowflake ->
                GuildData.getGuildData(guildId).hasAdministratorRole(roleId)
            }.block()!!
        }

        @Deprecated(message = "Use MemberData#getAdministratorLevel!", replaceWith = ReplaceWith(
                "MemberData#getAdministratorLevel",
                "io.github.xf8b.adminbot.data.MemberData"
        ))
        @JvmStatic
        fun getAdministratorLevel(guild: Guild, member: Member): Int {
            if (member.id == guild.ownerId) return 4
            val guildId = guild.id.asString()
            return member.roles.map(Role::getId).map { roleId: Snowflake ->
                if (GuildData.getGuildData(guildId).hasAdministratorRole(roleId)) {
                    GuildData.getGuildData(guildId).getLevelOfAdministratorRole(roleId)
                } else {
                    0
                }
            }.sort().blockLast()!!
        }
    }
}