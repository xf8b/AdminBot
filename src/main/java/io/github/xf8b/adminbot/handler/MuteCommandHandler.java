package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

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
                CommandType.ADMINISTRATION,
                1
        );
    }

    @Override
    public void onCommandFired(MessageReceivedEvent event) {
        try {
            String content = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            Guild guild = event.getGuild();
            Member author = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, author) &&
                    PermissionUtil.getAdministratorLevel(guild, author) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 3) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                String userId = content.trim().split(" ")[1].trim().replaceAll("[<@!>]", "");
                String time = content.trim().split(" ")[2].trim().replaceAll("[a-zA-Z]", "").trim();
                String tempTimeType = content.trim().split(" ")[2].trim().replaceAll("\\d", "").trim();
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
                if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
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
                }, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        if (((ErrorResponseException) throwable).getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                            channel.sendMessage("The member is not in the guild!").queue();
                        }
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
