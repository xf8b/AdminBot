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
                EmbedBuilder embedBuilderForEditedMessage = new EmbedBuilder()
                        .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.prefix + "help <command>`.")
                        .setColor(Color.BLUE);
                int amountOfCommandsDisplayedOnEditedMessage = 0;
                for (CommandHandler commandHandler : AdminBot.commandRegistry) {
                    if (HelpCommandHandler.commandsShown.contains(commandHandler)) {
                        String name = commandHandler.getName().replace("${prefix}", AdminBot.prefix);
                        String description = commandHandler.getDescription();
                        String usage = commandHandler.getUsage().replace("${prefix}", AdminBot.prefix);
                        embedBuilderForEditedMessage.addField("`" + name + "`", description + "\nUsage: `" + usage + "`", false);
                        if (amountOfCommandsDisplayedOnEditedMessage >= 6) {
                            break;
                        }
                        amountOfCommandsDisplayedOnEditedMessage++;
                        HelpCommandHandler.commandsShown.remove(commandHandler);
                    }
                }
                if (HelpCommandHandler.commandsShown.isEmpty()) {
                    return;
                }
                embedBuilderForEditedMessage
                        .setTitle("AdminBot Help Page");
                message.editMessage(embedBuilderForEditedMessage.build()).queue();
            }
        })));
        event.retrieveMessage().queue(message -> message.retrieveReactionUsers("➡").queue(users -> users.forEach(user -> {
            if (user != event.getJDA().getSelfUser() && message.equals(HelpCommandHandler.currentMessage)) {
                message.removeReaction("➡", user).queue();
                EmbedBuilder embedBuilderForEditedMessage = new EmbedBuilder()
                        .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.prefix + "help <command>`.")
                        .setColor(Color.BLUE);
                int amountOfCommandsDisplayedOnEditedMessage = 0;
                for (CommandHandler commandHandler : AdminBot.commandRegistry) {
                    if (!HelpCommandHandler.commandsShown.contains(commandHandler)) {
                        String name = commandHandler.getName().replace("${prefix}", AdminBot.prefix);
                        String description = commandHandler.getDescription();
                        String usage = commandHandler.getUsage().replace("${prefix}", AdminBot.prefix);
                        embedBuilderForEditedMessage.addField("`" + name + "`", description + "\nUsage: `" + usage + "`", false);
                        if (amountOfCommandsDisplayedOnEditedMessage >= 6) {
                            break;
                        }
                        amountOfCommandsDisplayedOnEditedMessage++;
                        HelpCommandHandler.commandsShown.add(commandHandler);
                    }
                }
                if (HelpCommandHandler.commandsShown.size() == AdminBot.commandRegistry.amountOfCommands()) {
                    return;
                }
                embedBuilderForEditedMessage
                        .setTitle("AdminBot Help Page");
                message.editMessage(embedBuilderForEditedMessage.build()).queue();
            }
        })));
    }
}
