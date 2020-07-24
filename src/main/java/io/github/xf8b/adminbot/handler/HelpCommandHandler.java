package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.AdminBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.text.WordUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommandHandler extends CommandHandler {
    public static Message currentMessage = null;
    public static CommandType currentCommandType = null;
    public static final List<CommandHandler> commandsShown = new ArrayList<>();

    public HelpCommandHandler() {
        super(
                "${prefix}help",
                "${prefix}help [section] [command]",
                "Shows the command's description, usage, aliases, and actions. \n" +
                        "If no command was specified, all the commands in the section will be shown. \n" +
                        "If no section was specified, all the commands will be shown.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER,
                0
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        if (content.toLowerCase().trim().equals(AdminBot.getInstance().prefix + "help")) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("AdminBot Help Page")
                    .setColor(Color.BLUE);
            for (CommandType commandType : CommandType.values()) {
                String commandTypeName = WordUtils.capitalizeFully(commandType.name().toLowerCase());
                embedBuilder.addField(
                        "`" + commandTypeName + "`",
                        commandType.getDescription() + "\n" +
                                "To go to this section, use `" + AdminBot.getInstance().prefix + "help " + commandType.name().toLowerCase() + "`.",
                        false
                );
            }
            channel.sendMessage(embedBuilder.build()).queue();
        } else {
            String category = content.trim().split(" ")[1].toLowerCase();
            for (CommandType commandType : CommandType.values()) {
                if (category.equals(commandType.name().toLowerCase())) {
                    if (content.trim().split(" ").length < 3) {
                        commandsShown.clear();
                        currentCommandType = commandType;
                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setTitle("AdminBot Help Page")
                                .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.getInstance().prefix + "help <section> <command>`.")
                                .setColor(Color.BLUE);
                        int amountOfCommandsDisplayed = 0;
                        for (CommandHandler commandHandler : AdminBot.getInstance().COMMAND_REGISTRY) {
                            if (commandHandler.getCommandType() == commandType) {
                                if (amountOfCommandsDisplayed >= 6) {
                                    break;
                                }
                                String name = commandHandler.getNameWithPrefix();
                                String nameWithPrefixRemoved = commandHandler.getName().replace("${prefix}", "");
                                String description = commandHandler.getDescription();
                                String usage = commandHandler.getUsageWithPrefix();
                                embedBuilder.addField(
                                        "`" + name + "`",
                                        description + "\n" +
                                                "Usage: `" + usage + "`\n" +
                                                "If you want to go to the help page for this command, use `" + AdminBot.getInstance().prefix + "help " + commandType.name().toLowerCase() + " " + nameWithPrefixRemoved + "`.",
                                        false
                                );
                                amountOfCommandsDisplayed++;
                                commandsShown.add(commandHandler);
                            }
                        }

                        channel.sendMessage(embedBuilder.build()).queue(message -> {
                            message.addReaction("⬅").queue();
                            message.addReaction("➡").queue();
                            currentMessage = message;
                        });
                    } else {
                        String command = content.trim().split(" ")[2].toLowerCase();
                        for (CommandHandler commandHandler : AdminBot.getInstance().COMMAND_REGISTRY) {
                            String name = commandHandler.getName();
                            String nameWithPrefix = commandHandler.getNameWithPrefix();
                            List<String> aliases = commandHandler.getAliases();
                            List<String> aliasesWithPrefixes = commandHandler.getAliasesWithPrefixes();
                            if (command.equals(name.replace("${prefix}", ""))) {
                                String description = commandHandler.getDescription();
                                String usage = commandHandler.getUsageWithPrefix();
                                Map<String, String> actions = commandHandler.getActions();
                                MessageEmbed embed = generateEmbed(nameWithPrefix, description, usage, aliasesWithPrefixes, actions);
                                channel.sendMessage(embed).queue();
                            } else if (!aliases.isEmpty()) {
                                for (String alias : aliases) {
                                    if (command.equals(alias.replace("${prefix}", ""))) {
                                        String description = commandHandler.getDescription();
                                        String usage = commandHandler.getUsageWithPrefix();
                                        Map<String, String> actions = commandHandler.getActions();
                                        MessageEmbed embed = generateEmbed(nameWithPrefix, description, usage, aliasesWithPrefixes, actions);
                                        channel.sendMessage(embed).queue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private MessageEmbed generateEmbed(String name, String description, String usage, List<String> aliases, Map<String, String> actions) {
        String aliasesFormatted = "";
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Help Page For `" + name + "`")
                .addField("`" + name + "`", description + "\nUsage: `" + usage + "`", false)
                .setColor(Color.BLUE);
        if (!actions.isEmpty()) {
            String[] actionsFormatted = {""};
            actions.forEach((action, actionDescription) -> actionsFormatted[0] = actionsFormatted[0].concat("`" + action + "`: " + actionDescription + "\n"));
            embedBuilder.addField("Actions", actionsFormatted[0].replaceAll("[\n]*$", ""), false);
        }
        if (!aliases.isEmpty()) {
            for (String string : aliases) {
                aliasesFormatted = aliasesFormatted.concat("`" + string + "`\n");
            }
            aliasesFormatted = aliasesFormatted.replace("[\n]*$", "");
            embedBuilder.addField("Aliases", aliasesFormatted, false);
        }
        return embedBuilder.build();
    }
}