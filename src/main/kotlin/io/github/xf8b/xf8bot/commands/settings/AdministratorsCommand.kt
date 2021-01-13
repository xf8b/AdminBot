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

package io.github.xf8b.xf8bot.commands.settings

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Range
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Role
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.utils.exceptions.UnexpectedException
import io.github.xf8b.utils.sorting.sortByValue
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.*
import io.github.xf8b.xf8bot.database.actions.add.AddAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.delete.RemoveAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.find.FindAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.find.GetGuildAdministratorRolesAction
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.InputParsing.parseRoleId
import io.github.xf8b.xf8bot.util.PermissionUtil.canUse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.*

class AdministratorsCommand : Command(
    name = "\${prefix}administrators",
    description = """
    Adds to, removes from, or gets the list of administrator roles.
    The level can be from 1 to 4.
    Level 1 can use `warn`, `removewarn`, `warns`, `mute`, and `nickname`.
    Level 2 can use all the commands for level 1 and `kick` and `clear`.
    Level 3 can use all the commands for level 2 and `ban` and `unban`.
    Level 4 can use all the commands for level 3, `disable`, `enable`, `administrators`, and `prefix`. **This is intended for administrator/owner roles!**
    """.trimIndent(),
    commandType = CommandType.SETTINGS,
    actions = ImmutableMap.copyOf(
        mapOf(
            "add/addrole" to "Adds to the list of administrator roles.",
            "rm/remove/removerole" to "Removes from the list of administrator roles.",
            "ls/list/listroles/get/getroles" to "Gets the list of administrator roles."
        )
    ),
    aliases = "\${prefix}admins".toSingletonImmutableList(),
    arguments = ACTION.toSingletonImmutableList(),
    flags = (ROLE to ADMINISTRATOR_LEVEL).toImmutableList(),
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet(),
    disabledChecks = EnumSet.of(ExecutionChecks.IS_ADMINISTRATOR),
    administratorLevelRequired = 4
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId = event.guildId.get()
        val member = event.member.get()
        val action = event[ACTION]!!

        return event.guild.flatMap { guild ->
            val isAdministrator = member.canUse(event.xf8bot, guild, command = this)

            return@flatMap when (action.toLowerCase(Locale.ROOT)) {
                "add", "addrole" -> isAdministrator.filter { it }.flatMap ifAdministratorRun@{
                    if (event[ROLE] == null || event[ADMINISTRATOR_LEVEL] == null) {
                        return@ifAdministratorRun event.channel.flatMap {
                            getUsageWithPrefix(event.xf8bot, guildId.asString()).flatMap { usage ->
                                it.createMessage("Huh? Could you repeat that? The usage of this command is: `${usage}`.")
                            }
                        }.then()
                    }
                    parseRoleId(event.guild, event[ROLE]!!)
                        .map(Long::toSnowflake)
                        .switchIfEmpty(event.channel
                            .flatMap { it.createMessage("The role does not exist!") }
                            .then()
                            .cast())
                        .onErrorResume<IndexOutOfBoundsException, Snowflake> {
                            event.channel
                                .flatMap { it.createMessage("There are multiple roles with that name! Please use the ID or a mention instead.") }
                                .then()
                                .cast()
                        }
                        .flatMap { roleId: Snowflake ->
                            val level = event[ADMINISTRATOR_LEVEL]!!

                            event.xf8bot.botDatabase
                                .execute(FindAdministratorRoleAction(guildId, roleId))
                                .toMono()
                                .filter { it.isNotEmpty() }
                                .filterWhen { it[0].updatedRows }
                                .flatMap {
                                    event.channel.flatMap {
                                        it.createMessage("The role already has been added as an administrator role.")
                                    }
                                }
                                .switchIfEmpty(event.xf8bot.botDatabase
                                    .execute(AddAdministratorRoleAction(guildId, roleId, level))
                                    .toMono()
                                    .then(guild.getRoleById(roleId)
                                        .map { it.name }
                                        .flatMap { roleName: String ->
                                            event.channel.flatMap {
                                                it.createMessage("Successfully added $roleName to the list of administrator roles.")
                                            }
                                        })
                                )
                        }
                }.switchIfEmpty(event.channel.flatMap {
                    it.createMessage("Sorry, you don't have high enough permissions.")
                }).then()


                "rm", "remove", "removerole" -> isAdministrator.filter { it }.flatMap ifAdministratorRun@{
                    if (event[ROLE] == null) {
                        return@ifAdministratorRun event.channel.flatMap {
                            getUsageWithPrefix(event.xf8bot, guildId.asString()).flatMap { usage ->
                                it.createMessage("Huh? Could you repeat that? The usage of this command is: `${usage}`.")
                            }
                        }
                    }

                    parseRoleId(event.guild, event[ROLE]!!)
                        .map(Long::toSnowflake)
                        .switchIfEmpty(event.channel
                            .flatMap { it.createMessage("The role does not exist!") }
                            .then()
                            .cast())
                        .onErrorResume<IndexOutOfBoundsException, Snowflake> {
                            event.channel
                                .flatMap { it.createMessage("There are multiple roles with that name! Please use the ID or a mention instead.") }
                                .then()
                                .cast()
                        }
                        .flatMap { roleId: Snowflake ->
                            event.xf8bot.botDatabase
                                .execute(FindAdministratorRoleAction(guildId, roleId))
                                .filter { it.isNotEmpty() }
                                .filterWhen { it[0].updatedRows }
                                .flatMap {
                                    event.xf8bot.botDatabase
                                        .execute(RemoveAdministratorRoleAction(guildId, roleId))
                                        .then(guild.getRoleById(roleId).map(Role::getName).flatMap { roleName: String ->
                                            event.channel.flatMap {
                                                it.createMessage("Successfully removed $roleName from the list of administrator roles.")
                                            }
                                        })
                                }
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("The role has not been added as an administrator role!")
                                })
                        }
                }.switchIfEmpty(event.channel.flatMap {
                    it.createMessage("Sorry, you don't have high enough permissions.")
                }).then()

                "ls", "list", "listroles", "get", "getroles" -> event.xf8bot.botDatabase
                    .execute(GetGuildAdministratorRolesAction(guildId))
                    .flatMapMany { it.toFlux() }
                    .flatMap { it.map { row, _ -> row } }
                    .collectMap(
                        { it.get("roleId", java.lang.Long::class.java) },
                        { it.get("level", java.lang.Integer::class.java) }
                    )
                    .filter { it.isNotEmpty() }
                    .cast<Map<Long, Int>>()
                    .map { it.sortByValue() }
                    .flatMap { administratorRoles ->
                        val roleNames = administratorRoles.keys.joinToString(separator = "\n") { "<@&$it>" }
                        val roleLevels = administratorRoles.values.joinToString(separator = "\n") { it.toString() }

                        event.channel.flatMap {
                            it.createEmbedDsl {
                                title("Administrator Roles")

                                field("Role", roleNames, inline = true)
                                field("Level", roleLevels, inline = true)

                                color(Color.BLUE)
                            }
                        }
                    }
                    .switchIfEmpty(event.channel.flatMap {
                        it.createMessage("The only administrator is the owner.")
                    })
                    .then()

                else -> throw UnexpectedException()
            }
        }
    }

    companion object {
        private val ACTION = StringArgument(
            name = "action",
            index = Range.singleton(1),
            validityPredicate = { value ->
                when (value.toLowerCase(Locale.ROOT)) {
                    "add", "addrole",
                    "rm", "remove", "removerole",
                    "ls", "list", "listroles", "get", "getroles" -> true
                    else -> false
                }
            },
            errorMessageFunction = {
                "Invalid action `%s`! The actions are `addrole`, `removerole`, and `getroles`!"
            }
        )
        private val ROLE = StringFlag(
            shortName = "r",
            longName = "role",
            required = false
        )
        private val ADMINISTRATOR_LEVEL = IntegerFlag(
            shortName = "l",
            longName = "level",
            validityPredicate = {
                try {
                    val level = it.toInt()
                    level in 1..4
                } catch (exception: NumberFormatException) {
                    false
                }
            },
            errorMessageFunction = {
                try {
                    val level = it.toInt()
                    when {
                        level > 4 -> "The maximum administrator level you can assign is 4!"
                        level < 1 -> "The minimum administrator level you can assign is 1!"
                        else -> throw UnexpectedException()
                    }
                } catch (exception: NumberFormatException) {
                    Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            },
            required = false
        )
    }
}