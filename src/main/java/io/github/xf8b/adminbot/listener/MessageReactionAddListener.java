package io.github.xf8b.adminbot.listener;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.handler.CommandHandler;
import io.github.xf8b.adminbot.handler.HelpCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;

public class MessageReactionAddListener extends ListenerAdapter {
    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        event.retrieveMessage().queue(message -> message.retrieveReactionUsers("⬅").queue(users -> users.forEach(user -> {
            if (user != event.getJDA().getSelfUser() && message.equals(HelpCommandHandler.currentMessage)) {
                message.removeReaction("⬅", user).queue();
                if (HelpCommandHandler.commandsShown.isEmpty()) {
                    return;
                }
                EmbedBuilder embedBuilderForEditedMessage = new EmbedBuilder()
                        .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.getInstance().prefix + "help <section> <command>`.")
                        .setColor(Color.BLUE);
                int amountOfCommandsDisplayedOnEditedMessage = 0;
                for (CommandHandler commandHandler : AdminBot.getInstance().COMMAND_REGISTRY) {
                    if (commandHandler.getCommandType() == HelpCommandHandler.currentCommandType) {
                        if (HelpCommandHandler.commandsShown.contains(commandHandler)) {
                            if (amountOfCommandsDisplayedOnEditedMessage >= 6) break;
                            String name = commandHandler.getNameWithPrefix();
                            String nameWithPrefixRemoved = commandHandler.getName().replace("${prefix}", "");
                            String description = commandHandler.getDescription();
                            String usage = commandHandler.getUsageWithPrefix();
                            embedBuilderForEditedMessage.addField(
                                    "`" + name + "`",
                                    description + "\n" +
                                            "Usage: `" + usage + "`\n" +
                                            "If you want to go to the help page for this command, use `" + AdminBot.getInstance().prefix + "help " + HelpCommandHandler.currentCommandType.name().toLowerCase() + " " + nameWithPrefixRemoved + "`.",
                                    false
                            );
                            amountOfCommandsDisplayedOnEditedMessage++;
                            HelpCommandHandler.commandsShown.remove(commandHandler);
                        }
                    }
                }
                if (amountOfCommandsDisplayedOnEditedMessage == 0) return;
                embedBuilderForEditedMessage.setTitle("AdminBot Help Page");
                message.editMessage(embedBuilderForEditedMessage.build()).queue();
            }
        })));
        event.retrieveMessage().queue(message -> message.retrieveReactionUsers("➡").queue(users -> users.forEach(user -> {
            if (user != event.getJDA().getSelfUser() && message.equals(HelpCommandHandler.currentMessage)) {
                message.removeReaction("➡", user).queue();
                if (HelpCommandHandler.commandsShown.size() == AdminBot.getInstance().COMMAND_REGISTRY.amountOfCommands()) {
                    return;
                }
                EmbedBuilder embedBuilderForEditedMessage = new EmbedBuilder()
                        .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.getInstance().prefix + "help <section> <command>`.")
                        .setColor(Color.BLUE);
                int amountOfCommandsDisplayedOnEditedMessage = 0;
                for (CommandHandler commandHandler : AdminBot.getInstance().COMMAND_REGISTRY) {
                    if (commandHandler.getCommandType() == HelpCommandHandler.currentCommandType) {
                        if (!HelpCommandHandler.commandsShown.contains(commandHandler)) {
                            if (amountOfCommandsDisplayedOnEditedMessage >= 6) break;
                            String name = commandHandler.getNameWithPrefix();
                            String nameWithPrefixRemoved = commandHandler.getName().replace("${prefix}", "");
                            String description = commandHandler.getDescription();
                            String usage = commandHandler.getUsageWithPrefix();
                            embedBuilderForEditedMessage.addField(
                                    "`" + name + "`",
                                    description + "\n" +
                                            "Usage: `" + usage + "`\n" +
                                            "If you want to go to the help page for this command, use `" + AdminBot.getInstance().prefix + "help " + HelpCommandHandler.currentCommandType.name().toLowerCase() + " " + nameWithPrefixRemoved + "`.",
                                    false
                            );
                            amountOfCommandsDisplayedOnEditedMessage++;
                            HelpCommandHandler.commandsShown.add(commandHandler);
                        }
                    }
                }
                if (amountOfCommandsDisplayedOnEditedMessage == 0) return;
                embedBuilderForEditedMessage.setTitle("AdminBot Help Page");
                message.editMessage(embedBuilderForEditedMessage.build()).queue();
            }
        })));
    }
}
