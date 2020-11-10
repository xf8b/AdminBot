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
import io.github.xf8b.xf8bot.api.commands.CommandFiredContext
import io.github.xf8b.xf8bot.api.commands.DisableChecks
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument
import io.github.xf8b.xf8bot.api.commands.flags.Flag
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag
import io.github.xf8b.xf8bot.database.actions.*
import io.github.xf8b.xf8bot.database.actions.add.AddAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.delete.DeleteDocumentAction
import io.github.xf8b.xf8bot.database.actions.delete.RemoveAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.find.FindAdministratorRoleAction
import io.github.xf8b.xf8bot.database.actions.find.GetGuildAdministratorRolesAction
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException
import io.github.xf8b.xf8bot.util.ParsingUtil.parseRoleId
import io.github.xf8b.xf8bot.util.PermissionUtil.canMemberUseCommand
import io.github.xf8b.xf8bot.util.toImmutableList
import io.github.xf8b.xf8bot.util.toSingletonImmutableList
import io.github.xf8b.xf8bot.util.toSingletonPermissionSet
import io.github.xf8b.xf8bot.util.toSnowflake
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.stream.Collectors

/*
.setName("${prefix}administrators")
                .setDescription("""
                        Adds to, removes from, or gets the list of administrator roles.
                        The level can be from 1 to 4.
                        Level 1 can use `warn`, `removewarn`, `warns`, `mute`, and `nickname`.
                        Level 2 can use all the commands for level 1 and `kick` and `clear`.
                        Level 3 can use all the commands for level 2 and `ban` and `unban`.
                        Level 4 can use all the commands for level 3 and `administrators`, and `prefix`. \
                        This is intended for administrator/owner roles!
                        """)
                .setCommandType(CommandType.ADMINISTRATION)
                .setActions(ImmutableMap.of(
                        "addrole", "Adds to the list of administrator roles.",
                        "removerole", "Removes from the list of administrator roles.",
                        "removedeletedroles", "Removes deleted roles from the list of administrator roles.",
                        "getroles", "Gets the list of administrator roles."
                ))
                .addAlias("${prefix}admins")
                .setMinimumAmountOfArgs(1)
                .addArgument(ACTION)
                .setFlags(ROLE, ADMINISTRATOR_LEVEL)
                .setBotRequiredPermissions(Permission.EMBED_LINKS)
                .setAdministratorLevelRequired(4)
 */
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
            "rdr/rmdel/removedeletedroles" to "Removes deleted roles from the list of administrator roles.",
            "ls/list/listroles/get/getroles" to "Gets the list of administrator roles."
        )
    ),
    aliases = "\${prefix}admins".toSingletonImmutableList(),
    arguments = ACTION.toSingletonImmutableList(),
    flags = (ROLE to ADMINISTRATOR_LEVEL).toImmutableList(),
    minimumAmountOfArgs = 1,
    botRequiredPermissions = Permission.EMBED_LINKS.toSingletonPermissionSet(),
    administratorLevelRequired = 4
) {
    override fun onCommandFired(context: CommandFiredContext): Mono<Void> {
        val guildId: Snowflake = context.guildId.get()
        val member: Member = context.member.get()
        val action: String = context.getValueOfArgument(ACTION).get()

        return context.guild.flatMap { guild ->
            val isAdministrator = canMemberUseCommand(context.xf8bot, guild, member, this)

            return@flatMap when (action) {
                "add", "addrole" -> isAdministrator.filter { it }.flatMap ifAdministratorRun@{
                    if (context.getValueOfFlag(ROLE).isEmpty || context.getValueOfFlag(ADMINISTRATOR_LEVEL).isEmpty) {
                        return@ifAdministratorRun context.channel.flatMap {
                            it.createMessage(
                                "Huh? Could you repeat that? The usage of this command is: `${
                                    getUsageWithPrefix(
                                        context.xf8bot,
                                        guildId.asString()
                                    )
                                }`."
                            )
                        }.then()
                    }
                    parseRoleId(context.guild, context.getValueOfFlag(ROLE).get())
                        .map { it.toSnowflake() }
                        .switchIfEmpty(context.channel.flatMap {
                            it.createMessage("The role does not exist!")
                        }.then().cast())
                        .flatMap { roleId: Snowflake ->
                            val level: Int = context.getValueOfFlag(ADMINISTRATOR_LEVEL).get()
                            context.xf8bot
                                .botMongoDatabase
                                .execute(FindAdministratorRoleAction(guildId, roleId))
                                .toMono()
                                .cast(Any::class.java)
                                .flatMap {
                                    context.channel.flatMap {
                                        it.createMessage("The role already has been added as an administrator role.")
                                    }
                                }
                                .switchIfEmpty(context.xf8bot
                                    .botMongoDatabase
                                    .execute(AddAdministratorRoleAction(guildId, roleId, level))
                                    .toMono()
                                    .then(guild.getRoleById(roleId)
                                        .map { it.name }
                                        .flatMap { roleName: String ->
                                            context.channel.flatMap {
                                                it.createMessage("Successfully added $roleName to the list of administrator roles.")
                                            }
                                        })
                                )
                        }
                }.switchIfEmpty(context.channel.flatMap {
                    it.createMessage("Sorry, you don't have high enough permissions.")
                }).then()


                "rm", "remove", "removerole" -> isAdministrator.filter { it }.flatMap ifAdministratorRun@{
                    if (context.getValueOfFlag(ROLE).isEmpty) {
                        return@ifAdministratorRun context.channel.flatMap {
                            it.createMessage(
                                "Huh? Could you repeat that? The usage of this command is: `${
                                    getUsageWithPrefix(
                                        context.xf8bot,
                                        guildId.asString()
                                    )
                                }`."
                            )
                        }.then()
                    }
                    parseRoleId(context.guild, context.getValueOfFlag(ROLE).get())
                        .map { it.toSnowflake() }
                        .switchIfEmpty(context.channel.flatMap {
                            it.createMessage("The role does not exist!")
                        }.then().cast())
                        .flatMap { roleId: Snowflake ->
                            context.xf8bot
                                .botMongoDatabase
                                .execute(RemoveAdministratorRoleAction(guildId, roleId))
                                .toMono()
                                .cast(Any::class.java)
                                .flatMap {
                                    guild.getRoleById(roleId)
                                        .map { it.name }
                                        .flatMap { roleName: String ->
                                            context.channel.flatMap {
                                                it.createMessage("Successfully removed $roleName from the list of administrator roles.")
                                            }
                                        }
                                }
                                .switchIfEmpty(context.channel.flatMap {
                                    it.createMessage("The role has not been added as an administrator role!")
                                })
                        }
                }.switchIfEmpty(context.channel.flatMap {
                    it.createMessage("Sorry, you don't have high enough permissions.")
                }).then()

                "rdr", "rmdel", "removedeletedroles" -> isAdministrator.filter { it }.flatMap {
                    val documentFlux = context.xf8bot
                        .botMongoDatabase
                        .execute(GetGuildAdministratorRolesAction(guildId))
                        .toFlux()
                        .filterWhen { document ->
                            guild.getRoleById(Snowflake.of(document.getLong("roleId")))
                                .flux()
                                .count()
                                .map { it == 0L }
                        }.cache()
                    documentFlux.count()
                        .flatMap { amountOfRemovedRoles: Long ->
                            documentFlux.flatMap {
                                context.xf8bot
                                    .botMongoDatabase
                                    .execute(DeleteDocumentAction("administratorRoles", it))
                            }.then(context.channel.flatMap {
                                it.createMessage("Successfully removed $amountOfRemovedRoles deleted roles from the list of administrator roles.")
                            })
                        }
                }.switchIfEmpty(context.channel.flatMap {
                    it.createMessage("Sorry, you don't have high enough permissions.")
                }).then()


                "ls", "list", "listroles", "get", "getroles" -> context.xf8bot
                    .botMongoDatabase
                    .execute(GetGuildAdministratorRolesAction(guildId))
                    .toFlux()
                    .collectMap(
                        { it.getLong("roleId") },
                        { it.getInteger("level") }
                    )
                    .filter { it.isNotEmpty() }
                    .map { it.sortByValue() }
                    .flatMap { administratorRoles: Map<Long, Int> ->
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
                        context.channel.flatMap {
                            it.createEmbed { spec ->
                                spec.setTitle("Administrator Roles")
                                    .addField("Role", roleNames, true)
                                    .addField("Level", roleLevels, true)
                                    .setColor(Color.BLUE)
                            }
                        }
                    }
                    .switchIfEmpty(context.channel.flatMap {
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
                    "rdr", "rmdel", "removedeletedroles",
                    "ls", "list", "listroles", "get", "getroles" -> true
                    else -> false
                }
            },
            invalidValueErrorMessageFunction = {
                "Invalid action `%s`! The actions are `addrole`, `removerole`, `removedeletedroles`, and `getroles`!"
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
            invalidValueErrorMessageFunction = {
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