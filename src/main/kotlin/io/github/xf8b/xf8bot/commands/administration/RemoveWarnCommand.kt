/*
 * Copyright (c) 2020 xf8b.
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
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.delete.DeleteAction
import io.github.xf8b.xf8bot.database.actions.find.SelectAction
import io.github.xf8b.xf8bot.util.InputParsing.parseUserId
import io.github.xf8b.xf8bot.util.toImmutableList
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Flux
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
        val userId = Mono.justOrEmpty(event.getValueOfFlag(MEMBER)).flatMap { member ->
            parseUserId(event.guild, member)
                .map { it.toSnowflake() }
                .switchIfEmpty(event.channel.flatMap {
                    it.createMessage("The member does not exist!")
                }.then().cast())
        }
        val warnerId = Mono.justOrEmpty(event.getValueOfFlag(WARNER)).flatMap { member ->
            parseUserId(event.guild, member)
                .map { it.toSnowflake() }
                .switchIfEmpty(event.channel.flatMap {
                    it.createMessage("The member who warned does not exist!")
                }.then().cast())
        }
        val reason = event.getValueOfFlag(REASON).toValueOrNull()
        val warnId = event.getValueOfFlag(WARN_ID).toValueOrNull()
        val filterMono = userId.map {
            mapOf("guildId" to event.guildId.get().asLong(), "memberId" to it.asLong())
        }.defaultIfEmpty(mapOf("guildId" to event.guildId.get().asLong()))
        val warns = filterMono.flatMapMany { filter ->
            event.xf8bot
                .botDatabase
                .execute(SelectAction("warns", listOf("*"), filter))
                .flatMapMany { it.toFlux() }
        }
        return Mono.zip(
            { array: Array<*> -> array.toList().all { it as Boolean } },
            (warnId == null).toMono(),
            warnerId.flux().count().map { it == 0L },
            userId.flux().count().map { it == 0L },
            (reason == null).toMono(),
        ).filter { it }
            .switchIfEmpty(event.channel.flatMap {
                it.createMessage("You must have at least 1 search query!")
            }.then().cast())
            .flatMap {
                if (reason != null && reason.equals("all", ignoreCase = true)) {
                    userId.flatMap { userId ->
                        warns
                            .flatMap { it.map { row, _ -> row } }
                            .flatMap {
                                event.xf8bot
                                    .botDatabase
                                    .execute(
                                        DeleteAction(
                                            "warns", mapOf(
                                                "guildId" to it["guildId", Long::class.java],
                                                "memberId" to it["guildId", Long::class.java],
                                                "warnerId" to it["guildId", Long::class.java],
                                                "warnId" to it["guildId", String::class.java],
                                                "reason" to it["guildId", String::class.java]
                                            )
                                        )
                                    )
                                    .toMono()
                            }
                            .flatMap {
                                event.guild.flatMap {
                                    it.getMemberById(userId)
                                }
                            }
                            .flatMap { member ->
                                event.channel.flatMap {
                                    it.createMessage("Successfully removed warn(s) for ${member.displayName}.")
                                }
                            }
                            .switchIfEmpty(event.channel.flatMap {
                                it.createMessage("Cannot remove all warns without a user!")
                            })
                            .then()
                    }
                } else {
                    val warnsToDeleteCriteria = mutableListOf<Mono<Pair<String, Any>>>()

                    warnId?.let { warnsToDeleteCriteria.add(("warnId" to it).toMono()) }
                    reason?.let { warnsToDeleteCriteria.add(("reason" to it).toMono()) }
                    warnsToDeleteCriteria.add(userId.map { "memberId" to it.asLong() })
                    warnsToDeleteCriteria.add(warnerId.map { "warnerId" to it.asLong() })

                    Flux.zip(warnsToDeleteCriteria) {
                        val map = mutableMapOf<String, Any>()

                        for (any in it) {
                            val pair = any as Pair<*, *>

                            map[pair.first as String] = pair.second as Any
                        }

                        map
                    }.flatMap { criteria ->
                        event.xf8bot
                            .botDatabase
                            .execute(DeleteAction("warns", criteria))
                    }.cast(Any::class.java)
                        .flatMap {
                            //context.guild.flatMap { it.getMemberById(memberId) }.flatMap { member ->
                            event.channel.flatMap {
                                it.createMessage("Successfully removed warn(s)!") // for ${member.displayName}.")
                            }
                            //}
                        }
                        .switchIfEmpty(event.channel.flatMap {
                            it.createMessage("The user does not have a warn with that reason!")
                        }).then()
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