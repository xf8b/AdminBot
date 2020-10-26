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

package io.github.xf8b.xf8bot.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import io.github.xf8b.xf8bot.Xf8bot;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.commands.HelpCommand;
import io.github.xf8b.xf8bot.data.GuildData;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;

@RequiredArgsConstructor
public class ReactionAddListener {
    private final Xf8bot xf8bot;
    private int currentPage = 1;

    public void onReactionAddEvent(@Nonnull ReactionAddEvent event) {
        //TODO: fix
        if (event.getMember().isEmpty()) return;
        if (event.getMember().get().isBot()) return;
        String guildId = event.getGuild()
                .map(Guild::getId)
                .map(Snowflake::asString)
                .block();
        event.getMessage().subscribe(message -> message.getReactors(ReactionEmoji.unicode("⬅")).subscribe(user -> {
            if (!user.getId().equals(event.getClient().getSelf().map(User::getId).block())
                    && message.getId().equals(HelpCommand.currentMessage.getId())) {
                message.removeReaction(ReactionEmoji.unicode("⬅"), user.getId()).block();
                if (currentPage < 0) return;
                currentPage--;
                if (HelpCommand.commandsShown.isEmpty()) return;
                List<AbstractCommand> commandsWithSameCommandType = xf8bot.getCommandRegistry()
                        .getCommandsWithCommandType(HelpCommand.currentCommandType);
                if (currentPage * 6 >= commandsWithSameCommandType.size()) return;
                message.edit(messageEditSpec -> messageEditSpec.setEmbed(embedCreateSpec -> {
                    embedCreateSpec.setTitle("Help Page #" + (currentPage + 1))
                            .setDescription("Actions are not listed on this page. To see them, do `" + GuildData.getGuildData(guildId).getPrefix() + "help <section> <command>`.")
                            .setColor(Color.BLUE);
                    for (int i = currentPage * 6; i < currentPage * 6 + 6; i++) {
                        AbstractCommand command;
                        try {
                            command = commandsWithSameCommandType.get(i);
                        } catch (IndexOutOfBoundsException exception) {
                            break;
                        }
                        if (HelpCommand.commandsShown.contains(command)) {
                            String name = command.getNameWithPrefix(guildId);
                            String nameWithPrefixRemoved = command.getName().replace("${prefix}", "");
                            String description = command.getDescription();
                            String usage = command.getUsageWithPrefix(guildId);
                            embedCreateSpec.addField(
                                    "`" + name + "`",
                                    description + "\n" +
                                            "Usage: `" + usage + "`\n" +
                                            "If you want to go to the help page for this command, use `" + GuildData.getGuildData(guildId).getPrefix() + "help " + HelpCommand.currentCommandType.name().toLowerCase() + " " + nameWithPrefixRemoved + "`.",
                                    false
                            );
                            HelpCommand.commandsShown.remove(command);
                        }
                    }
                })).block();
            }
        }));
        event.getMessage().subscribe(message -> message.getReactors(ReactionEmoji.unicode("➡")).subscribe(user -> {
            if (!user.getId().equals(event.getClient().getSelf().map(User::getId).block())
                    && message.getId().equals(HelpCommand.currentMessage.getId())) {
                message.removeReaction(ReactionEmoji.unicode("➡"), user.getId()).block();
                currentPage++;
                if (HelpCommand.commandsShown.size() == xf8bot.getCommandRegistry().size()) return;
                List<AbstractCommand> commandsWithSameCommandType = xf8bot.getCommandRegistry()
                        .getCommandsWithCommandType(HelpCommand.currentCommandType);
                if (currentPage * 6 >= commandsWithSameCommandType.size()) return;
                message.edit(messageEditSpec -> messageEditSpec.setEmbed(embedCreateSpec -> {
                    embedCreateSpec.setTitle("Help Page #" + (currentPage + 1))
                            .setDescription("Actions are not listed on this page. To see them, do `" + GuildData.getGuildData(guildId).getPrefix() + "help <section> <command>`.")
                            .setColor(Color.BLUE);
                    for (int i = currentPage * 6; i < currentPage * 6 + 6; i++) {
                        AbstractCommand command;
                        try {
                            command = commandsWithSameCommandType.get(i);
                        } catch (IndexOutOfBoundsException exception) {
                            break;
                        }
                        if (!HelpCommand.commandsShown.contains(command)) {
                            String name = command.getNameWithPrefix(guildId);
                            String nameWithPrefixRemoved = command.getName().replace("${prefix}", "");
                            String description = command.getDescription();
                            String usage = command.getUsageWithPrefix(guildId);
                            embedCreateSpec.addField(
                                    "`" + name + "`",
                                    description + "\n" +
                                            "Usage: `" + usage + "`\n" +
                                            "If you want to go to the help page for this command, use `" + GuildData.getGuildData(guildId).getPrefix() + "help " + HelpCommand.currentCommandType.name().toLowerCase() + " " + nameWithPrefixRemoved + "`.",
                                    false
                            );
                            HelpCommand.commandsShown.add(command);
                        }
                    }
                })).block();
            }
        }));
    }
}
