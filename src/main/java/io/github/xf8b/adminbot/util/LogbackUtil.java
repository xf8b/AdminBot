package io.github.xf8b.adminbot.util;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.github.napstr.logback.DiscordAppender;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class LogbackUtil {
    public void setupDiscordAppender(String webhookUrl, String username, String avatarUrl) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        AsyncAppender discordAsync = (AsyncAppender) loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_DISCORD");
        DiscordAppender discordAppender = (DiscordAppender) discordAsync.getAppender("DISCORD");
        discordAppender.setUsername(username);
        discordAppender.setAvatarUrl(avatarUrl);
        if (!webhookUrl.trim().isBlank()) discordAppender.setWebhookUri(webhookUrl);
        discordAppender.addFilter(new Filter<>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                if (webhookUrl.trim().isBlank()) {
                    return FilterReply.DENY;
                } else {
                    return FilterReply.NEUTRAL;
                }
            }
        });
    }
}
