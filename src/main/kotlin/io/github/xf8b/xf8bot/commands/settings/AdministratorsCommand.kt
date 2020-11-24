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

package io.github.xf8b.xf8bot.commands.settings

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Range
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.rest.util.Color
import discord4j.rest.util.Permission
import io.github.xf8b.utils.sorting.sortByValue
import io.github.xf8b.xf8bot.api.commands.AbstractCommand
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.api.commands.DisableChecks
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.*
import io.github.xf8b.xf8bot.database.actions.add.AddAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.delete.RemoveAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.find.FindAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.find.GetGuildAdministratorRolesAction
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.*
import io.github.xf8b.xf8bot.util.InputParsing.parseRoleId
import io.github.xf8b.xf8bot.util.PermissionUtil.canMemberUseCommand
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.stream.Collectors

@DisableChecks(AbstractCommand.Checks.IS_ADMINISTRATOR)
class AdministratorsCommand : AbstractCommand(
    name = "\${prefix}administators",
    description = """
    Adds to, removes from, or gets the list of administrator roles.
    The level can be from 1 to 4.
    Level 1 can use `warn`, `removewarn`, `warns`, `mute`, and `nickname`.
    Level 2 can use all the commands for level 1 and `kick` and `clear`.
    Level 3 can use all the commands for level 2 and `ban` and `unban`.
    Level 4 can use all the commands for level 3 and `administrators`, and `prefix`. **This is intended for administrator/owner roles!**
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
    administratorLevelRequired = 4
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> {
        val guildId: Snowflake = event.guildId.get()
        val member: Member = event.member.get()
        val action: String = event.getValueOfArgument(ACTION).get()

        return event.guild.flatMap { guild ->
            val isAdministrator = canMemberUseCommand(event.xf8bot, guild, member, this)

            return@flatMap when (action) {
                "add", "addrole" -> isAdministrator.filter { it }.flatMap ifAdministratorRun@{
                    if (event.getValueOfFlag(ROLE).isEmpty || event.getValueOfFlag(ADMINISTRATOR_LEVEL).isEmpty) {
                        return@ifAdministratorRun event.channel.flatMap {
                            it.createMessage(
                                "Huh? Could you repeat that? The usage of this command is: `${
                                    getUsageWithPrefix(
                                        event.xf8bot,
                                        guildId.asString()
                                    )
                                }`."
                            )
                        }.then()
                    }
                    parseRoleId(event.guild, event.getValueOfFlag(ROLE).get())
                        .map { it.toSnowflake() }
                        .switchIfEmpty(event.channel.flatMap {
                            it.createMessage("The role does not exist!")
                        }.then().cast())
                        .flatMap { roleId: Snowflake ->
                            val level: Int = event.getValueOfFlag(ADMINISTRATOR_LEVEL).get()
                            event.xf8bot.botDatabase
                                .execute(FindAdministratorRoleAction(guildId, roleId))
                                .toMono()
                                .filter { it.isNotEmpty() }
                                .filterWhen { it[0].map { row, _ -> row[0] != null } }
                                .cast(Any::class.java)
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
                    if (event.getValueOfFlag(ROLE).isEmpty) {
                        return@ifAdministratorRun event.channel.flatMap {
                            it.createMessage(
                                "Huh? Could you repeat that? The usage of this command is: `${
                                    getUsageWithPrefix(
                                        event.xf8bot,
                                        guildId.asString()
                                    )
                                }`."
                            )
                        }.then()
                    }
                    // FIXME: not recognizing already added roles
                    parseRoleId(event.guild, event.getValueOfFlag(ROLE).get())
                        .map { it.toSnowflake() }
                        .switchIfEmpty(event.channel.flatMap {
                            it.createMessage("The role does not exist!")
                        }.then().cast())
                        .flatMap { roleId: Snowflake ->
                            event.xf8bot
                                .botDatabase
                                .execute(RemoveAdministratorRoleAction(guildId, roleId))
                                .toMono()
                                .cast(Any::class.java)
                                .flatMap {
                                    guild.getRoleById(roleId)
                                        .map { it.name }
                                        .flatMap { roleName: String ->
                                            event.channel.flatMap {
                                                it.createMessage("Successfully removed $roleName from the list of administrator roles.")
                                            }
                                        }
                                }
                                .switchIfEmpty(event.channel.flatMap {
                                    it.createMessage("The role has not been added as an administrator role!")
                                })
                        }
                }.switchIfEmpty(event.channel.flatMap {
                    it.createMessage("Sorry, you don't have high enough permissions.")
                }).then()

                "ls", "list", "listroles", "get", "getroles" -> event.xf8bot
                    .botDatabase
                    .execute(GetGuildAdministratorRolesAction(guildId))
                    .flatMapMany { it.toFlux() }
                    .flatMap { it.map { row, _ -> row } }
                    .collectMap(
                        { it.get("roleId", java.lang.Long::class.java) },
                        { it.get("level", java.lang.Integer::class.java) }
                    )
                    .filter { it.isNotEmpty() }
                    .map {
                        @Suppress("UNCHECKED_CAST")
                        (it as Map<Long, Int>).sortByValue()
                    }
                    .flatMap { administratorRoles ->
                        val roleNames = administratorRoles.keys
                            .stream()
                            .map { "<@&$it>" }
                            .collect(Collectors.joining("\n"))
                            .replace("\n$".toRegex(), "")
                        val roleLevels = administratorRoles.values
                            .stream()
                            .map { it.toString() }
                            .collect(Collectors.joining("\n"))
                            .replace("\n$".toRegex(), "")
                        event.channel.flatMap {
                            it.createEmbedDsl {
                                title("Administrator Roles")

                                field("Role", roleNames, true)
                                field("Level", roleLevels, true)

                                color(Color.BLUE)
                            }
                        }
                    }
                    .switchIfEmpty(event.channel.flatMap {
                        it.createMessage("The only administrator is the owner.")
                    })
                    .then()

                else -> throw ThisShouldNotHaveBeenThrownException()
            }
        }
    }

    companion object {
        private val ACTION = StringArgument(
            name = "action",
            index = Range.singleton(1),
            validityPredicate = { value ->
                when (value) {
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
                        else -> throw ThisShouldNotHaveBeenThrownException()
                    }
                } catch (exception: NumberFormatException) {
                    Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE
                }
            },
            required = false
        )
    }
}