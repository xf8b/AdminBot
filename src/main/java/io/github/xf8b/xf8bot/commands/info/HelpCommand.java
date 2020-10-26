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

package io.github.xf8b.xf8bot.commands;

import com.google.common.collect.Range;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import io.github.xf8b.xf8bot.Xf8bot;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.CommandRegistry;
import io.github.xf8b.xf8bot.api.commands.arguments.IntegerArgument;
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HelpCommand extends AbstractCommand {
    //TODO: make paginated embed system so this can go back to using reactions for pages
    //public static Message currentMessage = null;
    //public static CommandType currentCommandType = null;
    //public static final List<AbstractCommand> commandsShown = Collections.synchronizedList(new ArrayList<>());

    private static final StringArgument SECTION_OR_COMMAND = StringArgument.builder()
            .setIndex(Range.singleton(1))
            .setName("section or command")
            .setNotRequired()
            .build();
    private static final IntegerArgument PAGE = IntegerArgument.builder()
            .setIndex(Range.singleton(2))
            .setName("page")
            .setNotRequired()
            .build();

    public HelpCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}help")
                .setDescription("""
                        Shows the command's description, usage, aliases, and actions.
                        If no command was specified, all the commands in the section will be shown.
                        If no section was specified, all the commands will be shown.
                        """)
                .setCommandType(CommandType.INFO)
                .setArguments(SECTION_OR_COMMAND, PAGE)
                .setBotRequiredPermissions(Permission.EMBED_LINKS));
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        String guildId = event.getGuild().block().getId().asString();
        Xf8bot xf8bot = event.getXf8bot();
        Optional<String> commandOrSection = event.getValueOfArgument(SECTION_OR_COMMAND);
        if (commandOrSection.isEmpty()) {
            return event.getChannel().flatMap(it -> it.createEmbed(embedCreateSpec -> {
                embedCreateSpec.setTitle("Help Page")
                        .setColor(Color.BLUE);
                for (CommandType commandType : CommandType.values()) {
                    String commandTypeName = WordUtils.capitalizeFully(commandType.name()
                            .toLowerCase()
                            .replace("_", " "));
                    embedCreateSpec.addField(
                            "`" + commandTypeName + "`",
                            commandType.getDescription() + "\n" +
                                    "To go to this section, use `" + event.getPrefix().block() + "help " + commandType.name()
                                    .toLowerCase()
                                    .replace(" ", "_") + "`.",
                            false
                    );
                }
            })).then();
        } else {
            for (CommandType commandType : CommandType.values()) {
                if (commandOrSection.get().equalsIgnoreCase(commandType.name())) {
                    Integer pageNumber = event.getValueOfArgument(PAGE).orElse(0);
                    List<AbstractCommand> commandsWithCurrentCommandType = event.getXf8bot().getCommandRegistry()
                            .getCommandsWithCommandType(commandType);
                    if (commandsWithCurrentCommandType.size() > 6) {
                        if (!Range.closedOpen(0, commandsWithCurrentCommandType.size() % 6).contains(pageNumber)) {
                            return event.getChannel()
                                    .flatMap(it -> it.createMessage("No page with the index " + (pageNumber + 1) + " exists!"))
                                    .then();
                        }
                    }
                    //commandsShown.clear();
                    //currentCommandType = commandType;
                    int finalPageNumber = pageNumber;
                    return event.getChannel().flatMap(it -> it.createEmbed(embedCreateSpec -> generateCommandTypeEmbed(
                            event,
                            event.getXf8bot().getCommandRegistry(),
                            embedCreateSpec,
                            commandType,
                            guildId,
                            finalPageNumber
                    ))).then();//.subscribe(/*message -> {
                    //message.addReaction(ReactionEmoji.unicode("⬅")).block();
                    //message.addReaction(ReactionEmoji.unicode("➡")).block();
                    //currentMessage = message;
                    //}*/);
                }
            }
            for (AbstractCommand command : event.getXf8bot().getCommandRegistry()) {
                String name = command.getName();
                String nameWithPrefix = command.getNameWithPrefix(xf8bot, guildId);
                List<String> aliases = command.getAliases();
                List<String> aliasesWithPrefixes = command.getAliasesWithPrefixes(xf8bot, guildId);
                if (commandOrSection.get().equals(name.replace("${prefix}", ""))) {
                    String description = command.getDescription();
                    String usage = command.getUsageWithPrefix(xf8bot, guildId);
                    Map<String, String> actions = command.getActions();
                    return event.getChannel().flatMap(it -> it.createEmbed(embedCreateSpec -> generateCommandEmbed(
                            embedCreateSpec,
                            nameWithPrefix,
                            description,
                            usage,
                            aliasesWithPrefixes,
                            actions
                    ))).then();
                } else if (!aliases.isEmpty()) {
                    for (String alias : aliases) {
                        if (commandOrSection.get().equals(alias.replace("${prefix}", ""))) {
                            String description = command.getDescription();
                            String usage = command.getUsageWithPrefix(xf8bot, guildId);
                            Map<String, String> actions = command.getActions();
                            return event.getChannel().flatMap(it -> it.createEmbed(embedCreateSpec -> generateCommandEmbed(
                                    embedCreateSpec,
                                    nameWithPrefix,
                                    description,
                                    usage,
                                    aliasesWithPrefixes,
                                    actions
                            ))).then();
                        }
                    }
                }
            }
        }
        return Mono.empty(); //TODO: see if this breaks anything
    }

    private void generateCommandTypeEmbed(@NotNull CommandFiredEvent event, @NotNull CommandRegistry commandRegistry, @NotNull EmbedCreateSpec embedCreateSpec, CommandType commandType, @NotNull String guildId, int pageNumber) {
        Xf8bot xf8bot = event.getXf8bot();
        embedCreateSpec.setTitle("Help Page #" + (pageNumber + 1))
                .setDescription("Actions are not listed on this page. To see them, do `" + event.getPrefix().block() + "help <command>`.\n" +
                        "To go to a different page, use `" + event.getPrefix().block() + "help <section> <page>`.")
                .setColor(Color.BLUE);
        List<AbstractCommand> commandsWithCurrentCommandType = commandRegistry
                .getCommandsWithCommandType(commandType);
        for (int i = pageNumber * 6; i < pageNumber * 6 + 6; i++) {
            AbstractCommand command;
            try {
                command = commandsWithCurrentCommandType.get(i);
            } catch (IndexOutOfBoundsException exception) {
                break;
            }
            String name = command.getNameWithPrefix(xf8bot, guildId);
            String nameWithPrefixRemoved = command.getName().replace("${prefix}", "");
            String description = command.getDescription();
            String usage = command.getUsageWithPrefix(xf8bot, guildId);
            embedCreateSpec.addField(
                    "`" + name + "`",
                    description + "\n" +
                            "Usage: `" + usage + "`\n" +
                            "If you want to go to the help page for this command, use `" + event.getPrefix().block() + "help " + nameWithPrefixRemoved + "`.",
                    false
            );
        }
    }

    private void generateCommandEmbed(@NotNull EmbedCreateSpec embedCreateSpec, String name, String description, String usage, @NotNull List<String> aliases, @NotNull Map<String, String> actions) {
        embedCreateSpec.setTitle("Help Page For `" + name + "`")
                .addField("`" + name + "`", description + "\n" +
                        "Usage: `" + usage + "`", false)
                .setColor(Color.BLUE);
        if (!actions.isEmpty()) {
            StringBuilder actionsFormatted = new StringBuilder();
            actions.forEach((action, actionDescription) -> actionsFormatted.append("`").append(action).append("`: ").append(actionDescription).append("\n"));
            embedCreateSpec.addField("Actions", actionsFormatted.toString().replaceAll("\n$", ""), false);
        }
        if (!aliases.isEmpty()) {
            StringBuilder aliasesFormatted = new StringBuilder();
            aliases.forEach(alias -> aliasesFormatted.append("`").append(alias).append("`\n"));
            embedCreateSpec.addField("Aliases", aliasesFormatted.toString().replaceAll("\n$", ""), false);
        }
    }
}