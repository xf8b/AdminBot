package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.AdminBot;
import io.github.xf8b.adminbot.helper.AdministratorsDatabaseHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class MuteCommandHandler extends CommandHandler {
    public MuteCommandHandler() {
        super(
                "${prefix}mute",
                "${prefix}mute <member> <time>",
                "Mutes the specified member for the specified amount of time.",
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
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + AdminBot.prefix + "mute <member> <time>`.").queue();
                return;
            }
            if (isAdministrator) {
                String args = content.replace(command, "").trim();
                String userId = args.split(" ")[0].trim().replaceAll("[<@!>]", "");
                if (args.split(" ").length == 1) {
                    channel.sendMessage("You must specify a time!").queue();
                    return;
                }
                String time = args.split(" ")[1].trim().replaceAll("[a-zA-Z]", "").trim();
                String tempTimeType = args.split(" ")[1].trim().replaceAll("\\d", "").trim();
                TimeUnit timeType;
                switch (tempTimeType.toLowerCase()) {
                    case "d":
                    case "day":
                    case "days":
                        timeType = TimeUnit.DAYS;
                        break;
                    case "h":
                    case "hr":
                    case "hrs":
                    case "hours":
                        timeType = TimeUnit.HOURS;
                        break;
                    case "m":
                    case "mins":
                    case "minutes":
                        timeType = TimeUnit.MINUTES;
                        break;
                    case "s":
                    case "sec":
                    case "secs":
                    case "second":
                    case "seconds":
                        timeType = TimeUnit.SECONDS;
                        break;
                    default:
                        channel.sendMessage("The time specified is invalid!").queue();
                        return;
                }
                try {
                    Long.parseLong(userId);
                } catch (NumberFormatException exception) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                if (userId.equals("")) {
                    channel.sendMessage("The member does not exist!").queue();
                    return;
                }
                if (!guild.getMember(event.getJDA().getSelfUser()).hasPermission(Permission.MANAGE_ROLES)) {
                    channel.sendMessage("Cannot mute member due to insufficient permissions!").queue();
                    return;
                }
                guild.retrieveMemberById(userId).queue(member -> {
                    if (member == null) {
                        throw new IllegalStateException("Member is null!");
                    }
                    if (member == event.getMember()) {
                        channel.sendMessage("You cannot mute yourself!").queue();
                        return;
                    }
                    if (member.getUser() == event.getJDA().getSelfUser()) {
                        channel.sendMessage("You cannot mute AdminBot!").queue();
                        return;
                    }
                });
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
