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

package io.github.xf8b.xf8bot.commands.administration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import io.github.xf8b.utils.sorting.MapSortersKt;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.DisableChecks;
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument;
import io.github.xf8b.xf8bot.api.commands.flags.Flag;
import io.github.xf8b.xf8bot.api.commands.flags.IntegerFlag;
import io.github.xf8b.xf8bot.api.commands.flags.StringFlag;
import io.github.xf8b.xf8bot.exceptions.ThisShouldNotHaveBeenThrownException;
import io.github.xf8b.xf8bot.util.ParsingUtil;
import io.github.xf8b.xf8bot.util.PermissionUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

@DisableChecks(AbstractCommand.Checks.IS_ADMINISTRATOR)
@Slf4j
public class AdministratorsCommand extends AbstractCommand {
    private static final StringArgument ACTION = StringArgument.builder()
            .setIndex(Range.singleton(1))
            .setName("action")
            .setValidityPredicate(value -> switch (value) {
                case "add", "addrole",
                        "rm", "remove", "removerole",
                        "rdr", "rmdel", "removedeletedroles",
                        "ls", "list", "listroles", "get", "getroles" -> true;
                default -> false;
            })
            .setInvalidValueErrorMessageFunction(invalidValue -> "Invalid action `%s`! The actions are `addrole`, `removerole`, `removedeletedroles`, and `getroles`!")
            .build();
    private static final StringFlag ROLE = StringFlag.builder()
            .setShortName("r")
            .setLongName("role")
            .setNotRequired()
            .build();
    private static final IntegerFlag ADMINISTRATOR_LEVEL = IntegerFlag.builder()
            .setShortName("l")
            .setLongName("level")
            .setNotRequired()
            .setValidityPredicate(value -> {
                try {
                    int level = Integer.parseInt(value);
                    return level <= 4 && level >= 1;
                } catch (NumberFormatException exception) {
                    return false;
                }
            })
            .setInvalidValueErrorMessageFunction(invalidValue -> {
                try {
                    int level = Integer.parseInt(invalidValue);
                    if (level > 4) {
                        return "The maximum administrator level you can assign is 4!";
                    } else if (level < 1) {
                        return "The minimum administrator level you can assign is 1!";
                    } else {
                        throw new ThisShouldNotHaveBeenThrownException();
                    }
                } catch (NumberFormatException exception) {
                    return Flag.DEFAULT_INVALID_VALUE_ERROR_MESSAGE;
                }
            })
            .build();

    public AdministratorsCommand() {
        super(AbstractCommand.builder()
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
                .setAdministratorLevelRequired(4));
    }

    @Nonnull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        String guildId = event.getGuildId().get().asString();
        Member member = event.getMember().get();
        String action = event.getValueOfArgument(ACTION).get();
        MongoCollection<Document> mongoCollection = event.getXf8bot()
                .getMongoDatabase()
                .getCollection("administratorRoles");

        return event.getGuild().flatMap(guild -> {
            Mono<Boolean> isAdministrator = PermissionUtil.canMemberUseCommand(event.getXf8bot(), guild, member, this);
            return switch (action) {
                case "add", "addrole" -> isAdministrator.filter(administrator -> administrator).flatMap($ -> {
                    if (event.getValueOfFlag(ROLE).isEmpty() || event.getValueOfFlag(ADMINISTRATOR_LEVEL).isEmpty()) {
                        return event.getChannel().flatMap(it -> it.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(event.getXf8bot(), guildId) + "`.")).then();
                    }
                    return ParsingUtil.parseRoleId(event.getGuild(), event.getValueOfFlag(ROLE).get())
                            .map(Snowflake::of)
                            .switchIfEmpty(event.getChannel()
                                    .flatMap(it -> it.createMessage("The role does not exist!"))
                                    .then()
                                    .cast(Snowflake.class))
                            .flatMap(roleId -> {
                                int level = event.getValueOfFlag(ADMINISTRATOR_LEVEL).get();
                                return Mono.from(mongoCollection.find(Filters.and(
                                        Filters.eq("roleId", roleId.asLong()),
                                        Filters.eq("guildId", Long.parseLong(guildId))
                                ))).cast(Object.class)
                                        .flatMap($1 -> event.getChannel().flatMap(it -> it.createMessage("The role already has been added as an administrator role.")))
                                        .switchIfEmpty(Mono.from(mongoCollection.insertOne(new Document()
                                                .append("guildId", Long.parseLong(guildId))
                                                .append("roleId", roleId.asLong())
                                                .append("level", level)))
                                                .then(guild.getRoleById(roleId)
                                                        .map(Role::getName)
                                                        .flatMap(roleName -> event.getChannel()
                                                                .flatMap(it -> it.createMessage("Successfully added " + roleName + " to the list of administrator roles.")))))
                                        .then();
                            });
                }).switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("Sorry, you don't have high enough permissions.")).then());
                case "rm", "remove", "removerole" -> isAdministrator.filter(administrator -> administrator).flatMap($ -> {
                    if (event.getValueOfFlag(ROLE).isEmpty()) {
                        return event.getChannel().flatMap(it -> it.createMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix(event.getXf8bot(), guildId) + "`.")).then();
                    }
                    return ParsingUtil.parseRoleId(event.getGuild(), event.getValueOfFlag(ROLE).get())
                            .map(Snowflake::of)
                            .switchIfEmpty(event.getChannel()
                                    .flatMap(it -> it.createMessage("The role does not exist!"))
                                    .then()
                                    .cast(Snowflake.class))
                            .flatMap(roleId -> Mono.from(mongoCollection.findOneAndDelete(Filters.and(
                                    Filters.eq("roleId", roleId.asLong()),
                                    Filters.eq("guildId", Long.parseLong(guildId))
                            ))).cast(Object.class)
                                    .flatMap($1 -> guild.getRoleById(roleId)
                                            .map(Role::getName)
                                            .flatMap(roleName -> event.getChannel()
                                                    .flatMap(it -> it.createMessage("Successfully removed " + roleName + " from the list of administrator roles."))))
                                    .switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("The role has not been added as an administrator role!")))
                                    .then());
                }).switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("Sorry, you don't have high enough permissions.")).then());
                case "rdr", "rmdel", "removedeletedroles" -> isAdministrator.filter(administrator -> administrator).flatMap($ -> {
                    Flux<Document> documentFlux = Flux.from(mongoCollection.find(Filters.eq("guildId", Long.parseLong(guildId))))
                            .filterWhen(document -> guild.getRoleById(Snowflake.of(document.getLong("roleId")))
                                    .flux()
                                    .count()
                                    .map(count -> count == 0L));
                    return documentFlux.count()
                            .flatMap(amountOfRemovedRoles -> documentFlux.flatMap(mongoCollection::deleteOne)
                                    .then(event.getChannel()
                                            .flatMap(it -> it.createMessage("Successfully removed " + amountOfRemovedRoles + " deleted roles from the list of administrator roles.")))
                                    .then());
                }).switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("Sorry, you don't have high enough permissions.")).then());
                case "ls", "list", "listroles", "get", "getroles" -> Flux.from(mongoCollection.find(Filters.eq("guildId", Long.parseLong(guildId))))
                        .collectMap(document -> document.getLong("roleId"), document -> document.getInteger("level"))
                        .map(MapSortersKt::sortByValue)
                        .flatMap(administratorRoles -> {
                            String roleNames = administratorRoles.keySet()
                                    .stream()
                                    .map(roleId -> "<@&" + roleId + ">")
                                    .collect(Collectors.joining("\n"))
                                    .replaceAll("\n$", "");
                            String roleLevels = administratorRoles.values()
                                    .stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining("\n"))
                                    .replaceAll("\n$", "");
                            return event.getChannel()
                                    .flatMap(it -> it.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("Administrator Roles")
                                            .addField("Role", roleNames, true)
                                            .addField("Level", roleLevels, true)
                                            .setColor(Color.BLUE)));
                        })
                        .switchIfEmpty(event.getChannel().flatMap(it -> it.createMessage("The only administrator is the owner.")))
                        .then();
                default -> throw new ThisShouldNotHaveBeenThrownException();
            };
        });
    }
}
