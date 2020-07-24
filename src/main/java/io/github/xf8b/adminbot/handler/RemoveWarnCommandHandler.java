package io.github.xf8b.adminbot.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.xf8b.adminbot.helper.WarnsDatabaseHelper;
import io.github.xf8b.adminbot.util.PermissionUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.sql.SQLException;

public class RemoveWarnCommandHandler extends CommandHandler {
    public RemoveWarnCommandHandler() {
        super(
                "${prefix}removewarn",
                "${prefix}removewarn <member> <reason> [warnId]",
                "Removes the specified member's warns with the warnId and reason provided. " +
                        "\nIf the reason is all, all warns will be removed. The warnId is not needed." +
                        "\nIf the warnId is all, all warns with the same reason will be removed. ",
                ImmutableMap.of(),
                ImmutableList.of("${prefix}removewarns"),
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
            String guildId = guild.getId();
            Member author = event.getMember();
            boolean isAdministrator = PermissionUtil.isAdministrator(guild, author) &&
                    PermissionUtil.getAdministratorLevel(guild, author) >= this.getLevelRequired();
            if (content.trim().split(" ").length < 3) {
                channel.sendMessage("Huh? Could you repeat that? The usage of this command is: `" + this.getUsageWithPrefix() + "`.").queue();
                return;
            }
            if (isAdministrator) {
                String userId = content.trim().split(" ")[1].trim().replaceAll("[<@!>]", "").trim();
                String reasonAndWarnId = content.trim().substring(content.trim().indexOf(" ", content.trim().indexOf(" ") + 1) + 1);
                String reason;
                String warnId;
                if (reasonAndWarnId.lastIndexOf(" ") == -1) {
                    reason = reasonAndWarnId.trim();
                    warnId = "all";
                } else {
                    reason = reasonAndWarnId.substring(0, reasonAndWarnId.lastIndexOf(" ")).trim();
                    warnId = reasonAndWarnId.trim().substring(reasonAndWarnId.lastIndexOf(" ")).trim();
                }
                boolean checkIfWarnExists = true;
                boolean removeAllWarnsWithSameName = false;
                if (reasonAndWarnId.lastIndexOf(" ") == 0) {
                    reason = warnId;
                    warnId = "all";
                }
                if (reason.equals("all")) {
                    checkIfWarnExists = false;
                    removeAllWarnsWithSameName = true;
                }
                if (warnId.trim().equals("")) {
                    warnId = "all";
                }
                if (warnId.trim().equals("all")) {
                    removeAllWarnsWithSameName = true;
                }
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
                }, throwable -> {
                    if (throwable instanceof ErrorResponseException) {
                        if (((ErrorResponseException) throwable).getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                            channel.sendMessage("The member is not in the guild!").queue();
                        }
                    }
                });
                if (!WarnsDatabaseHelper.doesUserHaveWarn(guildId, userId, reason) && checkIfWarnExists) {
                    channel.sendMessage("The user does not have a warn with that reason!").queue();
                } else {
                    WarnsDatabaseHelper.removeWarnsFromUserForGuild(guildId, userId, removeAllWarnsWithSameName ? null : warnId, checkIfWarnExists ? reason : null);
                    guild.retrieveMemberById(userId).queue(member -> channel.sendMessage("Successfully removed warn(s) for " + member.getAsMention() + ".").queue());
                }
            } else {
                channel.sendMessage("Sorry, you don't have high enough permissions.").queue();
            }
        } catch (SQLException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
