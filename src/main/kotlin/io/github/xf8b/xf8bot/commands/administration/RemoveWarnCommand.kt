/*
 * Copyright (c) 2020, 2021 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import discord4j.common.util.Snowflake
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.delete.RemoveWarnAction
import io.github.xf8b.xf8bot.database.actions.find.FindWarnsAction
import io.github.xf8b.xf8bot.util.*
import io.r2dbc.spi.Result
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.util.function.Tuples
import java.util.*

class RemoveWarnCommand : Command(
    name = "\${prefix}removewarn",
    description = """
    Removes the specified member's warns with the warnId and reason provided.
    If the reason is all, all warns will be removed. The warnId is not needed.
    If the warnId is all, all warns with the same reason will be removed.
    """.trimIndent(),
    commandType = CommandType.ADMINISTRATION,
    aliases = ("\${prefix}removewarns" to "\${prefix}rmwarn" and "\${prefix}rmwarns").toImmutableList(),
    flags = ImmutableList.of(MEMBER, WARNER, REASON, WARN_ID),
    administratorLevelRequired = 1
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId = event.guildId.get()

        val memberIdMono = event[MEMBER].toMono().flatMap { member ->
            InputParsing.parseUserId(event.guild, member)
                .map(Long::toSnowflake)
                .switchIfEmpty(event.channel
                    .flatMap { it.createMessage("No member found! This may be caused by 2+ people having the same username or nickname.") }
                    .then() // yes i know, very hacky
                    .cast())
        }

        val warnerIdMono = event[WARNER].toMono().flatMap { member ->
            InputParsing.parseUserId(event.guild, member)
                .map(Long::toSnowflake)
                .switchIfEmpty(event.channel
                    .flatMap { it.createMessage("No member found! This may be caused by 2+ people having the same username or nickname.") }
                    .then() // yes i know, very hacky
                    .cast())
        }

        val reason = event[REASON]
        val warnId = event[WARN_ID]

        val warns = memberIdMono.flatMap { event.xf8bot.botDatabase.execute(FindWarnsAction(guildId, memberId = it)) }
            .switchIfEmpty(event.xf8bot.botDatabase.execute(FindWarnsAction(guildId)))
            .flatMapMany(List<Result>::toFlux)

        return Mono.zip(
            { array -> array.cast<Boolean>().any { present -> present } },
            Mono.just(warnId != null),
            warnerIdMono.flux().count().map(1L::equals),
            memberIdMono.flux().count().map(1L::equals),
            Mono.just(reason != null),
        ).filter { it } // filter if none are present
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("You must have at least 1 search query!") }
                .then()
                .cast())
            .flatMap {
                if (reason != null && reason.equals("all", ignoreCase = true)) {
                    memberIdMono.switchIfEmpty(event.channel
                        .flatMap { it.createMessage("Cannot remove all warns without a user!") }
                        .then()
                        .cast())
                        .flatMap { _ ->
                            warns.flatMap { result ->
                                result.map { row, _ ->
                                    event.xf8bot.botDatabase.execute(
                                        RemoveWarnAction(
                                            guildId = (row["guildId", Long.JAVA_TYPE] as Long).toSnowflake(),
                                            memberId = (row["memberId", Long.JAVA_TYPE] as Long).toSnowflake(),
                                            warnerId = (row["warnerId", Long.JAVA_TYPE] as Long).toSnowflake(),
                                            warnId = row["warnId", UUID::class.java],
                                            reason = row["reason", String::class.java]
                                        )
                                    )
                                }
                            }.then(event.channel
                                .flatMap { it.createMessage("Successfully removed all warns!") }
                                .then())
                        }
                } else {
                    Mono.zip(memberIdMono, warnerIdMono)
                        .defaultIfEmpty(Tuples.of(0L.toSnowflake(), 0L.toSnowflake()))
                        .flatMap { ids ->
                            val (memberId, warnerId) = ids.cast<Snowflake>().map { snowflake ->
                                if (snowflake.asLong() == 0L) null else snowflake
                            }

                            event.xf8bot.botDatabase.execute(
                                RemoveWarnAction(
                                    guildId,
                                    memberId,
                                    warnerId,
                                    warnId?.toUuid(),
                                    reason
                                )
                            )
                        }
                        .then(event.channel
                            .flatMap { it.createMessage("Successfully removed warn(s)!") }
                            .then())
                }
            }
    }

    companion object {
        private const val UUID_REGEX = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b"
        private val MEMBER = StringFlag(
            shortName = "m",
            longName = "member",
            required = false
        )
        private val WARNER = StringFlag(
            shortName = "w",
            longName = "warner",
            required = false
        )
        private val REASON = StringFlag(
            shortName = "r",
            longName = "reason",
            required = false
        )
        private val WARN_ID = StringFlag(
            shortName = "i",
            longName = "warnId",
            validityPredicate = { it matches UUID_REGEX.toRegex() },
            errorMessageFunction = { "The warn ID must be a UUID!" },
            required = false
        )
    }
}