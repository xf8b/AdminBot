package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import io.github.xf8b.adminbot.helper.WarnsDatabaseHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

public class WarnsCommandHandler extends CommandHandler {
    public WarnsCommandHandler() {
        super(
                "${prefix}warns",
                "${prefix}warns <member>",
                "Gets the warns for the specified member.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.ADMINISTRATION
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            String guildId = guild.getId();
            boolean isAdministrator = false;
            String command = content.split(" ")[0];
            for (Role role : event.getMember().getRoles()) {
                String id = role.getId();
                if (AdministratorsDatabaseHelper.doesAdministratorRoleExistInDatabase(guildId, id)) {
                    isAdministrator = true;
                }
            }
            if (event.getMember().isOwner()) isAdministrator = true;
            if (content.trim().equals(command)) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "warns <member>`.").queue();
                return;
            }
            if (isAdministrator) {
                String userId = content.replace(command, "").trim().replaceAll("(<@!|>)", "").trim();
                try {
                    Long.parseLong(userId);
                } catch (NumberFormatException exception) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                guild.retrieveMemberById(userId).queue(member -> {
                    if (member == null) {
                        throw new IllegalStateException("Member is null!");
                    }
                });
                String username = event.getMessage().getContentDisplay().replace(AdminBot.prefix + "warns", "").trim().replace("@", "").trim();
                Multimap<String, String> warns = WarnsDatabaseHelper.getAllWarnsForUser(guildId, userId);
                if (warns.isEmpty()) {
                    channel.sendMessage("The user has no warnings.").queue();
                } else {
                    boolean[] doSeparators = {true};
                    if (warns.size() == 1) {
                        doSeparators[0] = false;
                    }
                    String[] warnsFormatted = {"", ""};
                    warns.forEach((reason, warnId) -> {
                        warnsFormatted[0] = warnsFormatted[0].concat(warnId + (doSeparators[0] ? "\n-----\n" : "\n"));
                        warnsFormatted[1] = warnsFormatted[1].concat(reason + (doSeparators[0] ? "\n-----\n" : "\n"));
                    });
                    warnsFormatted[0] = warnsFormatted[0].replaceAll("-----[\n]*$", "");
                    warnsFormatted[1] = warnsFormatted[1].replaceAll("-----[\n]*$", "");
                    MessageEmbed embed = new EmbedBuilder()
                            .setTitle("Warnings For " + username)
                            .addField("Warn ID", warnsFormatted[0], true)
                            .addField("Reason", warnsFormatted[1], true)
                            .setColor(Color.BLUE)
                            .build();
                    channel.sendMessage(embed).queue();
                }
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
