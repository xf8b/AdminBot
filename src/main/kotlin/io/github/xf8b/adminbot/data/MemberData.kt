package io.github.xf8b.adminbot.data

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import io.github.xf8b.adminbot.helpers.WarnsDatabaseHelper
import io.github.xf8b.adminbot.util.LoggerDelegate
import io.github.xf8b.adminbot.util.PermissionUtil
import org.slf4j.Logger
import java.sql.SQLException
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION") //for the database helper deprecations
class MemberData(val guild: Guild, val memberId: Snowflake) {
    private val logger: Logger by LoggerDelegate()
    private var administratorLevel = 0
    private var warns: List<WarnContext> = ArrayList()

    init {
        administratorLevel = PermissionUtil.getAdministratorLevel(guild, guild.getMemberById(memberId).block()!!)
        try {
            warns = WarnsDatabaseHelper.getWarnsForUser(guild.id.asLong(), memberId.asLong())
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read from the warns database!", exception)
        }
    }

    companion object {
        private val MEMBER_DATA_CACHE: LoadingCache<Pair<Guild, Snowflake>, MemberData> = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build { pair: Pair<Guild, Snowflake> -> MemberData(pair.first, pair.second) }

        @JvmStatic
        fun getMemberData(guild: Guild, memberId: Snowflake): MemberData = MEMBER_DATA_CACHE.get(guild to memberId)!!
    }

    private fun readWarns(): List<WarnContext> {
        try {
            warns = WarnsDatabaseHelper.getWarnsForUser(guild.id.asLong(), memberId.asLong())
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to read from the warns database!", exception)
        }
        return warns
    }

    fun isAdministrator(): Boolean = getAdministratorLevel() > 0

    fun getAdministratorLevel(): Int {
        administratorLevel = PermissionUtil.getAdministratorLevel(
                guild,
                guild.getMemberById(memberId).block()!!
        )
        return administratorLevel
    }

    fun getWarns(): List<WarnContext> = readWarns()

    fun addWarn(memberWhoWarnedId: Snowflake, warnId: Int, reason: String) {
        try {
            WarnsDatabaseHelper.add(guild.id.asLong(), memberId.asLong(), memberWhoWarnedId.asLong(), warnId, reason)
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to write to the warns database!", exception)
        }
        readWarns()
    }

    fun removeWarn(memberWhoWarnedId: Snowflake?, warnId: Int, reason: String?) {
        try {
            WarnsDatabaseHelper.remove(
                    guild.id.asLong(),
                    memberId.asLong(),
                    memberWhoWarnedId?.asLong() ?: -1,
                    warnId,
                    reason
            )
        } catch (exception: SQLException) {
            logger.error("An exception happened while trying to write to the warns database!", exception)
        }
        readWarns()
    }

    fun hasWarn(reason: String): Boolean = readWarns().any { warnContext: WarnContext ->
        warnContext.reason == reason
    }
}