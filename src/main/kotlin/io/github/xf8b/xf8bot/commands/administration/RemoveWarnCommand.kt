/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.commands.administration

import com.google.common.collect.ImmutableList
import com.mongodb.client.model.Filters
import io.github.xf8b.utils.optional.toValueOrNull
import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.DeleteDocumentAction
import io.github.xf8b.xf8bot.database.actions.FindAllMatchingAction
import io.github.xf8b.xf8bot.util.ParsingUtil.parseUserId
import io.github.xf8b.xf8bot.util.toImmutableList
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
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
    flags = ImmutableList.of(MEMBER, MEMBER_WHO_WARNED, REASON, WARN_ID),
    minimumAmountOfArgs = 2,
    administratorLevelRequired = 1
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val userId = Mono.justOrEmpty(context.getValueOfFlag(MEMBER)).flatMap { member ->
            parseUserId(context.guild, member)
                .map { it.toSnowflake() }
                .switchIfEmpty(context.channel.flatMap {
                    it.createMessage("The member does not exist!")
                }.then().cast())
        }
        val memberWhoWarnedId = Mono.justOrEmpty(context.getValueOfFlag(MEMBER_WHO_WARNED)).flatMap { member ->
            parseUserId(context.guild, member)
                .map { it.toSnowflake() }
                .switchIfEmpty(context.channel.flatMap {
                    it.createMessage("The member who warned does not exist!")
                }.then().cast())
        }
        val reason = context.getValueOfFlag(REASON).toValueOrNull()
        val warnId = context.getValueOfFlag(WARN_ID).toValueOrNull()
        val filter = userId.map {
            Filters.and(
                Filters.eq("guildId", context.guildId.get().asLong()),
                Filters.eq("userId", it.asLong())
            )
        }.defaultIfEmpty(Filters.eq("guildId", context.guildId.get().asLong()))
        val warnDocuments = filter.flatMapMany {
            context.xf8bot
                .botMongoDatabase
                .execute(FindAllMatchingAction("warns", it))
        }
        return Mono.zip(
            (warnId == null).toMono(),
            memberWhoWarnedId.flux().count().map { it == 0L },
            userId.flux().count().map { it == 0L },
            (reason == null).toMono()
        ).filter { !it.t1 && !it.t2 && !it.t3 && !it.t4 }
            .flatMap {
                if (reason != null && reason.equals("all", ignoreCase = true)) {
                    userId.flatMap { userId ->
                        warnDocuments
                            .flatMap {
                                context.xf8bot
                                    .botMongoDatabase
                                    .execute(DeleteDocumentAction("warns", it))
                                    .toMono()
                            }
                            .flatMap {
                                context.guild.flatMap {
                                    it.getMemberById(userId)
                                }
                            }
                            .flatMap { member ->
                                context.channel.flatMap {
                                    it.createMessage("Successfully removed warn(s) for ${member.displayName}.")
                                }
                            }
                            .switchIfEmpty(context.channel.flatMap {
                                it.createMessage("Cannot remove all warns without a user!")
                            })
                            .then()
                    }
                } else {
                    warnDocuments
                        .filter { document ->
                            warnId?.let { document.getString("warnId") == it } ?: true
                        }
                        .filter { document ->
                            reason?.let { document.getString("reason") == it } ?: true
                        }
                        .filterWhen { document ->
                            userId.map {
                                document.getLong("userId") == it.asLong()
                            }.defaultIfEmpty(true)
                        }
                        .filterWhen { document ->
                            memberWhoWarnedId.map {
                                document.getLong("memberWhoWarnedId") == it.asLong()
                            }.defaultIfEmpty(true)
                        }
                        .flatMap {
                            context.xf8bot
                                .botMongoDatabase
                                .execute(DeleteDocumentAction("warns", it))
                        }
                        .cast(Any::class.java)
                        .flatMap { /*guild.getMemberById(userId).flatMap { member -> */
                            context.channel.flatMap {
                                it.createMessage("Successfully removed warn(s)!" /*for ${member.getDisplayName()}."*/)
                            }
                        }
                        .switchIfEmpty(context.channel.flatMap {
                            it.createMessage("The user does not have a warn with that reason!")
                        })
                        .then()
                }
            }
            .switchIfEmpty(context.channel.flatMap {
                it.createMessage("You must have at least 1 search query!")
            }.then())

    }

    companion object {
        private val UUID_REGEX = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b".toRegex()
        private val MEMBER = StringFlag(
            shortName = "m",
            longName = "member",
            required = false
        )
        private val MEMBER_WHO_WARNED = StringFlag(
            shortName = "mww",
            longName = "memberWhoWarned",
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
            validityPredicate = { it matches UUID_REGEX },
            invalidValueErrorMessageFunction = { "The warn ID must be a UUID!" },
            required = false
        )
    }
}