package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.ArrayList;

@CommandHandler(
        name = "${prefix}help",
        usage = "${prefix}help [command/page]",
        description = "Brings up the help page for the specified command or if none was specified, all of them."
)
public class HelpCommandHandler {
    public static Message currentMessage = null;
    public static ArrayList<Class<?>> commandsShown = new ArrayList<>();

    public static void onHelpCommand(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        if (content.trim().matches("^\\" + AdminBot.prefix + "help$")) {
            commandsShown.clear();
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("AdminBot Help Page")
                    .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.prefix + "help <command>`.")
                    .setColor(Color.BLUE);
            int amountOfCommandsDisplayed = 0;
            for (Class<?> clazz : AdminBot.commandRegistry) {
                String name = AdminBot.commandRegistry.getNameOfCommand(clazz);
                String description = AdminBot.commandRegistry.getDescriptionOfCommand(clazz);
                String usage = AdminBot.commandRegistry.getUsageOfCommand(clazz);
                embedBuilder.addField("`" + name + "`", description + "\nUsage: `" + usage + "`", false);
                if (amountOfCommandsDisplayed >= 6) {
                    break;
                }
                amountOfCommandsDisplayed++;
                commandsShown.add(clazz);
            }
            channel.sendMessage(embedBuilder.build()).queue(message -> {
                message.addReaction("⬅️").queue();
                message.addReaction("➡️").queue();
                currentMessage = message;
            });
        } else {
            String command = content.split(" ")[1].replace(AdminBot.prefix, "");
            for (Class<?> clazz : AdminBot.commandRegistry) {
                String name = AdminBot.commandRegistry.getNameOfCommand(clazz).replace(AdminBot.prefix, "");
                String[] aliases = AdminBot.commandRegistry.getAliasesOfCommand(clazz);
                boolean ignoreAliases = false;
                if (aliases.length == 0) ignoreAliases = true;
                if (command.equals(name)) {
                    String description = AdminBot.commandRegistry.getDescriptionOfCommand(clazz);
                    String usage = AdminBot.commandRegistry.getUsageOfCommand(clazz);
                    String actions = AdminBot.commandRegistry.getActionsOfCommand(clazz);
                    String aliasesFormatted = "";
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("Help Page For `" + command + "`")
                            .addField("`" + AdminBot.prefix + command + "`", description + "\nUsage: `" + usage + "`", false)
                            .setColor(Color.BLUE);
                    if (!actions.equals("")) {
                        embedBuilder.addField("Actions", actions, false);
                    }
                    if (!ignoreAliases) {
                        for (String string : aliases) {
                            aliasesFormatted = aliasesFormatted.concat("`" + string + "`\n");
                        }
                        aliasesFormatted = aliasesFormatted.replace("[\n]*$", "");
                        embedBuilder.addField("Aliases", aliasesFormatted, false);
                    }
                    channel.sendMessage(embedBuilder.build()).queue();
                } else if (!ignoreAliases) {
                    for (String alias : aliases) {
                        if (command.equals(alias.replace(AdminBot.prefix, ""))) {
                            String description = AdminBot.commandRegistry.getDescriptionOfCommand(clazz);
                            String usage = AdminBot.commandRegistry.getUsageOfCommand(clazz);
                            String actions = AdminBot.commandRegistry.getActionsOfCommand(clazz);
                            String aliasesFormatted = "";
                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setTitle("Help Page For `" + command + "`")
                                    .addField("`" + AdminBot.prefix + command + "`", description + "\nUsage: `" + usage + "`", false)
                                    .setColor(Color.BLUE);
                            if (!actions.equals("")) {
                                embedBuilder.addField("Actions", actions, false);
                            }
                            for (String string : aliases) {
                                aliasesFormatted = aliasesFormatted.concat("`" + string + "`\n");
                            }
                            aliasesFormatted = aliasesFormatted.replace("[\n]*$", "");
                            embedBuilder.addField("Aliases", aliasesFormatted, false);
                            channel.sendMessage(embedBuilder.build()).queue();
                        }
                    }
                }
            }
        }
    }
}