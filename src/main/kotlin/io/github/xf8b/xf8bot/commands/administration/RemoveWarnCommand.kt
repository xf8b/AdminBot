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
import io.github.xf8b.utils.optional.toNullable
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.delete.RemoveWarnAction
import io.github.xf8b.xf8bot.database.actions.find.FindWarnsAction
import io.github.xf8b.xf8bot.util.InputParsing.parseUserId
import io.github.xf8b.xf8bot.util.toImmutableList
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class RemoveWarnCommand : AbstractCommand(
    name = "\${prefix}removewarn",
    description = """
    Removes the specified member's warns with the warnId and reason provided.
    If the reason is all, all warns will be removed. The warnId is not needed.
    If the warnId is all, all warns with the same reason will be removed.
    """.trimIndent(),
    commandType = CommandType.ADMINISTRATION,
    aliases = ("\${prefix}removewarns" to "\${prefix}rmwarn" and "\${prefix}rmwarns").toImmutableList(),
    flags = ImmutableList.of(MEMBER, WARNER, REASON, WARN_ID),
    minimumAmountOfArgs = 2,
    administratorLevelRequired = 1
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val memberIdMono = Mono.justOrEmpty(event.getValueOfFlag(MEMBER)).flatMap { member ->
            parseUserId(event.guild, member)
                .map { it.toSnowflake() }
                .switchIfEmpty(event.channel
                    .flatMap { it.createMessage("The member does not exist!") }
                    .then()
                    .cast())
        }
        val warnerIdMono = Mono.justOrEmpty(event.getValueOfFlag(WARNER)).flatMap { member ->
            parseUserId(event.guild, member)
                .map { it.toSnowflake() }
                .switchIfEmpty(event.channel
                    .flatMap { it.createMessage("The member who warned does not exist!") }
                    .then()
                    .cast())
        }
        val reason = event.getValueOfFlag(REASON).toNullable()
        val warnId = event.getValueOfFlag(WARN_ID).toNullable()
        val warns = memberIdMono
            .flatMap { memberId ->
                event.xf8bot
                    .botDatabase
                    .execute(FindWarnsAction(guildId = event.guildId.get(), memberId = memberId))
            }
            .switchIfEmpty(
                event.xf8bot
                    .botDatabase
                    .execute(FindWarnsAction(guildId = event.guildId.get()))
            )
            .flatMapMany { it.toFlux() }
        return Mono.zip(
            { array -> array.toList().map { it as Boolean }.all { it } },
            (warnId == null).toMono(),
            warnerIdMono.flux().count().map { it == 0L },
            memberIdMono.flux().count().map { it == 0L },
            (reason == null).toMono(),
        ).filter { it }
            .switchIfEmpty(event.channel
                .flatMap { it.createMessage("You must have at least 1 search query!") }
                .then()
                .cast())
            .flatMap {
                if (reason != null && reason.equals("all", ignoreCase = true)) {
                    memberIdMono
                        .flatMap { _ ->
                            warns.flatMap { it.map { row, _ -> row } }
                                .flatMap {
                                    event.xf8bot.botDatabase
                                        .execute(
                                            RemoveWarnAction(
                                                guildId = (it["guildId", java.lang.Long::class.java] as Long).toSnowflake(),
                                                memberId = (it["memberId", java.lang.Long::class.java] as Long).toSnowflake(),
                                                warnerId = (it["warnerId", java.lang.Long::class.java] as Long).toSnowflake(),
                                                warnId = it["warnId", String::class.java],
                                                reason = it["reason", String::class.java]
                                            )
                                        )
                                        .toMono()
                                }
                                .then(event.channel.flatMap {
                                    it.createMessage("Successfully removed all warns!")
                                })
                        }
                        .switchIfEmpty(event.channel.flatMap {
                            it.createMessage("Cannot remove all warns without a user!")
                        })
                        .then()
                } else {
                    Mono.zip(memberIdMono, warnerIdMono)
                        .flatMap {
                            event.xf8bot
                                .botDatabase
                                .execute(
                                    RemoveWarnAction(
                                        guildId = event.guildId.get(),
                                        memberId = it.t1,
                                        warnerId = it.t2,
                                        warnId = warnId,
                                        reason = reason
                                    )
                                )
                        }
                        .then(event.channel.flatMap {
                            it.createMessage("Successfully removed warn(s)!")
                        })
                        .then()
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