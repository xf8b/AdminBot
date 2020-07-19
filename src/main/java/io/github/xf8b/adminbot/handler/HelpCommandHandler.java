package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.util.RegexUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommandHandler extends CommandHandler {
    public static Message currentMessage = null;
    public static ArrayList<CommandHandler> commandsShown = new ArrayList<>();

    public HelpCommandHandler() {
        super(
                "${prefix}help",
                "${prefix}help [command]",
                "Shows the command's description, usage, aliases, and actions. If no command was specified, all the commands will be shown.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        String regex;
        if (RegexUtil.containsIllegals(AdminBot.prefix)) {
            regex = "^\\" + AdminBot.prefix + "help$";
        } else {
            regex = "^" + AdminBot.prefix + "help$";
        }
        if (content.trim().matches(regex)) {
            commandsShown.clear();
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("AdminBot Help Page")
                    .setDescription("Actions are not listed on this page. To see them, do `" + AdminBot.prefix + "help <command>`.")
                    .setColor(Color.BLUE);
            int amountOfCommandsDisplayed = 0;
            for (CommandHandler commandHandler : AdminBot.commandRegistry) {
                String name = commandHandler.getName().replace("${prefix}", AdminBot.prefix);
                String description = commandHandler.getDescription();
                String usage = commandHandler.getUsage().replace("${prefix}", AdminBot.prefix);
                embedBuilder.addField("`" + name + "`", description + "\nUsage: `" + usage + "`", false);
                if (amountOfCommandsDisplayed >= 6) {
                    break;
                }
                amountOfCommandsDisplayed++;
                commandsShown.add(commandHandler);
            }
            channel.sendMessage(embedBuilder.build()).queue(message -> {
                message.addReaction("⬅").queue();
                message.addReaction("➡").queue();
                currentMessage = message;
            });
        } else {
            String command = content.split(" ")[1].replace(AdminBot.prefix, "");
            for (CommandHandler commandHandler : AdminBot.commandRegistry) {
                String name = commandHandler.getName();
                List<String> aliases = commandHandler.getAliases();
                List<String> aliasesWithPrefixesAdded = new ArrayList<>();
                aliases.forEach(alias -> aliasesWithPrefixesAdded.add(alias.replace("${prefix}", AdminBot.prefix)));
                if (command.equals(name.replace("${prefix}", ""))) {
                    String description = commandHandler.getDescription();
                    String usage = commandHandler.getUsage().replace("${prefix}", AdminBot.prefix);
                    Map<String, String> actions = commandHandler.getActions();
                    MessageEmbed embed = generateEmbed(name.replace("${prefix}", AdminBot.prefix), description, usage, aliasesWithPrefixesAdded, actions);
                    channel.sendMessage(embed).queue();
                } else if (!aliases.isEmpty()) {
                    for (String alias : aliases) {
                        if (command.equals(alias.replace("${prefix}", ""))) {
                            String description = commandHandler.getDescription();
                            String usage = commandHandler.getUsage().replace("${prefix}", AdminBot.prefix);
                            Map<String, String> actions = commandHandler.getActions();
                            MessageEmbed embed = generateEmbed(name.replace("${prefix}", AdminBot.prefix), description, usage, aliasesWithPrefixesAdded, actions);
                            channel.sendMessage(embed).queue();
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
            actions.forEach((action, actionDescription) -> actionsFormatted[0] = action + ": " + actionDescription + "\n");
            embedBuilder.addField("Actions", actionsFormatted[0], false);
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