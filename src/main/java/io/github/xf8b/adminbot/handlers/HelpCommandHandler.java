/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.handlers;

import com.google.common.collect.Range;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import io.github.xf8b.adminbot.settings.GuildSettings;
import io.github.xf8b.adminbot.util.CommandRegistry;
import org.apache.commons.text.WordUtils;

import java.util.List;
import java.util.Map;

public class HelpCommandHandler extends AbstractCommandHandler {
    //TODO: make paginated embed system so this can go back to using reactions for pages
    //public static Message currentMessage = null;
    //public static CommandType currentCommandType = null;
    //public static final List<AbstractCommandHandler> commandsShown = Collections.synchronizedList(new ArrayList<>());

    public HelpCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}help")
                .setUsage("${prefix}help [section/command] [page]")
                .setDescription("Shows the command's description, usage, aliases, and actions. \n" +
                        "If no command was specified, all the commands in the section will be shown. \n" +
                        "If no section was specified, all the commands will be shown.")
                .setCommandType(CommandType.OTHER)
                .setBotRequiredPermissions(PermissionSet.of(Permission.EMBED_LINKS)));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String content = event.getMessage().getContent();
        MessageChannel channel = event.getChannel().block();
        String guildId = event.getGuild().block().getId().asString();
        if (content.trim().equalsIgnoreCase(GuildSettings.getGuildSettings(guildId).getPrefix() + "help")) {
            channel.createEmbed(embedCreateSpec -> {
                embedCreateSpec.setTitle("AdminBot Help Page")
                        .setColor(Color.BLUE);
                for (CommandType commandType : CommandType.values()) {
                    String commandTypeName = WordUtils.capitalizeFully(commandType.name()
                            .toLowerCase()
                            .replace("_", " "));
                    embedCreateSpec.addField(
                            "`" + commandTypeName + "`",
                            commandType.getDescription() + "\n" +
                                    "To go to this section, use `" + GuildSettings.getGuildSettings(guildId).getPrefix() + "help " + commandType.name()
                                    .toLowerCase()
                                    .replace(" ", "_") + "`.",
                            false
                    );
                }
            }).block();
        } else {
            String secondInput = content.trim().split(" ")[1].toLowerCase();
            for (CommandType commandType : CommandType.values()) {
                if (secondInput.equalsIgnoreCase(commandType.name())) {
                    int pageNumber;
                    try {
                        String thirdInput = content.trim().split(" ")[2].toLowerCase();
                        pageNumber = Integer.parseInt(thirdInput) - 1;
                        List<AbstractCommandHandler> commandHandlersWithCurrentCommandType = event.getAdminBot().getCommandRegistry()
                                .getCommandHandlersWithCommandType(commandType);
                        if (!Range.closedOpen(0, commandHandlersWithCurrentCommandType.size() % 6).contains(pageNumber)) {
                            channel.createMessage("No page with the index " + (pageNumber + 1) + " exists!").subscribe();
                            return;
                        }
                    } catch (IndexOutOfBoundsException | NumberFormatException exception) {
                        pageNumber = 0;
                    }
                    //commandsShown.clear();
                    //currentCommandType = commandType;
                    int finalPageNumber = pageNumber;
                    channel.createEmbed(embedCreateSpec -> generateCommandTypeEmbed(
                            event.getAdminBot().getCommandRegistry(),
                            embedCreateSpec,
                            commandType,
                            guildId,
                            finalPageNumber
                    )).subscribe(/*message -> {
                        message.addReaction(ReactionEmoji.unicode("⬅")).block();
                        message.addReaction(ReactionEmoji.unicode("➡")).block();
                        //currentMessage = message;
                    }*/);
                    return;
                }
            }
            for (AbstractCommandHandler commandHandler : event.getAdminBot().getCommandRegistry()) {
                String name = commandHandler.getName();
                String nameWithPrefix = commandHandler.getNameWithPrefix(guildId);
                List<String> aliases = commandHandler.getAliases();
                List<String> aliasesWithPrefixes = commandHandler.getAliasesWithPrefixes(guildId);
                if (secondInput.equals(name.replace("${prefix}", ""))) {
                    String description = commandHandler.getDescription();
                    String usage = commandHandler.getUsageWithPrefix(guildId);
                    Map<String, String> actions = commandHandler.getActions();
                    channel.createEmbed(embedCreateSpec -> generateCommandEmbed(embedCreateSpec, nameWithPrefix, description, usage, aliasesWithPrefixes, actions)).block();
                } else if (!aliases.isEmpty()) {
                    for (String alias : aliases) {
                        if (secondInput.equals(alias.replace("${prefix}", ""))) {
                            String description = commandHandler.getDescription();
                            String usage = commandHandler.getUsageWithPrefix(guildId);
                            Map<String, String> actions = commandHandler.getActions();
                            channel.createEmbed(embedCreateSpec -> generateCommandEmbed(embedCreateSpec, nameWithPrefix, description, usage, aliasesWithPrefixes, actions)).block();
                        }
                    }
                }
            }
        }
    }

    private void generateCommandTypeEmbed(CommandRegistry commandRegistry, EmbedCreateSpec embedCreateSpec, CommandType commandType, String guildId, int pageNumber) {
        embedCreateSpec.setTitle("AdminBot Help Page #" + (pageNumber + 1))
                .setDescription("Actions are not listed on this page. To see them, do `" + GuildSettings.getGuildSettings(guildId).getPrefix() + "help <section> <command>`.\n" +
                        "To go to a different page, use `" + GuildSettings.getGuildSettings(guildId).getPrefix() + "help <section> <page>`.")
                .setColor(Color.BLUE);
        List<AbstractCommandHandler> commandHandlersWithCurrentCommandType = commandRegistry
                .getCommandHandlersWithCommandType(commandType);
        for (int i = pageNumber * 6; i < pageNumber * 6 + 6; i++) {
            AbstractCommandHandler commandHandler;
            try {
                commandHandler = commandHandlersWithCurrentCommandType.get(i);
            } catch (IndexOutOfBoundsException exception) {
                break;
            }
            String name = commandHandler.getNameWithPrefix(guildId);
            String nameWithPrefixRemoved = commandHandler.getName().replace("${prefix}", "");
            String description = commandHandler.getDescription();
            String usage = commandHandler.getUsageWithPrefix(guildId);
            embedCreateSpec.addField(
                    "`" + name + "`",
                    description + "\n" +
                            "Usage: `" + usage + "`\n" +
                            "If you want to go to the help page for this command, use `" + GuildSettings.getGuildSettings(guildId).getPrefix() + "help " + nameWithPrefixRemoved + "`.",
                    false
            );
        }
    }

    private void generateCommandEmbed(EmbedCreateSpec embedCreateSpec, String name, String description, String usage, List<String> aliases, Map<String, String> actions) {
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